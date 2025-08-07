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

data class ChartEntry(val label: String, val value: Float)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    application: Application,
    private val exerciseDao: ExerciseDao,
    private val biaDao: BiaDao,
    private val userProfileDao: UserProfileDao,
    private val studyRepository: StudyRepository,
) : AndroidViewModel(application) {

    private val dayFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())

    private val _caloriesByDate = MutableStateFlow<List<ChartEntry>>(emptyList())
    val caloriesByDate: StateFlow<List<ChartEntry>> = _caloriesByDate

    private val _muscleMassByDate = MutableStateFlow<List<ChartEntry>>(emptyList())
    val muscleMassByDate: StateFlow<List<ChartEntry>> = _muscleMassByDate

    private val _fatMassByDate = MutableStateFlow<List<ChartEntry>>(emptyList())
    val fatMassByDate: StateFlow<List<ChartEntry>> = _fatMassByDate

    private val _fatFreeMassByDate = MutableStateFlow<List<ChartEntry>>(emptyList())
    val fatFreeMassByDate: StateFlow<List<ChartEntry>> = _fatFreeMassByDate

    private val _weightByDate = MutableStateFlow<List<ChartEntry>>(emptyList())
    val weightByDate: StateFlow<List<ChartEntry>> = _weightByDate

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
                    _caloriesByDate.value = aggregateFloatData(daily, sum = true)
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
                    Triple(
                        aggregateFloatData(muscle.mapValues { it.value.average().toFloat() }),
                        aggregateFloatData(fatMass.mapValues { it.value.average().toFloat() }),
                        aggregateFloatData(fatFree.mapValues { it.value.average().toFloat() })
                    )
                }.collect { (muscleList, fatMassList, fatFreeList) ->
                    _muscleMassByDate.value = muscleList
                    _fatMassByDate.value = fatMassList
                    _fatFreeMassByDate.value = fatFreeList
                }

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

    private fun aggregateFloatData(daily: Map<LocalDate, Float>, sum: Boolean = false): List<ChartEntry> {
        val sorted = daily.toSortedMap()
        if (sorted.isEmpty()) return emptyList()

        val start = enrollmentDate ?: sorted.firstKey()
        val end = sorted.lastKey()

        val dailyEntries = groupByDay(sorted, start, end)
        if (dailyEntries.size <= 6) return dailyEntries

        val weeklyEntries = groupByWeek(sorted, start, end, sum)
        if (weeklyEntries.size <= 6) return weeklyEntries

        return groupByMonth(sorted, start, end, sum).takeLast(6)
    }

    private fun groupByDay(data: Map<LocalDate, Float>, start: LocalDate, end: LocalDate): List<ChartEntry> {
        val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
        return (0 until days).map { offset ->
            val date = start.plusDays(offset.toLong())
            ChartEntry(date.format(dayFormatter), data[date] ?: 0f)
        }
    }

    private fun groupByWeek(data: Map<LocalDate, Float>, start: LocalDate, end: LocalDate, sum: Boolean): List<ChartEntry> {
        val weeks = ChronoUnit.WEEKS.between(start, end).toInt() + 1
        return (0 until weeks).map { index ->
            val weekStart = start.plusWeeks(index.toLong())
            val weekEnd = weekStart.plusDays(6)
            val values = data.filterKeys { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }.values
            val value = if (values.isNotEmpty()) {
                if (sum) values.sum() else values.average().toFloat()
            } else 0f
            ChartEntry("Week-${index + 1}", value)
        }
    }

    private fun groupByMonth(data: Map<LocalDate, Float>, start: LocalDate, end: LocalDate, sum: Boolean): List<ChartEntry> {
        val months = ChronoUnit.MONTHS.between(start, end).toInt() + 1
        return (0 until months).map { index ->
            val monthStart = start.plusMonths(index.toLong())
            val nextMonthStart = monthStart.plusMonths(1)
            val values = data.filterKeys { !it.isBefore(monthStart) && it.isBefore(nextMonthStart) }.values
            val value = if (values.isNotEmpty()) {
                if (sum) values.sum() else values.average().toFloat()
            } else 0f
            ChartEntry("Month-${index + 1}", value)
        }
    }
}
