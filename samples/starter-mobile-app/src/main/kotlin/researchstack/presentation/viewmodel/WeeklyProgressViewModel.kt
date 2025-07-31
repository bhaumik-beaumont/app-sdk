package researchstack.presentation.viewmodel

import android.app.Application
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.pref.dataStore
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.domain.repository.StudyRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class WeeklyProgressViewModel @Inject constructor(
    application: Application,
    private val studyRepository: StudyRepository,
    private val exerciseDao: ExerciseDao,
) : AndroidViewModel(application) {

    companion object {
        const val ACTIVITY_GOAL_MINUTES = 150
    }

    private val enrollmentDatePref = EnrollmentDatePref(application.dataStore)

    private var enrollmentDate: LocalDate? = null
    private var currentWeekIndex = 0
    private var maxWeekIndex = 0
    private var progressJob: Job? = null

    private val _weekStart = MutableStateFlow(LocalDate.now())
    val weekStart: StateFlow<LocalDate> = _weekStart

    private val _weekDays = MutableStateFlow<List<LocalDate>>(emptyList())
    val weekDays: StateFlow<List<LocalDate>> = _weekDays

    private val _activityMinutes = MutableStateFlow(0)
    val activityMinutes: StateFlow<Int> = _activityMinutes

    private val _resistanceMinutes = MutableStateFlow(0)
    val resistanceMinutes: StateFlow<Int> = _resistanceMinutes

    private val _activityProgressPercent = MutableStateFlow(0)
    val activityProgressPercent: StateFlow<Int> = _activityProgressPercent

    private val _resistanceProgressPercent = MutableStateFlow(0)
    val resistanceProgressPercent: StateFlow<Int> = _resistanceProgressPercent

    private val _hasData = MutableStateFlow(true)
    val hasData: StateFlow<Boolean> = _hasData

    private val _canNavigatePrevious = MutableStateFlow(false)
    val canNavigatePrevious: StateFlow<Boolean> = _canNavigatePrevious

    private val _canNavigateNext = MutableStateFlow(false)
    val canNavigateNext: StateFlow<Boolean> = _canNavigateNext

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val studyId = studyRepository.getActiveStudies().firstOrNull()?.firstOrNull()?.id
            if (studyId != null) {
                enrollmentDate = enrollmentDatePref.getEnrollmentDate(studyId)?.let { LocalDate.parse(it) }
                enrollmentDate?.let { start ->
                    val today = LocalDate.now()
                    val days = ChronoUnit.DAYS.between(start, today).toInt().coerceAtLeast(0)
                    currentWeekIndex = days / 7
                    maxWeekIndex = currentWeekIndex
                    _weekStart.value = start.plusDays((currentWeekIndex * 7).toLong())
                    _weekDays.value = (0..6).map { _weekStart.value.plusDays(it.toLong()) }
                    updateNavigationButtons()
                    loadProgressForWeek(_weekStart.value)
                }
            }
        }
    }

    fun navigateWeek(offset: Int) {
        val newIndex = currentWeekIndex + offset
        if (newIndex < 0 || newIndex > maxWeekIndex) return
        currentWeekIndex = newIndex
        enrollmentDate?.let { start ->
            _weekStart.value = start.plusDays((currentWeekIndex * 7).toLong())
            _weekDays.value = (0..6).map { _weekStart.value.plusDays(it.toLong()) }
            updateNavigationButtons()
            loadProgressForWeek(_weekStart.value)
        }
    }

    private fun loadProgressForWeek(start: LocalDate) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch(Dispatchers.IO) {
            val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = start.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            exerciseDao.getExercisesFrom(startMillis).collect { list ->
                val weekList = list.filter { it.startTime < endMillis }
                val resistanceList = weekList.filter { isResistance(it.exerciseType.toInt()) }
                val exerciseList = weekList.filterNot { isResistance(it.exerciseType.toInt()) }

                val totalMillis = exerciseList.sumOf { it.endTime - it.startTime }
                val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis).toInt()
                _activityMinutes.value = minutes

                val resistanceMillis = resistanceList.sumOf { it.endTime - it.startTime }
                val resistanceMinutes = TimeUnit.MILLISECONDS.toMinutes(resistanceMillis).toInt()
                _resistanceMinutes.value = resistanceMinutes

                _activityProgressPercent.value =
                    ((minutes * 100f) / ACTIVITY_GOAL_MINUTES).coerceAtMost(100f).toInt()
                _resistanceProgressPercent.value =
                    ((resistanceMinutes * 100f) / ACTIVITY_GOAL_MINUTES).coerceAtMost(100f).toInt()

                _hasData.value = weekList.isNotEmpty()
            }
        }
    }

    private fun updateNavigationButtons() {
        _canNavigatePrevious.value = currentWeekIndex > 0
        _canNavigateNext.value = currentWeekIndex < maxWeekIndex
    }

    private fun isResistance(exerciseType: Int): Boolean {
        return when (exerciseType) {
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> true

            else -> false
        }
    }
}

