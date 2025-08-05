package researchstack.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

private const val COMPLIANCE_REQUEST_CODE = 1000
private const val INTERVAL_MINUTE = 60_000L

fun scheduleComplianceCheck(context: Context) {
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        COMPLIANCE_REQUEST_CODE,
        Intent(context, ComplianceCheckReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val triggerAt = System.currentTimeMillis() + INTERVAL_MINUTE
    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
}
