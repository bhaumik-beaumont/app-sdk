package researchstack.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import researchstack.auth.data.repository.auth.AuthRepositoryWrapper
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.pref.dataStore
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.domain.model.healthConnect.Exercise
import researchstack.domain.repository.StudyRepository
import researchstack.presentation.util.kgToLbs
import researchstack.presentation.util.toDecimalFormat
import researchstack.util.MINIMUM_BIA_ENTRIES_PER_WEEK
import researchstack.util.MINIMUM_WEIGHT_ENTRIES_PER_WEEK
import researchstack.util.WEEKLY_ACTIVITY_GOAL_MINUTES
import researchstack.util.WEEKLY_RESISTANCE_SESSION_COUNT
import researchstack.util.getActivityMessage
import researchstack.util.getBiaMessage
import researchstack.util.getResistanceMessage
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
    private val authRepositoryWrapper: AuthRepositoryWrapper,
) : AndroidViewModel(application) {

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

    private val _weightProgressPercent = MutableStateFlow(0)
    val weightProgressPercent: StateFlow<Int> = _weightProgressPercent

    private val _weight = MutableStateFlow("--")
    val weight: StateFlow<String> = _weight

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek

    private val _currentDay = MutableStateFlow(1)
    val currentDay: StateFlow<Int> = _currentDay

    private val _weightCount = MutableStateFlow(0)
    val weightCount: StateFlow<Int> = _weightCount

    private val _complianceMessages = MutableStateFlow<List<String>>(emptyList())
    val complianceMessages: StateFlow<List<String>> = _complianceMessages

    init {
        refreshData()
    }

    suspend fun ensureAuthenticated(): Boolean =
        authRepositoryWrapper.getIdToken().isSuccess

    fun refreshData(){
        viewModelScope.launch(Dispatchers.IO) {
            val studyId = studyRepository.getActiveStudies().firstOrNull()?.firstOrNull()?.id
            if (studyId != null) {
                val enrollmentDate = enrollmentDatePref.getEnrollmentDate(studyId)
                enrollmentDate?.let { dateString ->
                    val enrollmentLocalDate = LocalDate.parse(dateString)
                    val weekStart = calculateCurrentWeekStart(enrollmentLocalDate)
                    _weekStart.value = weekStart
                    val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMillis = startMillis + TimeUnit.DAYS.toMillis(7)

                    val daysSinceEnrollment = ChronoUnit.DAYS.between(enrollmentLocalDate, LocalDate.now()).toInt()
                    _currentWeek.value = daysSinceEnrollment / 7 + 1
                    _currentDay.value = daysSinceEnrollment % 7 + 1
                    updateComplianceMessages()

                    launch {
                        biaDao.countBetween(startMillis, endMillis).collect { count ->
                            _biaCount.value = count
                            val progress = if (count > 0) 100 else 0
                            _biaProgressPercent.value = progress
                            updateComplianceMessages()
                        }
                    }
                    launch {
                        userProfileDao.countBetween(startMillis, endMillis).collect { count ->
                            val progress = if (count > 0) 100 else 0
                            _weightProgressPercent.value = progress
                            _weightCount.value = count
                            updateComplianceMessages()
                        }
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        userProfileDao.getLatest().collect { profile ->
                            profile?.let {
                                val unit = if (it.isMetricUnit == false) "lbs" else "kg"
                                val value = it.weight.kgToLbs(it.isMetricUnit == true)
                                _weight.value = "${value.toDecimalFormat(2)} $unit"
                            }
                        }
                    }

                    exerciseDao.getExercisesFrom(startMillis).collect { list ->
                        val resistanceList = list.filter { it.isResistance }
                        val exerciseList = list.filterNot {it.isResistance }

                        _resistanceExercises.value = resistanceList
                        _exercises.value = exerciseList

                        val totalMillis = exerciseList.sumOf { it.endTime - it.startTime }
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis)
                        _totalDurationMinutes.value = minutes
                        val progress = ((minutes * 100f) / WEEKLY_ACTIVITY_GOAL_MINUTES).coerceAtMost(100f)
                        _activityProgressPercent.value = progress.toInt()

                        val resistanceMillis = resistanceList.sumOf { it.endTime - it.startTime }
                        val resistanceMinutes = TimeUnit.MILLISECONDS.toMinutes(resistanceMillis)
                        _resistanceDurationMinutes.value = resistanceMinutes
                        val resistanceProgress = resistanceList.size * 100 / WEEKLY_RESISTANCE_SESSION_COUNT
                        _resistanceProgressPercent.value = if (resistanceProgress > 100) {
                            100
                        } else {
                            resistanceProgress
                        }
                        updateComplianceMessages()
                    }
                }
            }
        }
    }

    private fun updateComplianceMessages() {
        val messages = mutableListOf<String>()
        if (_currentDay.value >= 3 && _totalDurationMinutes.value < WEEKLY_ACTIVITY_GOAL_MINUTES) {
            messages += getActivityMessage()
        }
        if (_currentDay.value >= 4 && _resistanceExercises.value.size < WEEKLY_RESISTANCE_SESSION_COUNT) {
            messages += getResistanceMessage()
        }
        if (
            _currentDay.value == 7 &&
            (_biaCount.value < MINIMUM_BIA_ENTRIES_PER_WEEK ||
                _weightCount.value < MINIMUM_WEIGHT_ENTRIES_PER_WEEK)
        ) {
            messages += getBiaMessage()
        }
        _complianceMessages.value = messages
    }

    private fun calculateCurrentWeekStart(enrollmentDate: LocalDate): LocalDate {
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(enrollmentDate, today)
        val weeks = days / 7
        return enrollmentDate.plusWeeks(weeks)
    }
}
