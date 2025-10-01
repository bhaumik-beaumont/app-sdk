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
import researchstack.util.logDataSync
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
    private val logTag = this::class.simpleName ?: "DashboardViewModel"

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
        logDataSync("Initializing DashboardViewModel; triggering initial refresh", tag = logTag)
        refreshData()
    }

    suspend fun ensureAuthenticated(): Boolean {
        logDataSync("Ensuring authentication via AuthRepositoryWrapper", tag = logTag)
        return runCatching { authRepositoryWrapper.getIdToken().isSuccess }
            .onSuccess { result ->
                logDataSync("Authentication result received: $result", tag = logTag)
            }
            .onFailure {
                logDataSync("Authentication check failed: ${it.message}", it, tag = logTag)
            }
            .getOrThrow()
    }

    fun refreshData() {
        logDataSync("refreshData invoked", tag = logTag)
        viewModelScope.launch(Dispatchers.IO) {
            logDataSync("refreshData coroutine launched on ${Thread.currentThread().name}", tag = logTag)
            val studyId = studyRepository.getActiveStudies().firstOrNull()?.firstOrNull()?.id
            logDataSync("Active study resolved to id=${studyId ?: "none"}", tag = logTag)
            if (studyId != null) {
                val enrollmentDate = enrollmentDatePref.getEnrollmentDate(studyId)
                logDataSync("Enrollment date fetched for $studyId: ${enrollmentDate ?: "none"}", tag = logTag)
                enrollmentDate?.let { dateString ->
                    val enrollmentLocalDate = LocalDate.parse(dateString)
                    logDataSync("Parsed enrollment date $dateString into $enrollmentLocalDate", tag = logTag)
                    val weekStart = calculateCurrentWeekStart(enrollmentLocalDate)
                    _weekStart.value = weekStart
                    val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMillis = startMillis + TimeUnit.DAYS.toMillis(7)
                    logDataSync(
                        "Week window established from $startMillis to $endMillis for week starting $weekStart",
                        tag = logTag
                    )

                    val daysSinceEnrollment = ChronoUnit.DAYS.between(enrollmentLocalDate, LocalDate.now()).toInt()
                    _currentWeek.value = daysSinceEnrollment / 7 + 1
                    _currentDay.value = daysSinceEnrollment % 7 + 1
                    logDataSync(
                        "Current week=${_currentWeek.value}, day=${_currentDay.value} computed from enrollment",
                        tag = logTag
                    )
                    updateComplianceMessages()

                    launch {
                        logDataSync("Collecting BIA counts between $startMillis and $endMillis", tag = logTag)
                        biaDao.countBetween(startMillis, endMillis).collect { count ->
                            logDataSync("BIA count emission received: $count", tag = logTag)
                            _biaCount.value = count
                            val progress = if (count > 0) 100 else 0
                            _biaProgressPercent.value = progress
                            updateComplianceMessages()
                        }
                    }
                    launch {
                        logDataSync("Collecting weight entries between $startMillis and $endMillis", tag = logTag)
                        userProfileDao.countBetween(startMillis, endMillis).collect { count ->
                            logDataSync("Weight count emission received: $count", tag = logTag)
                            val progress = if (count > 0) 100 else 0
                            _weightProgressPercent.value = progress
                            _weightCount.value = count
                            updateComplianceMessages()
                        }
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        logDataSync("Collecting latest user profile for weight display", tag = logTag)
                        userProfileDao.getLatest().collect { profile ->
                            logDataSync("Latest profile emission received: ${profile != null}", tag = logTag)
                            profile?.let {
                                val unit = if (it.isMetricUnit == false) "lbs" else "kg"
                                val value = it.weight.kgToLbs(it.isMetricUnit == true)
                                logDataSync("Weight converted to $value $unit", tag = logTag)
                                _weight.value = "${value.toDecimalFormat(2)} $unit"
                                logDataSync("Weight state updated to ${_weight.value}", tag = logTag)
                            }
                        }
                    }

                    exerciseDao.getExercisesFrom(startMillis).collect { list ->
                        logDataSync("Exercise list emission received with size=${list.size}", tag = logTag)
                        val resistanceList = list.filter { it.isResistance }
                        val exerciseList = list.filterNot {it.isResistance }

                        _resistanceExercises.value = resistanceList
                        _exercises.value = exerciseList
                        logDataSync(
                            "Resistance exercises=${resistanceList.size}, cardio exercises=${exerciseList.size}",
                            tag = logTag
                        )

                        val totalMillis = exerciseList.sumOf { it.endTime - it.startTime }
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis)
                        _totalDurationMinutes.value = minutes
                        val progress = ((minutes * 100f) / WEEKLY_ACTIVITY_GOAL_MINUTES).coerceAtMost(100f)
                        _activityProgressPercent.value = progress.toInt()
                        logDataSync(
                            "Updated activity duration=$minutes minutes with progress=${_activityProgressPercent.value}%",
                            tag = logTag
                        )

                        val resistanceMillis = resistanceList.sumOf { it.endTime - it.startTime }
                        val resistanceMinutes = TimeUnit.MILLISECONDS.toMinutes(resistanceMillis)
                        _resistanceDurationMinutes.value = resistanceMinutes
                        val resistanceProgress = resistanceList.size * 100 / WEEKLY_RESISTANCE_SESSION_COUNT
                        _resistanceProgressPercent.value = if (resistanceProgress > 100) {
                            100
                        } else {
                            resistanceProgress
                        }
                        logDataSync(
                            "Updated resistance duration=$resistanceMinutes minutes with progress=${_resistanceProgressPercent.value}%",
                            tag = logTag
                        )
                        updateComplianceMessages()
                    }
                }
            } else {
                logDataSync("No active study found; skipping refreshData processing", tag = logTag)
            }
        }
    }

    private fun updateComplianceMessages() {
        val messages = mutableListOf<String>()
        val currentDay = _currentDay.value
        val totalActivityMinutes = _totalDurationMinutes.value
        logDataSync(
            "Evaluating compliance messages for day=$currentDay with activityMinutes=$totalActivityMinutes, " +
                "resistanceSessions=${_resistanceExercises.value.size}, biaCount=${_biaCount.value}, " +
                "weightCount=${_weightCount.value}",
            tag = logTag
        )
        if (
            (currentDay in 3 until 5 && totalActivityMinutes < 50L) ||
            (currentDay in 5 until 7 && totalActivityMinutes < 100L) ||
            (currentDay == 7 && totalActivityMinutes < WEEKLY_ACTIVITY_GOAL_MINUTES.toLong())
        ) {
            messages += getActivityMessage()
            logDataSync("Added activity compliance message", tag = logTag)
        }
        val resistanceSessions = _resistanceExercises.value.size
        if (
            (currentDay in 4 until 7 && resistanceSessions < 1) ||
            (currentDay == 7 && resistanceSessions < WEEKLY_RESISTANCE_SESSION_COUNT)
        ) {
            messages += getResistanceMessage()
            logDataSync("Added resistance compliance message", tag = logTag)
        }
        if (
            currentDay == 7 &&
            (_biaCount.value < MINIMUM_BIA_ENTRIES_PER_WEEK ||
                _weightCount.value < MINIMUM_WEIGHT_ENTRIES_PER_WEEK)
        ) {
            messages += getBiaMessage()
            logDataSync("Added BIA/weight compliance message", tag = logTag)
        }
        _complianceMessages.value = messages
        logDataSync("Compliance messages updated: ${messages.joinToString()} (total=${messages.size})", tag = logTag)
    }

    private fun calculateCurrentWeekStart(enrollmentDate: LocalDate): LocalDate {
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(enrollmentDate, today)
        val weeks = days / 7
        logDataSync(
            "calculateCurrentWeekStart: today=$today, enrollmentDate=$enrollmentDate, days=$days, weeks=$weeks",
            tag = logTag
        )
        return enrollmentDate.plusWeeks(weeks).also {
            logDataSync("Week start calculated as $it", tag = logTag)
        }
    }
}
