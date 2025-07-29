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
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.pref.dataStore
import researchstack.data.datasource.local.room.dao.ExerciseDao
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
) : AndroidViewModel(application) {

    private val enrollmentDatePref = EnrollmentDatePref(application.dataStore)

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    private val _totalDurationMinutes = MutableStateFlow(0L)
    val totalDurationMinutes: StateFlow<Long> = _totalDurationMinutes

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val studyId = studyRepository.getActiveStudies().firstOrNull()?.firstOrNull()?.id
            if (studyId != null) {
                val enrollmentDate = enrollmentDatePref.getEnrollmentDate(studyId)
                enrollmentDate?.let { dateString ->
                    val weekStart = calculateCurrentWeekStart(LocalDate.parse(dateString))
                    val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    exerciseDao.getExercisesFrom(startMillis).collect { list ->
                        _exercises.value = list
                        val totalMillis = list.sumOf { it.endTime - it.startTime }
                        _totalDurationMinutes.value = TimeUnit.MILLISECONDS.toMinutes(totalMillis)
                    }
                }
            }
        }
    }

    private fun calculateCurrentWeekStart(enrollmentDate: LocalDate): LocalDate {
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(enrollmentDate, today)
        val weeks = days / 7
        return enrollmentDate.plusWeeks(weeks)
    }
}
