package researchstack.presentation.util

import android.app.AlarmManager
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

fun ignoreBatteryOptimization(context: Context) {
    val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
    if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) return
    context.startActivity(
        Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:" + context.packageName)
        }
    )
}

fun checkAlarmPermission(context: Context){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(("package:" + context.packageName).toUri())
            context.startActivity(intent)
        }
    }
}
