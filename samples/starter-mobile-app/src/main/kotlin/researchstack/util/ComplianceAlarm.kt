package researchstack.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

private const val COMPLIANCE_REQUEST_CODE = 1000
private const val CHECK_HOUR = 18

fun scheduleComplianceCheck(context: Context) {
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        COMPLIANCE_REQUEST_CODE,
        Intent(context, ComplianceCheckReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val now = LocalDateTime.now()
    var nextTrigger = now.withHour(CHECK_HOUR).withMinute(0).withSecond(0).withNano(0)
    if (!nextTrigger.isAfter(now)) {
        nextTrigger = nextTrigger.plusDays(1)
    }
    val triggerAt = nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
        // SCHEDULE_EXACT_ALARM is intended for apps like clocks; fall back to an
        // inexact alarm when the permission isn't granted.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    } else {
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}
