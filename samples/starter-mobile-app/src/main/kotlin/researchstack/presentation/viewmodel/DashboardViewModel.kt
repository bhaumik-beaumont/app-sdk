package researchstack.presentation.viewmodel

import android.app.Application
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.pref.dataStore
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.domain.model.healthConnect.Exercise
import researchstack.domain.repository.StudyRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val studyRepository: StudyRepository,
    private val exerciseDao: ExerciseDao,
    private val biaDao: BiaDao,
    private val userProfileDao: UserProfileDao,
) : AndroidViewModel(application) {

    companion object {
        const val ACTIVITY_GOAL_MINUTES = 150
    }

    private val enrollmentDatePref = EnrollmentDatePref(application.dataStore)

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    private val _resistanceExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val resistanceExercises: StateFlow<List<Exercise>> = _resistanceExercises

    private val _totalDurationMinutes = MutableStateFlow(0L)
    val totalDurationMinutes: StateFlow<Long> = _totalDurationMinutes

    private val _resistanceDurationMinutes = MutableStateFlow(0L)
    val resistanceDurationMinutes: StateFlow<Long> = _resistanceDurationMinutes

    private val _activityProgressPercent = MutableStateFlow(0)
    val activityProgressPercent: StateFlow<Int> = _activityProgressPercent

    private val _resistanceProgressPercent = MutableStateFlow(0)
    val resistanceProgressPercent: StateFlow<Int> = _resistanceProgressPercent

    private val _weekStart = MutableStateFlow(LocalDate.now())
    val weekStart: StateFlow<LocalDate> = _weekStart

    private val _biaCount = MutableStateFlow(0)
    val biaCount: StateFlow<Int> = _biaCount

    private val _biaProgressPercent = MutableStateFlow(0)
    val biaProgressPercent: StateFlow<Int> = _biaProgressPercent

    private val _weight = MutableStateFlow("--")
    val weight: StateFlow<String> = _weight

    init {
        refreshData()
        viewModelScope.launch(Dispatchers.IO) {
            userProfileDao.getLatest().collect { profile ->
                profile?.let {
                    val unit = if (it.isMetricUnit == true) "kg" else "lb"
                    val value = it.weight.toInt()
                    _weight.value = "$value $unit"
                }
            }
        }
    }

    fun refreshData(){
        viewModelScope.launch(Dispatchers.IO) {
            val studyId = studyRepository.getActiveStudies().firstOrNull()?.firstOrNull()?.id
            if (studyId != null) {
                val enrollmentDate = enrollmentDatePref.getEnrollmentDate(studyId)
                enrollmentDate?.let { dateString ->
                    val weekStart = calculateCurrentWeekStart(LocalDate.parse(dateString))
                    _weekStart.value = weekStart
                    val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMillis = startMillis + TimeUnit.DAYS.toMillis(7)

                    launch {
                        biaDao.countBetween(startMillis, endMillis).collect { count ->
                            _biaCount.value = count
                            val progress = if (count > 0) 100 else 0
                            _biaProgressPercent.value = progress
                        }
                    }

                    exerciseDao.getExercisesFrom(startMillis).collect { list ->
                        val resistanceList = list.filter { isResistance(it.exerciseType.toInt()) }
                        val exerciseList = list.filterNot { isResistance(it.exerciseType.toInt()) }

                        _resistanceExercises.value = resistanceList
                        _exercises.value = exerciseList

                        val totalMillis = exerciseList.sumOf { it.endTime - it.startTime }
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis)
                        _totalDurationMinutes.value = minutes
                        val progress = ((minutes * 100f) / ACTIVITY_GOAL_MINUTES).coerceAtMost(100f)
                        _activityProgressPercent.value = progress.toInt()

                        val resistanceMillis = resistanceList.sumOf { it.endTime - it.startTime }
                        val resistanceMinutes = TimeUnit.MILLISECONDS.toMinutes(resistanceMillis)
                        _resistanceDurationMinutes.value = resistanceMinutes
                        val resistanceProgress = ((resistanceMinutes * 100f) / ACTIVITY_GOAL_MINUTES).coerceAtMost(100f)
                        _resistanceProgressPercent.value = resistanceProgress.toInt()
                    }
                }
            }
        }
    }

    fun isResistance(exerciseType:Int) : Boolean{
        return when(exerciseType){
            ExerciseSessionRecord.Companion.EXERCISE_TYPE_STRENGTH_TRAINING,
            ExerciseSessionRecord.Companion.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
            ExerciseSessionRecord.Companion.EXERCISE_TYPE_PILATES,
            ExerciseSessionRecord.Companion.EXERCISE_TYPE_STRETCHING,
            ExerciseSessionRecord.Companion.EXERCISE_TYPE_YOGA,
            ExerciseSessionRecord.Companion.EXERCISE_TYPE_CALISTHENICS->
                true

            else -> false
        }

    }

    private fun calculateCurrentWeekStart(enrollmentDate: LocalDate): LocalDate {
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(enrollmentDate, today)
        val weeks = days / 7
        return enrollmentDate.plusWeeks(weeks)
    }
}
