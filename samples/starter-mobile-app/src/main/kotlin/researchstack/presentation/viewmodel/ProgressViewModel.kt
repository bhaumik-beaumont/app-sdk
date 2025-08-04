package researchstack.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.domain.model.priv.Bia
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    application: Application,
    private val exerciseDao: ExerciseDao,
    private val biaDao: BiaDao,
) : AndroidViewModel(application) {

    private val dayFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

    private val _caloriesByDate = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val caloriesByDate: StateFlow<List<Pair<String, Float>>> = _caloriesByDate

    private val _biaEntries = MutableStateFlow<List<Bia>>(emptyList())
    val biaEntries: StateFlow<List<Bia>> = _biaEntries

    init {
        viewModelScope.launch(Dispatchers.IO) {
            exerciseDao.getExercisesFrom(0).collect { list ->
                val grouped = list.groupBy {
                    Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
                }.toSortedMap()
                _caloriesByDate.value = grouped.map { (date, exercises) ->
                    date.format(dayFormatter) to exercises.sumOf { it.calorie }.toFloat()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            biaDao.getBetween(0, Long.MAX_VALUE).collect { list ->
                _biaEntries.value = list.sortedBy { it.timestamp }
            }
        }
    }
}
