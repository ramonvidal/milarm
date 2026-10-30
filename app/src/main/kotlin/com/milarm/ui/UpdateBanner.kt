package com.milarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milarm.core.AppUpdate
import com.milarm.update.UpdateState
import com.milarm.update.UpdateViewModel

/** "Milarm 0.25 is available – Install". Takes no room when there's nothing to offer. */
@Composable
fun UpdateBanner(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when (val current = state) {
        UpdateState.None -> Unit
        is UpdateState.Available -> BannerCard {
            MessageRow("Milarm ${current.update.versionName} is available") {
                TextButton(onClick = { viewModel.later(current.update) }) { Text("Later") }
                Button(onClick = { viewModel.install(current.update) }) { Text("Update") }
            }
        }
        is UpdateState.Downloading -> BannerCard {
            Text("Downloading…", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { current.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 4.dp),
            )
        }
        is UpdateState.NeedsPermission -> BannerCard {
            MessageRow("To update, allow Milarm to install apps (only needed once).") {
                Button(onClick = { context.startActivity(viewModel.allowInstallsIntent()) }) { Text("Allow") }
            }
        }
        is UpdateState.Failed -> BannerCard {
            MessageRow("The update couldn't be downloaded. Check your connection.") {
                TextButton(onClick = { viewModel.install(current.update) }) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun BannerCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun MessageRow(text: String, actions: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        actions()
    }
}
