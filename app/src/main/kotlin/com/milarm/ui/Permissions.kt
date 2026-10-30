package com.milarm.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.milarm.milarm

/** A permission the app needs, with how to ask for it. */
sealed class MissingPermission(val title: String, val reason: String) {
    /** Granted through the system permission dialog. */
    class Runtime(val permission: String, title: String, reason: String) : MissingPermission(title, reason)

    /** Granted on a system settings screen. */
    class SystemSettings(val intent: Intent, title: String, reason: String) : MissingPermission(title, reason)
}

object Permissions {
    fun missing(context: Context, wakeCheckUsed: Boolean): List<MissingPermission> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !granted(context, Manifest.permission.POST_NOTIFICATIONS)) {
            add(
                MissingPermission.Runtime(
                    Manifest.permission.POST_NOTIFICATIONS,
                    "Notifications",
                    "Needed to show the alarm and to send alerts to your band.",
                ),
            )
        }
        if (!context.milarm.scheduler.canScheduleExact() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                MissingPermission.SystemSettings(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context)),
                    "Exact alarms",
                    "Needed so alarms ring exactly on time.",
                ),
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        ) {
            add(
                MissingPermission.SystemSettings(
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context)),
                    "Full-screen alarm",
                    "Needed to show the alarm over the lock screen.",
                ),
            )
        }
        if (wakeCheckUsed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !granted(context, Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            add(
                MissingPermission.Runtime(
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    "Physical activity",
                    "Lets the wake-up check count your steps.",
                ),
            )
        }
    }

    private fun granted(context: Context, permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun packageUri(context: Context) = Uri.parse("package:${context.packageName}")
}
