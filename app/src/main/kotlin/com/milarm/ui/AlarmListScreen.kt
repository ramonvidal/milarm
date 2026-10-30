package com.milarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.milarm.alarm.Notifications
import com.milarm.core.Alarm
import com.milarm.core.SoundMode
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    updateBanner: @Composable () -> Unit,
    version: String,
    onCheckUpdates: () -> Unit,
    missingPermissions: List<MissingPermission>,
    onGrant: (MissingPermission) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Milarm") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Check for updates") },
                            onClick = {
                                menuOpen = false
                                onCheckUpdates()
                            },
                        )
                        DropdownMenuItem(text = { Text("Version $version") }, onClick = {}, enabled = false)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add alarm") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "update") { updateBanner() }
            items(missingPermissions, key = { it.title }) { PermissionCard(it, onGrant) }
            if (alarms.isEmpty()) {
                item { Text("No alarms yet. Tap + to add one.", style = MaterialTheme.typography.bodyLarge) }
            }
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(alarm, onClick = { onEdit(alarm) }, onToggle = { onToggle(alarm, it) })
            }
        }
    }
}

@Composable
private fun PermissionCard(permission: MissingPermission, onGrant: (MissingPermission) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(permission.title, style = MaterialTheme.typography.titleMedium)
            Text(permission.reason, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onGrant(permission) }) { Text("Allow") }
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Notifications.timeText(alarm), fontSize = 40.sp)
                Text(summary(alarm), style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun summary(alarm: Alarm): String = listOfNotNull(
    alarm.label.takeIf { it.isNotBlank() },
    daysText(alarm.days),
    when (alarm.soundMode) {
        SoundMode.RINGTONE_AND_VIBRATE -> null
        SoundMode.VIBRATE_ONLY -> "Vibrate only"
        SoundMode.BAND_ONLY -> "Band only"
    },
    if (alarm.wakeCheck.enabled) "Wake-up check ${alarm.wakeCheck.checkAfterMinutes} min" else null,
).joinToString(" · ")

fun daysText(days: Set<DayOfWeek>): String = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Every day"
    days == WEEKDAYS -> "Weekdays"
    days == WEEKEND -> "Weekends"
    else -> DayOfWeek.entries.filter { it in days }
        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
private val WEEKDAYS = DayOfWeek.entries.toSet() - WEEKEND
