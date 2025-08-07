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
            val activityMinutes = exercises.filter { !it.isResistance }
                .sumOf { TimeUnit.MILLISECONDS.toMinutes(it.endTime - it.startTime) }
            val resistanceSessions = exercises.count { it.isResistance }
            val biaCount = biaDao.countBetween(startMillis, endMillis).first()
            val weightCount = userProfileDao.countBetween(startMillis, endMillis).first()

            val activityCompliant = activityMinutes >= WEEKLY_ACTIVITY_GOAL_MINUTES
            val resistanceCompliant = resistanceSessions >= WEEKLY_RESISTANCE_SESSION_COUNT
            val biaCompliant = isBiaCompliant(biaCount)
            val weightCompliant = isWeightCompliant(weightCount)

            if (activityCompliant) reminderPref.clear(Type.ACTIVITY)
            if (resistanceCompliant) reminderPref.clear(Type.RESISTANCE)
            if (biaCompliant && weightCompliant) reminderPref.clear(Type.BIA)

            val todayStr = today.toString()
            val messages = mutableListOf<String>()

            if (dayOfWeek >= 3 && !activityCompliant && reminderPref.getLastReminderDate(Type.ACTIVITY) != todayStr) {
                reminderPref.saveReminderDate(Type.ACTIVITY, todayStr)
                messages += getActivityMessage(context, activityMinutes)
            }
            if (dayOfWeek >= 4 && !resistanceCompliant && reminderPref.getLastReminderDate(Type.RESISTANCE) != todayStr) {
                reminderPref.saveReminderDate(Type.RESISTANCE, todayStr)
                messages += getResistanceMessage(context, resistanceSessions)
            }
            if (dayOfWeek == 7 && reminderPref.getLastReminderDate(Type.BIA) != todayStr) {
                val biaMissing = !biaCompliant
                val weightMissing = !weightCompliant
                if (biaMissing || weightMissing) {
                    reminderPref.saveReminderDate(Type.BIA, todayStr)
                    if (biaMissing) messages += getBiaMessage(context, biaCount)
                    if (weightMissing) messages += getWeightMessage(context, weightCount)
                }
            }

            if (messages.isNotEmpty()) {
                showNotification(context, messages.joinToString("\n"))
            }
        }
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        manager.notify(notificationId, notification)
    }

    private fun getActivityMessage(context: Context, minutes: Long): String {
        return when {
            minutes == 0L -> context.getString(R.string.weekly_message_activity_none)
            minutes < WEEKLY_ACTIVITY_GOAL_MINUTES -> context.getString(
                R.string.weekly_message_activity_partial,
                minutes,
                WEEKLY_ACTIVITY_GOAL_MINUTES
            )
            else -> ""
        }
    }

    private fun getResistanceMessage(context: Context, count: Int): String {
        return when (count) {
            0 -> context.getString(R.string.weekly_message_resistance_none)
            1 -> context.getString(R.string.weekly_message_resistance_one)
            else -> ""
        }
    }

    private fun getBiaMessage(context: Context, count: Int): String {
        return if (count == 0) {
            context.getString(R.string.weekly_message_bia_none)
        } else {
            ""
        }
    }

    private fun getWeightMessage(context: Context, count: Int): String {
        return if (count == 0) {
            context.getString(R.string.weekly_message_weight_none)
        } else {
            ""
        }
    }
}
