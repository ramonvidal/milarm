package com.milarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.milarm.milarm
import java.time.ZonedDateTime

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RING) return
        val app = context.milarm
        val alarm = app.repository.get(intent.getIntExtra(EXTRA_ALARM_ID, -1)) ?: return

        if (alarm.isRepeating) {
            // Start a minute ahead so an alarm that fires a few ms early is not scheduled again today.
            app.scheduler.schedule(alarm, ZonedDateTime.now().plusMinutes(1))
        } else {
            app.repository.upsert(alarm.copy(enabled = false))
        }

        AlarmService.start(context, alarm.id, intent.getIntExtra(EXTRA_ROUND, 0))
    }

    companion object {
        const val ACTION_RING = "com.milarm.action.RING"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ROUND = "round"
    }
}
