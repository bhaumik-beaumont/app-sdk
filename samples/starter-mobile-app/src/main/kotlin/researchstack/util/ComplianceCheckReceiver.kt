package researchstack.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import researchstack.R
import researchstack.data.datasource.local.pref.ComplianceReminderPref
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.pref.ComplianceReminderPref.Type
import researchstack.data.datasource.local.pref.dataStore
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.domain.model.healthConnect.Exercise
import researchstack.domain.repository.StudyRepository
import researchstack.presentation.initiate.MainActivity
import researchstack.presentation.service.DaggerBroadcastReceiver
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val CHANNEL_ID = "weekly_compliance"
private const val WEEKLY_ACTIVITY_GOAL_MINUTES = 150
private const val WEEKLY_RESISTANCE_SESSION_COUNT = 2
private const val MINIMUM_BIA_ENTRIES_PER_WEEK = 1
private const val MINIMUM_WEIGHT_ENTRIES_PER_WEEK = 1

@AndroidEntryPoint
class ComplianceCheckReceiver : DaggerBroadcastReceiver() {

    @Inject
    lateinit var studyRepository: StudyRepository

    @Inject
    lateinit var exerciseDao: ExerciseDao

    @Inject
    lateinit var biaDao: BiaDao

    @Inject
    lateinit var userProfileDao: UserProfileDao

    lateinit var enrollmentDatePref: EnrollmentDatePref

    @Inject
    lateinit var reminderPref: ComplianceReminderPref

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        enrollmentDatePref= EnrollmentDatePref(context.dataStore)
        scheduleComplianceCheck(context)
        runBlocking {
            val studyId = studyRepository.getActiveStudies().first().firstOrNull()?.id ?: return@runBlocking
            val enrollment = enrollmentDatePref.getEnrollmentDate(studyId)?.let { LocalDate.parse(it) } ?: return@runBlocking
            val today = LocalDate.now()
            val days = ChronoUnit.DAYS.between(enrollment, today).toInt().coerceAtLeast(0)
            val weekStart = enrollment.plusDays((days / 7) * 7L)
            val dayOfWeek = days % 7 + 1
//            val dayOfWeek = 7
            val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = weekStart.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val exercises = exerciseDao.getExercisesFrom(startMillis).first().filter { it.startTime < endMillis }
            val activityCompliant = isActivityCompliant(exercises)
            val resistanceCompliant = isResistanceCompliant(exercises)
            val biaCount = biaDao.countBetween(startMillis, endMillis).first()
            val weightCount = userProfileDao.countBetween(startMillis, endMillis).first()
            val biaCompliant = isBiaCompliant(biaCount)
            val weightCompliant = isWeightCompliant(weightCount)

            if (activityCompliant) reminderPref.clear(Type.ACTIVITY)
            if (resistanceCompliant) reminderPref.clear(Type.RESISTANCE)
            if (biaCompliant && weightCompliant) reminderPref.clear(Type.BIA)

            val todayStr = today.toString()
            var missingActivity = false
            var missingResistance = false
            var missingBia = false

            if (dayOfWeek >= 3 && !activityCompliant && reminderPref.getLastReminderDate(Type.ACTIVITY) != todayStr) {
                missingActivity = true
                reminderPref.saveReminderDate(Type.ACTIVITY, todayStr)
            }
            if (dayOfWeek >= 4 && !resistanceCompliant && reminderPref.getLastReminderDate(Type.RESISTANCE) != todayStr) {
                missingResistance = true
                reminderPref.saveReminderDate(Type.RESISTANCE, todayStr)
            }
            if (dayOfWeek == 7 && (!biaCompliant || !weightCompliant) && reminderPref.getLastReminderDate(Type.BIA) != todayStr) {
                missingBia = true
                reminderPref.saveReminderDate(Type.BIA, todayStr)
            }

            if (missingActivity || missingResistance || missingBia) {
                val message = buildMessage(missingActivity, missingResistance, missingBia)
                showNotification(context, message)
            }
        }
    }

    private fun isActivityCompliant(exercises: List<Exercise>): Boolean {
        val minutes = exercises.filter { !isResistance(it.exerciseType.toInt()) }
            .sumOf { TimeUnit.MILLISECONDS.toMinutes(it.endTime - it.startTime) }
        return minutes >= WEEKLY_ACTIVITY_GOAL_MINUTES
    }

    private fun isResistanceCompliant(exercises: List<Exercise>): Boolean {
        val sessions = exercises.count { isResistance(it.exerciseType.toInt()) }
        return sessions >= WEEKLY_RESISTANCE_SESSION_COUNT
    }

    private fun isBiaCompliant(count: Int) = count >= MINIMUM_BIA_ENTRIES_PER_WEEK

    private fun isWeightCompliant(count: Int) = count >= MINIMUM_WEIGHT_ENTRIES_PER_WEEK

    private fun showNotification(context: Context, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
        )
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("openWeeklyProgress", true)
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.flexed_biceps)
            .setContentTitle("Weekly Progress Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        manager.notify(notificationId, notification)
    }

    private fun buildMessage(activity: Boolean, resistance: Boolean, bia: Boolean): String {
        return when {
            activity && resistance && bia -> "You haven’t logged any activity, resistance training, or BIA/weight data this week. Let’s complete your weekly goals!"
            activity && resistance -> "No physical activity or resistance training logged yet. Let’s get moving!"
            activity && bia -> "You haven’t logged activity or completed BIA/weight measurements this week. Let’s catch up!"
            resistance && bia -> "Still missing resistance training and BIA/weight data this week. Time to take action!"
            activity -> "Don’t forget to get moving! You haven’t logged any physical activity this week."
            resistance -> "Time to train! You haven’t completed any resistance training this week."
            bia -> "Please log your BIA or weight. Baseline measurements are due this week."
            else -> ""
        }
    }

    private fun isResistance(exerciseType: Int): Boolean {
        return when (exerciseType) {
            androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
            androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
            androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
            androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
            androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> true
            else -> false
        }
    }
}
