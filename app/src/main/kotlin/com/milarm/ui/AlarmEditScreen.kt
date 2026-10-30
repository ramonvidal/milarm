package com.milarm.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.milarm.core.Alarm
import com.milarm.core.SoundMode
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(
    initial: Alarm,
    isNew: Boolean,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)
    val context = LocalContext.current
    val time = rememberTimePickerState(initial.hour, initial.minute, DateFormat.is24HourFormat(context))
    var days by remember { mutableStateOf(initial.days) }
    var label by remember { mutableStateOf(initial.label) }
    var soundMode by remember { mutableStateOf(initial.soundMode) }
    var snooze by remember { mutableStateOf(initial.snoozeMinutes) }
    var wakeCheck by remember { mutableStateOf(initial.wakeCheck) }

    Scaffold(topBar = { TopAppBar(title = { Text(if (isNew) "New alarm" else "Edit alarm") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TimePicker(state = time, modifier = Modifier.align(Alignment.CenterHorizontally))

            Section("Repeat")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in days,
                        onClick = { days = if (day in days) days - day else days + day },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                    )
                }
            }
            Text(daysText(days), style = MaterialTheme.typography.bodySmall)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Sound")
            SoundOption("Ringtone + vibrate", SoundMode.RINGTONE_AND_VIBRATE, soundMode) { soundMode = it }
            SoundOption("Vibrate only", SoundMode.VIBRATE_ONLY, soundMode) { soundMode = it }
            SoundOption("Band only (phone stays silent)", SoundMode.BAND_ONLY, soundMode) { soundMode = it }

            Section("Snooze: $snooze min")
            Slider(
                value = snooze.toFloat(),
                onValueChange = { snooze = it.roundToInt() },
                valueRange = 1f..30f,
                steps = 28,
            )

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Wake-up check", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "After you dismiss, the alarm rings again if your phone sees no movement.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = wakeCheck.enabled, onCheckedChange = { wakeCheck = wakeCheck.copy(enabled = it) })
            }
            if (wakeCheck.enabled) {
                Text("Check after ${wakeCheck.checkAfterMinutes} min")
                Slider(
                    value = wakeCheck.checkAfterMinutes.toFloat(),
                    onValueChange = { wakeCheck = wakeCheck.copy(checkAfterMinutes = it.roundToInt()) },
                    valueRange = 1f..30f,
                    steps = 28,
                )
                Text("Up to ${wakeCheck.maxRounds} checks")
                Slider(
                    value = wakeCheck.maxRounds.toFloat(),
                    onValueChange = { wakeCheck = wakeCheck.copy(maxRounds = it.roundToInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) TextButton(onClick = onDelete) { Text("Delete") }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = {
                    onSave(
                        initial.copy(
                            hour = time.hour,
                            minute = time.minute,
                            days = days,
                            label = label.trim(),
                            soundMode = soundMode,
                            snoozeMinutes = snooze,
                            wakeCheck = wakeCheck,
                        ),
                    )
                }) { Text("Save") }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SoundOption(text: String, mode: SoundMode, selected: SoundMode, onSelect: (SoundMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = mode == selected, onClick = { onSelect(mode) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = mode == selected, onClick = { onSelect(mode) })
        Text(text)
    }
}
