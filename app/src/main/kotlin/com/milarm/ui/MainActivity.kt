package com.milarm.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milarm.core.Alarm
import com.milarm.milarm
import java.time.Duration
import java.time.ZonedDateTime

class MainActivity : ComponentActivity() {
    /** Bumped on resume so permission state is re-read after returning from settings. */
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = milarm
        setContent {
            MilarmTheme {
                val alarms by app.repository.alarms.collectAsStateWithLifecycle()
                var editing by remember { mutableStateOf<Alarm?>(null) }
                var permissionTick by remember { mutableIntStateOf(0) }
                val context = LocalContext.current
                val missing = remember(alarms, resumeTick.intValue, permissionTick) {
                    Permissions.missing(context, alarms.any { it.wakeCheck.enabled })
                }
                val requestPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { permissionTick++ }

                val current = editing
                if (current == null) {
                    AlarmListScreen(
                        alarms = alarms,
                        missingPermissions = missing,
                        onGrant = { permission ->
                            when (permission) {
                                is MissingPermission.Runtime -> requestPermission.launch(permission.permission)
                                is MissingPermission.SystemSettings -> startActivity(permission.intent)
                            }
                        },
                        onAdd = { editing = Alarm(id = app.repository.nextId(), hour = 7, minute = 0) },
                        onEdit = { editing = it },
                        onToggle = { alarm, enabled -> save(alarm.copy(enabled = enabled)) },
                    )
                } else {
                    AlarmEditScreen(
                        initial = current,
                        isNew = app.repository.get(current.id) == null,
                        onSave = {
                            save(it.copy(enabled = true))
                            editing = null
                        },
                        onDelete = {
                            app.scheduler.cancel(current.id)
                            app.repository.delete(current.id)
                            editing = null
                        },
                        onCancel = { editing = null },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }

    private fun save(alarm: Alarm) {
        milarm.repository.upsert(alarm)
        val at = milarm.scheduler.schedule(alarm) ?: return
        Toast.makeText(this, "Alarm set for ${untilText(at)} from now", Toast.LENGTH_SHORT).show()
    }

    private fun untilText(at: ZonedDateTime): String {
        val minutes = Duration.between(ZonedDateTime.now(), at).toMinutes() + 1
        val hours = minutes / 60
        return if (hours > 0) "$hours h ${minutes % 60} min" else "$minutes min"
    }
}
