package com.milarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.milarm.core.Alarm
import com.milarm.core.NextTrigger
import com.milarm.ui.MainActivity
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Schedules the next occurrence after [from], or cancels the alarm if it is switched off. */
    fun schedule(alarm: Alarm, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return null
        }
        val at = NextTrigger.after(alarm, from)
        setAlarmClock(at.toInstant().toEpochMilli(), ringIntent(alarm.id, round = 0, requestCode = alarm.id))
        return at
    }

    fun scheduleSnooze(alarmId: Int, round: Int, minutes: Int) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        setAlarmClock(at, ringIntent(alarmId, round, snoozeRequestCode(alarmId)))
    }

    fun cancel(alarmId: Int) {
        alarmManager.cancel(ringIntent(alarmId, 0, alarmId))
        alarmManager.cancel(ringIntent(alarmId, 0, snoozeRequestCode(alarmId)))
    }

    fun rescheduleAll(alarms: List<Alarm>) = alarms.forEach { schedule(it) }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun setAlarmClock(triggerAtMillis: Long, operation: PendingIntent) {
        if (!canScheduleExact()) {
            // Android 12 without the exact alarm permission: ring late rather than never.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            return
        }
        val showIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation)
    }

    private fun ringIntent(alarmId: Int, round: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_RING)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmReceiver.EXTRA_ROUND, round)
        return PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun snoozeRequestCode(alarmId: Int) = SNOOZE_REQUEST_OFFSET + alarmId

    private companion object {
        const val SNOOZE_REQUEST_OFFSET = 1_000_000
    }
}
