package com.milarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.milarm.R
import com.milarm.core.Alarm
import com.milarm.ui.RingActivity
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Notifications {
    const val FOREGROUND_ID = 1
    private const val BAND_ID_A = 2
    private const val BAND_ID_B = 3

    private const val CHANNEL_RINGING = "ringing"
    private const val CHANNEL_BAND = "band"
    private const val CHANNEL_WAKE_CHECK = "wake_check"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                // Sound and vibration for ringing come from AlarmService, not the channel.
                NotificationChannel(CHANNEL_RINGING, "Ringing alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                },
                // Mi Fitness forwards these to the band. Silent on the phone so "band only" stays quiet.
                NotificationChannel(CHANNEL_BAND, "Band alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Turn on Milarm in Mi Fitness app notifications so these reach your band"
                    setSound(null, null)
                    enableVibration(false)
                },
                NotificationChannel(CHANNEL_WAKE_CHECK, "Wake-up check", NotificationManager.IMPORTANCE_LOW),
            ),
        )
    }

    fun ringing(context: Context, alarm: Alarm): Notification =
        base(context, CHANNEL_RINGING)
            .setContentTitle(title(alarm))
            .setContentText(timeText(alarm))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(ringScreen(context), true)
            .addAction(0, "Snooze", serviceAction(context, AlarmService.ACTION_SNOOZE))
            .addAction(0, "Dismiss", serviceAction(context, AlarmService.ACTION_DISMISS))
            .build()

    fun wakeCheck(context: Context, alarm: Alarm): Notification =
        base(context, CHANNEL_WAKE_CHECK)
            .setContentTitle("Wake-up check active")
            .setContentText(
                "Move around. The alarm rings again if no movement is seen within " +
                    "${alarm.wakeCheck.checkAfterMinutes} min.",
            )
            .setOngoing(true)
            .build()

    fun asking(context: Context, alarm: Alarm): Notification =
        base(context, CHANNEL_RINGING)
            .setContentTitle("Are you up?")
            .setContentText("Tap I'm up within ${alarm.wakeCheck.responseSeconds} s or the alarm rings again")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(ringScreen(context), true)
            .addAction(0, "I'm up", serviceAction(context, AlarmService.ACTION_IM_UP))
            .build()

    /**
     * Posts a fresh notification for the band. The band buzzes once per new notification, so the
     * previous one is removed and the id alternates to make Mi Fitness treat each as new.
     */
    fun postBandAlert(context: Context, title: String, text: String, previousId: Int): Int {
        val manager = context.getSystemService(NotificationManager::class.java)
        val id = if (previousId == BAND_ID_A) BAND_ID_B else BAND_ID_A
        manager.cancel(previousId)
        manager.notify(
            id,
            base(context, CHANNEL_BAND)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build(),
        )
        return id
    }

    fun cancelBandAlerts(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(BAND_ID_A)
        manager.cancel(BAND_ID_B)
    }

    fun title(alarm: Alarm): String = alarm.label.ifBlank { "Alarm" }

    fun timeText(alarm: Alarm): String =
        LocalTime.of(alarm.hour, alarm.minute).format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun base(context: Context, channel: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentIntent(ringScreen(context))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    private fun ringScreen(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun serviceAction(context: Context, action: String): PendingIntent = PendingIntent.getService(
        context, action.hashCode(),
        Intent(context, AlarmService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
