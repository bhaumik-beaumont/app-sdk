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
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.domain.repository.StudyRepository
import researchstack.presentation.initiate.MainActivity
import researchstack.presentation.service.DaggerBroadcastReceiver
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val CHANNEL_ID = "weekly_compliance"

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

    @Inject
    lateinit var enrollmentDatePref: EnrollmentDatePref

    @Inject
    lateinit var reminderPref: ComplianceReminderPref

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        scheduleComplianceCheck(context)
        runBlocking {
            val studyId = studyRepository.getActiveStudies().first().firstOrNull()?.id ?: return@runBlocking
            val enrollment = enrollmentDatePref.getEnrollmentDate(studyId)?.let { LocalDate.parse(it) } ?: return@runBlocking
            val today = LocalDate.now()
            val days = ChronoUnit.DAYS.between(enrollment, today).toInt().coerceAtLeast(0)
            val weekStart = enrollment.plusDays((days / 7) * 7L)
            val dayOfWeek = days % 7 + 1
            val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val exercises = exerciseDao.getExercisesFrom(startMillis).first().filter { it.startTime < endMillis }
            val activityRecorded = exercises.any { !isResistance(it.exerciseType.toInt()) }
            val resistanceRecorded = exercises.any { isResistance(it.exerciseType.toInt()) }
            val biaCount = biaDao.countBetween(startMillis, endMillis).first()
            val weightCount = userProfileDao.countBetween(startMillis, endMillis).first()
            val biaOrWeightRecorded = (biaCount + weightCount) > 0

            if (activityRecorded) reminderPref.clear(Type.ACTIVITY)
            if (resistanceRecorded) reminderPref.clear(Type.RESISTANCE)
            if (biaOrWeightRecorded) reminderPref.clear(Type.BIA)

            val todayStr = today.toString()
            var missingActivity = false
            var missingResistance = false
            var missingBia = false

            if (dayOfWeek >= 3 && !activityRecorded && reminderPref.getLastReminderDate(Type.ACTIVITY) != todayStr) {
                missingActivity = true
                reminderPref.saveReminderDate(Type.ACTIVITY, todayStr)
            }
            if (dayOfWeek >= 4 && !resistanceRecorded && reminderPref.getLastReminderDate(Type.RESISTANCE) != todayStr) {
                missingResistance = true
                reminderPref.saveReminderDate(Type.RESISTANCE, todayStr)
            }
            if (dayOfWeek == 7 && !biaOrWeightRecorded && reminderPref.getLastReminderDate(Type.BIA) != todayStr) {
                missingBia = true
                reminderPref.saveReminderDate(Type.BIA, todayStr)
            }

            if (missingActivity || missingResistance || missingBia) {
                val message = buildMessage(missingActivity, missingResistance, missingBia)
                showNotification(context, message)
            }
        }
    }

    private fun showNotification(context: Context, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
            )
        }
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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Weekly Progress Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        manager.notify(2001, notification)
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
