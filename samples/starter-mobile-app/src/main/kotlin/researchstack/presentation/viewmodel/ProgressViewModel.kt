package researchstack.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.presentation.util.kgToLbs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class ChartEntry(val label: String, val value: Float)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    application: Application,
    private val exerciseDao: ExerciseDao,
    private val biaDao: BiaDao,
    private val userProfileDao: UserProfileDao,
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                exerciseDao.getExercisesFrom(0).collect { list ->
                    _caloriesByDate.value = list
                        .groupBy { Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate() }
                        .toSortedMap()
                        .map { (date, entries) ->
                            ChartEntry(date.format(dayFormatter), entries.sumOf { it.calorie }.toFloat())
                        }
                }
            }
            launch {
                combine(biaDao.getBetween(0, Long.MAX_VALUE), _isMetricUnit) { list, isMetric ->
                    val muscleList = mutableListOf<ChartEntry>()
                    val fatMassList = mutableListOf<ChartEntry>()
                    val fatFreeList = mutableListOf<ChartEntry>()
                    list.sortedBy { it.timestamp }.forEach { bia ->
                        val date = Instant.ofEpochMilli(bia.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val label = date.format(dayFormatter)
                        muscleList.add(ChartEntry(label, bia.skeletalMuscleMass.kgToLbs(isMetric)))
                        fatMassList.add(ChartEntry(label, bia.bodyFatMass.kgToLbs(isMetric)))
                        fatFreeList.add(ChartEntry(label, bia.fatFreeMass.kgToLbs(isMetric)))
                    }
                    Triple(muscleList, fatMassList, fatFreeList)
                }.collect { (muscleList, fatMassList, fatFreeList) ->
                    _muscleMassByDate.value = muscleList
                    _fatMassByDate.value = fatMassList
                    _fatFreeMassByDate.value = fatFreeList
                }

            }
            launch {
                combine(userProfileDao.getBetween(0, Long.MAX_VALUE), _isMetricUnit) { list, isMetric ->
                    list.sortedBy { it.timestamp }.map { profile ->
                        val date = Instant.ofEpochMilli(profile.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        ChartEntry(date.format(dayFormatter), profile.weight.kgToLbs(isMetric))
                    }
                }.collect { _weightByDate.value = it }
            }
            launch {
                userProfileDao.getLatest().collect { profile ->
                    _isMetricUnit.value = profile?.isMetricUnit != false
                }
            }
        }
    }
}
