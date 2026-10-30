package com.milarm.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milarm.alarm.AlarmService
import com.milarm.alarm.AlarmSession
import com.milarm.alarm.Notifications
import com.milarm.core.AlarmPhase

/** Full-screen alarm shown over the lock screen while ringing or asking "Are you up?". */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MilarmTheme {
                val state by AlarmSession.state.collectAsStateWithLifecycle()
                val current = state
                // During the wake-up check only movement counts, so the screen gets out of the way.
                val visible = current != null && current.phase !is AlarmPhase.WakeCheck
                LaunchedEffect(visible) { if (!visible) finish() }
                if (current != null && visible) {
                    RingScreen(current, send = { AlarmService.send(this, it) })
                }
            }
        }
    }
}

@Composable
private fun RingScreen(state: AlarmSession.State, send: (String) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.phase) {
                is AlarmPhase.Ringing -> {
                    Heading(Notifications.timeText(state.alarm), Notifications.title(state.alarm))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { send(AlarmService.ACTION_SNOOZE) }, Modifier.height(64.dp).weight(1f)) {
                            Text("Snooze ${state.alarm.snoozeMinutes} min")
                        }
                        Button(onClick = { send(AlarmService.ACTION_DISMISS) }, Modifier.height(64.dp).weight(1f)) {
                            Text("Dismiss")
                        }
                    }
                }
                is AlarmPhase.Asking -> {
                    Heading("Are you up?", "Tap within ${state.alarm.wakeCheck.responseSeconds} s or the alarm rings again.")
                    ImUpButton(send)
                }
                is AlarmPhase.WakeCheck, AlarmPhase.Done -> Unit
            }
        }
    }
}

@Composable
private fun Heading(big: String, small: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, fontSize = 56.sp, textAlign = TextAlign.Center)
        Text(small, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ImUpButton(send: (String) -> Unit) {
    Button(onClick = { send(AlarmService.ACTION_IM_UP) }, Modifier.fillMaxWidth().height(80.dp)) {
        Text("I'm up", fontSize = 24.sp)
    }
}
