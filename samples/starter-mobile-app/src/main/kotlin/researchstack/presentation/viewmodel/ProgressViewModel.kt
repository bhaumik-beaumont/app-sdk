package researchstack.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.pref.dataStore
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.domain.repository.StudyRepository
import researchstack.presentation.util.kgToLbs
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    application: Application,
    private val exerciseDao: ExerciseDao,
    private val biaDao: BiaDao,
    private val userProfileDao: UserProfileDao,
    private val studyRepository: StudyRepository,
) : AndroidViewModel(application) {

    private val dayFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
    private val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

    private val _caloriesByDate = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val caloriesByDate: StateFlow<List<Pair<String, Float>>> = _caloriesByDate

    private val _muscleMassByDate = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val muscleMassByDate: StateFlow<List<Pair<String, Float>>> = _muscleMassByDate

    private val _fatMassByDate = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val fatMassByDate: StateFlow<List<Pair<String, Float>>> = _fatMassByDate

    private val _fatFreeMassByDate = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val fatFreeMassByDate: StateFlow<List<Pair<String, Float>>> = _fatFreeMassByDate

    private val _weightByDate = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val weightByDate: StateFlow<List<Pair<String, Float>>> = _weightByDate

    private val _isMetricUnit = MutableStateFlow(true)
    val isMetricUnit: StateFlow<Boolean> = _isMetricUnit

    private val enrollmentPref = EnrollmentDatePref(application.dataStore)
    private var enrollmentDate: LocalDate? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val studyId = studyRepository.getActiveStudies().firstOrNull()?.firstOrNull()?.id
            if (studyId != null) {
                enrollmentDate = enrollmentPref.getEnrollmentDate(studyId)?.let { LocalDate.parse(it) }
            }

            launch {
                exerciseDao.getExercisesFrom(0).collect { list ->
                    val daily = list.groupBy {
                        Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
                    }.mapValues { entry ->
                        entry.value.sumOf { it.calorie }.toFloat()
                    }
                    _caloriesByDate.value = aggregateFloatData(daily)
                }
            }
            launch {
                combine(biaDao.getBetween(0, Long.MAX_VALUE), _isMetricUnit) { list, isMetric ->
                    val muscle = mutableMapOf<LocalDate, MutableList<Float>>() // skeletal muscle mass
                    val fatMass = mutableMapOf<LocalDate, MutableList<Float>>() // body fat mass
                    val fatFree = mutableMapOf<LocalDate, MutableList<Float>>() // fat free mass
                    list.forEach { bia ->
                        val date = Instant.ofEpochMilli(bia.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        muscle.getOrPut(date) { mutableListOf() }.add(bia.skeletalMuscleMass.kgToLbs(isMetric))
                        fatMass.getOrPut(date) { mutableListOf() }.add(bia.bodyFatMass.kgToLbs(isMetric))
                        fatFree.getOrPut(date) { mutableListOf() }.add(bia.fatFreeMass.kgToLbs(isMetric))
                    }
                    _muscleMassByDate.value = aggregateFloatData(muscle.mapValues { it.value.average().toFloat() })
                    _fatMassByDate.value = aggregateFloatData(fatMass.mapValues { it.value.average().toFloat() })
                    _fatFreeMassByDate.value = aggregateFloatData(fatFree.mapValues { it.value.average().toFloat() })
                }.collect()
            }
            launch {
                combine(userProfileDao.getBetween(0, Long.MAX_VALUE), _isMetricUnit) { list, isMetric ->
                    val daily = list.groupBy {
                        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                    }.mapValues { entry ->
                        entry.value.map { it.weight.kgToLbs(isMetric) }.average().toFloat()
                    }
                    aggregateFloatData(daily)
                }.collect { _weightByDate.value = it }
            }
            launch {
                userProfileDao.getLatest().collect { profile ->
                    _isMetricUnit.value = profile?.isMetricUnit != false
                }
            }
        }
    }

    private fun aggregateFloatData(daily: Map<LocalDate, Float>): List<Pair<String, Float>> {
        val sorted = daily.toSortedMap()
        if (sorted.isEmpty()) return emptyList()

        if (sorted.size <= 6) {
            return sorted.map { (date, value) -> date.format(dayFormatter) to value }
        }

        val start = enrollmentDate ?: sorted.firstKey()
        val weekGroups = sorted.entries.groupBy { entry ->
            val days = ChronoUnit.DAYS.between(start, entry.key).toInt()
            start.plusDays((days / 7) * 7L)
        }
        val weekData = weekGroups.toSortedMap().map { (weekStart, entries) ->
            val avg = entries.map { it.value }.average().toFloat()
            val end = weekStart.plusDays(6)
            val label = "${weekStart.format(dayFormatter)} - ${end.format(dayFormatter)}"
            label to avg
        }
        if (weekData.size <= 6) {
            return weekData
        }

        val monthGroups = sorted.entries.groupBy { entry ->
            val months = ChronoUnit.MONTHS.between(start, entry.key).toInt()
            start.plusMonths(months.toLong())
        }
        val monthData = monthGroups.toSortedMap().map { (monthStart, entries) ->
            val avg = entries.map { it.value }.average().toFloat()
            monthStart.format(monthFormatter) to avg
        }
        return monthData.takeLast(6)
    }
}
