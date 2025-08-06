package researchstack.presentation.viewmodel

import android.app.Application
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_INT_TO_STRING_MAP
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
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.domain.model.healthConnect.Exercise
import researchstack.domain.repository.StudyRepository
import researchstack.presentation.util.kgToLbs
import researchstack.presentation.util.toDecimalFormat
import researchstack.domain.model.priv.Bia
import researchstack.domain.model.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class WeeklyProgressViewModel @Inject constructor(
    application: Application,
    private val studyRepository: StudyRepository,
    private val exerciseDao: ExerciseDao,
    private val biaDao: BiaDao,
    private val userProfileDao: UserProfileDao,
) : AndroidViewModel(application) {

    companion object {
        const val ACTIVITY_GOAL_MINUTES = 150
        const val RESISTANCE_SESSION_GOAL = 2
    }

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault())

    private val enrollmentDatePref = EnrollmentDatePref(application.dataStore)

    private var enrollmentDate: LocalDate? = null
    private var currentWeekIndex = 0
    private var maxWeekIndex = 0
    private var progressJob: Job? = null
    private var isMetricUnit: Boolean = true

    private val _weekStart = MutableStateFlow(LocalDate.now())
    val weekStart: StateFlow<LocalDate> = _weekStart

    private val _weekDays = MutableStateFlow<List<LocalDate>>(emptyList())
    val weekDays: StateFlow<List<LocalDate>> = _weekDays

    private val _activityMinutes = MutableStateFlow(0)
    val activityMinutes: StateFlow<Int> = _activityMinutes

    private val _resistanceMinutes = MutableStateFlow(0)
    val resistanceMinutes: StateFlow<Int> = _resistanceMinutes

    private val _activityCalories = MutableStateFlow(0)
    val activityCalories: StateFlow<Int> = _activityCalories

    private val _resistanceCalories = MutableStateFlow(0)
    val resistanceCalories: StateFlow<Int> = _resistanceCalories

    private val _activityProgressPercent = MutableStateFlow(0)
    val activityProgressPercent: StateFlow<Int> = _activityProgressPercent

    private val _resistanceProgressPercent = MutableStateFlow(0)
    val resistanceProgressPercent: StateFlow<Int> = _resistanceProgressPercent

    private val _weightProgressPercent = MutableStateFlow(0)
    val weightProgressPercent: StateFlow<Int> = _weightProgressPercent

    private val _biaProgressPercent = MutableStateFlow(0)
    val biaProgressPercent: StateFlow<Int> = _biaProgressPercent

    private val _hasData = MutableStateFlow(true)
    val hasData: StateFlow<Boolean> = _hasData

    private val _canNavigatePrevious = MutableStateFlow(false)
    val canNavigatePrevious: StateFlow<Boolean> = _canNavigatePrevious

    private val _canNavigateNext = MutableStateFlow(false)
    val canNavigateNext: StateFlow<Boolean> = _canNavigateNext

    private val _activityDetails = MutableStateFlow<List<ExerciseDetailUi>>(emptyList())
    val activityDetails: StateFlow<List<ExerciseDetailUi>> = _activityDetails

    private val _resistanceDetails = MutableStateFlow<List<ExerciseDetailUi>>(emptyList())
    val resistanceDetails: StateFlow<List<ExerciseDetailUi>> = _resistanceDetails

    private val _weightDetails = MutableStateFlow<List<WeightDetailUi>>(emptyList())
    val weightDetails: StateFlow<List<WeightDetailUi>> = _weightDetails

    private val _biaDetails = MutableStateFlow<List<BiaDetailUi>>(emptyList())
    val biaDetails: StateFlow<List<BiaDetailUi>> = _biaDetails

    private val _daysWithExercise = MutableStateFlow<Set<LocalDate>>(emptySet())
    val daysWithExercise: StateFlow<Set<LocalDate>> = _daysWithExercise

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
            isMetricUnit = userProfileDao.getLatest().firstOrNull()?.isMetricUnit != false

            launch {
                exerciseDao.getExercisesFrom(startMillis).collect { list ->
                    val weekList = list.filter { it.startTime < endMillis }
                    val resistanceList = weekList.filter { it.isResistance }
                    val activityList = weekList.filterNot { it.isResistance }

                    _activityDetails.value = activityList
                        .sortedBy { it.startTime }
                        .map { it.toDetailUi() }
                    _resistanceDetails.value = resistanceList
                        .sortedBy { it.startTime }
                        .map { it.toDetailUi() }

                    val totalMillis = activityList.sumOf { it.endTime - it.startTime }
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis).toInt()
                    _activityMinutes.value = minutes

                    _activityCalories.value = activityList.sumOf { it.calorie.toInt() }

                    val resistanceMillis = resistanceList.sumOf { it.endTime - it.startTime }
                    val resistanceMinutes =
                        TimeUnit.MILLISECONDS.toMinutes(resistanceMillis).toInt()
                    _resistanceMinutes.value = resistanceMinutes

                    _resistanceCalories.value = resistanceList.sumOf { it.calorie.toInt() }

                    _activityProgressPercent.value =
                        ((minutes * 100f) / ACTIVITY_GOAL_MINUTES).coerceAtMost(100f).toInt()
                    _resistanceProgressPercent.value =
                        ((resistanceList.size * 100f) / RESISTANCE_SESSION_GOAL).coerceAtMost(100f).toInt()

                    _daysWithExercise.value = weekList.map {
                        Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
                    }.toSet()

                    _hasData.value = weekList.isNotEmpty()
                }
            }

            launch {
                biaDao.getBetween(startMillis, endMillis).collect { list ->
                    _biaDetails.value = list.sortedBy { it.timestamp }.map { it.toDetailUi(isMetricUnit) }
                    _biaProgressPercent.value = if (list.isNotEmpty()) 100 else 0
                }
            }

            launch {
                userProfileDao.getBetween(startMillis, endMillis).collect { list ->
                    _weightDetails.value = list.sortedBy { it.timestamp }.map { it.toDetailUi(isMetricUnit) }
                    _weightProgressPercent.value = if (list.isNotEmpty()) 100 else 0
                }
            }
        }
    }

    private fun updateNavigationButtons() {
        _canNavigatePrevious.value = currentWeekIndex > 0
        _canNavigateNext.value = currentWeekIndex < maxWeekIndex
    }

    private fun Exercise.toDetailUi(): ExerciseDetailUi {
        val startInstant = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault())
        val endInstant = Instant.ofEpochMilli(endTime).atZone(ZoneId.systemDefault())
        val start = startInstant.toLocalTime().format(timeFormatter)
        val end = endInstant.toLocalTime().format(timeFormatter)
        val date = startInstant.toLocalDate().format(dateFormatter)
        val name = exerciseName
        val duration = TimeUnit.MILLISECONDS.toMinutes(endTime - startTime).toInt()
        return ExerciseDetailUi(
            name = name,
            date = date,
            startTime = start,
            endTime = end,
            durationMinutes = duration,
            calories = calorie.toInt(),
            minHeartRate = minHeartRate.toInt(),
            maxHeartRate = maxHeartRate.toInt(),
        )
    }

    private fun UserProfile.toDetailUi(isMetric: Boolean): WeightDetailUi {
        val instant = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        val date = instant.toLocalDate().format(dateFormatter)
        val time = instant.toLocalTime().format(timeFormatter)
        val unit = if (!isMetric) "lbs" else "kg"
        val value = weight.kgToLbs(isMetric).toDecimalFormat(2)
        return WeightDetailUi(
            timestamp = "$date $time",
            weight = "$value $unit"
        )
    }

    private fun Bia.toDetailUi(isMetric: Boolean): BiaDetailUi {
        val instant = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        val date = instant.toLocalDate().format(dateFormatter)
        val time = instant.toLocalTime().format(timeFormatter)
        val unit = if (!isMetric) "lbs" else "kg"
        val muscle = skeletalMuscleMass.kgToLbs(isMetric).toDecimalFormat(2)
        val water = totalBodyWater.kgToLbs(isMetric).toDecimalFormat(2)
        return BiaDetailUi(
            timestamp = "$date $time",
            skeletalMuscleMass = "$muscle $unit",
            bodyFatPercent = bodyFatRatio.toDecimalFormat(2),
            totalBodyWater = "$water $unit",
            basalMetabolicRate = basalMetabolicRate.toDecimalFormat(2)
        )
    }
}

data class ExerciseDetailUi(
    val name: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val calories: Int,
    val minHeartRate: Int,
    val maxHeartRate: Int,
)

data class WeightDetailUi(
    val timestamp: String,
    val weight: String,
)

data class BiaDetailUi(
    val timestamp: String,
    val skeletalMuscleMass: String,
    val bodyFatPercent: Float,
    val totalBodyWater: String,
    val basalMetabolicRate: Float,
)

