package com.milarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.milarm.milarm

/** Alarms are cleared on reboot and must be recomputed when the clock or time zone changes. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val app = context.milarm
                app.scheduler.rescheduleAll(app.repository.alarms.value)
            }
        }
    }
}
