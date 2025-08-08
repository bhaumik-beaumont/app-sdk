package researchstack.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.pref.dataStore
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.domain.repository.StudyRepository
import researchstack.util.WEEKLY_ACTIVITY_GOAL_MINUTES
import researchstack.util.WEEKLY_RESISTANCE_SESSION_COUNT
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class RewardsViewModel @Inject constructor(
    application: Application,
    private val studyRepository: StudyRepository,
    private val exerciseDao: ExerciseDao,
    private val biaDao: BiaDao,
    private val userProfileDao: UserProfileDao,
) : AndroidViewModel(application) {

    private val enrollmentDatePref = EnrollmentDatePref(application.dataStore)

    private val _activityRewardWeeks = MutableStateFlow<List<Int>>(emptyList())
    val activityRewardWeeks: StateFlow<List<Int>> = _activityRewardWeeks

    private val _resistanceRewardWeeks = MutableStateFlow<List<Int>>(emptyList())
    val resistanceRewardWeeks: StateFlow<List<Int>> = _resistanceRewardWeeks

    private val _biaRewardWeeks = MutableStateFlow<List<Int>>(emptyList())
    val biaRewardWeeks: StateFlow<List<Int>> = _biaRewardWeeks

    private val _weightRewardWeeks = MutableStateFlow<List<Int>>(emptyList())
    val weightRewardWeeks: StateFlow<List<Int>> = _weightRewardWeeks

    private val _championRewardWeeks = MutableStateFlow<List<Int>>(emptyList())
    val championRewardWeeks: StateFlow<List<Int>> = _championRewardWeeks

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val studyId = studyRepository.getActiveStudies().firstOrNull()?.firstOrNull()?.id
            if (studyId != null) {
                enrollmentDatePref.getEnrollmentDate(studyId)?.let { startStr ->
                    val startDate = LocalDate.parse(startStr)
                    loadRewards(startDate)
                }
            }
        }
    }

    private suspend fun loadRewards(start: LocalDate) {
        val today = LocalDate.now()
        val weeks = ChronoUnit.WEEKS.between(start, today).toInt().coerceAtLeast(0)
        val activity = mutableListOf<Int>()
        val resistance = mutableListOf<Int>()
        val bia = mutableListOf<Int>()
        val weight = mutableListOf<Int>()

        for (i in 0..weeks) {
            val weekStart = start.plusWeeks(i.toLong())
            val weekEnd = weekStart.plusDays(7)
            val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = weekEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val exercises = exerciseDao.getExercisesBetween(startMillis, endMillis).first()
            val activityMinutes = exercises.filterNot { it.isResistance }
                .sumOf { TimeUnit.MILLISECONDS.toMinutes(it.endTime - it.startTime).toInt() }
            val resistanceCount = exercises.count { it.isResistance }
            if (activityMinutes >= WEEKLY_ACTIVITY_GOAL_MINUTES) activity.add(i + 1)
            if (resistanceCount >= WEEKLY_RESISTANCE_SESSION_COUNT) resistance.add(i + 1)

            val biaCount = biaDao.countBetween(startMillis, endMillis).first()
            if (biaCount > 0) bia.add(i + 1)
            val weightCount = userProfileDao.countBetween(startMillis, endMillis).first()
            if (weightCount > 0) weight.add(i + 1)
        }

        _activityRewardWeeks.value = activity
        _resistanceRewardWeeks.value = resistance
        _biaRewardWeeks.value = bia
        _weightRewardWeeks.value = weight

        val champion = activity.toSet()
            .intersect(resistance.toSet())
            .intersect(bia.toSet())
            .intersect(weight.toSet())
            .toList()
            .sorted()
        _championRewardWeeks.value = champion
    }
}

