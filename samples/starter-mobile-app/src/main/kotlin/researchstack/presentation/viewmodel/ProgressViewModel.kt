package researchstack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.domain.model.priv.Bia
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CaloriePoint(val date: LocalDate, val calories: Double)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val biaDao: BiaDao,
) : ViewModel() {

    private val _caloriePoints = MutableStateFlow<List<CaloriePoint>>(emptyList())
    val caloriePoints: StateFlow<List<CaloriePoint>> = _caloriePoints

    private val _biaEntries = MutableStateFlow<List<Bia>>(emptyList())
    val biaEntries: StateFlow<List<Bia>> = _biaEntries

    init {
        viewModelScope.launch(Dispatchers.IO) {
            exerciseDao.getExercisesFrom(0).collect { list ->
                val zone = ZoneId.systemDefault()
                val grouped = list.groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() }
                    .map { (date, entries) ->
                        CaloriePoint(date, entries.sumOf { it.calorie })
                    }
                    .sortedBy { it.date }
                _caloriePoints.value = grouped
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            biaDao.getBetween(0, Long.MAX_VALUE).collect { list ->
                _biaEntries.value = list.sortedBy { it.timestamp }
            }
        }
    }
}
