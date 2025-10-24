package researchstack.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
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
const val WEEKLY_ACTIVITY_GOAL_MINUTES = 150
const val WEEKLY_RESISTANCE_SESSION_COUNT = 2
const val MINIMUM_BIA_ENTRIES_PER_WEEK = 1
const val MINIMUM_WEIGHT_ENTRIES_PER_WEEK = 1

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

            val activityCompliant = activityMinutes >= WEEKLY_ACTIVITY_GOAL_MINUTES.toLong()
            val resistanceCompliant = resistanceSessions >= WEEKLY_RESISTANCE_SESSION_COUNT
            val biaCompliant = isBiaCompliant(biaCount)
            val weightCompliant = isWeightCompliant(weightCount)

            if (activityCompliant) reminderPref.clear(Type.ACTIVITY)
            if (resistanceCompliant) reminderPref.clear(Type.RESISTANCE)
            if (biaCompliant && weightCompliant) reminderPref.clear(Type.BIA)

            val todayStr = today.toString()
            val messages = mutableListOf<String>()

            val shouldRemindActivity = when {
                dayOfWeek in 3 until 5 -> activityMinutes < 50L
                dayOfWeek in 5 until 7 -> activityMinutes < 100L
                dayOfWeek == 7 -> activityMinutes < WEEKLY_ACTIVITY_GOAL_MINUTES.toLong()
                else -> false
            }

            if (shouldRemindActivity && reminderPref.getLastReminderDate(Type.ACTIVITY) != todayStr) {
                reminderPref.saveReminderDate(Type.ACTIVITY, todayStr)
                messages += getActivityMessage()
            }
            val shouldRemindResistance = when {
                dayOfWeek in 4 until 7 -> resistanceSessions < 1
                dayOfWeek == 7 -> resistanceSessions < WEEKLY_RESISTANCE_SESSION_COUNT
                else -> false
            }

            if (shouldRemindResistance && reminderPref.getLastReminderDate(Type.RESISTANCE) != todayStr) {
                reminderPref.saveReminderDate(Type.RESISTANCE, todayStr)
                messages += getResistanceMessage()
            }
            if (dayOfWeek == 7 && reminderPref.getLastReminderDate(Type.BIA) != todayStr) {
                val biaMissing = !biaCompliant
                val weightMissing = !weightCompliant
                if (biaMissing || weightMissing) {
                    reminderPref.saveReminderDate(Type.BIA, todayStr)
                    messages += getBiaMessage()
                }
            }

            if (messages.isNotEmpty()) {
                showNotification(context, messages)
            }
        }
    }

    private fun isBiaCompliant(count: Int) = count >= MINIMUM_BIA_ENTRIES_PER_WEEK

    private fun isWeightCompliant(count: Int) = count >= MINIMUM_WEIGHT_ENTRIES_PER_WEEK

    private fun showNotification(context: Context, messages: List<String>) {
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

        val message = " ⚠\uFE0F Alert: " +
            messages.joinToString("\n") +
            " Please complete this as soon as possible to stay on track!"

        val isYellow = messages.size == 1
        val bgRes = if (isYellow) {
            R.drawable.notification_bg_yellow
        } else {
            R.drawable.notification_bg_red
        }
        val textColor = if (isYellow) Color.BLACK else Color.WHITE

        val expanded = RemoteViews(context.packageName, R.layout.notification_compliance).apply {
            setTextViewText(R.id.notification_title, "Weekly Progress Reminder")
            setViewVisibility(R.id.notification_text, View.VISIBLE)
            setTextViewText(R.id.notification_text, message)
            setInt(R.id.notification_root, "setBackgroundResource", bgRes)
            setTextColor(R.id.notification_title, textColor)
            setTextColor(R.id.notification_text, textColor)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.flexed_biceps)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(expanded)
            .setCustomBigContentView(expanded)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        manager.notify(notificationId, notification)
    }

}
