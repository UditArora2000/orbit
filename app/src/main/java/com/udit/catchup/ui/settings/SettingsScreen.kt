package com.udit.catchup.ui.settings

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udit.catchup.CatchUpApp
import com.udit.catchup.data.AppSettings
import com.udit.catchup.data.ContactsSyncer
import com.udit.catchup.data.InstagramImporter
import com.udit.catchup.notify.NudgeScheduler
import com.udit.catchup.ui.components.SwitchRow
import com.udit.catchup.ui.components.formatTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val catchUpApp = app as CatchUpApp
    private val repo = catchUpApp.settings

    val settings = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    var message by mutableStateOf<String?>(null)
        private set

    fun setNudgeEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.setNudgeEnabled(enabled)
        NudgeScheduler.scheduleNext(catchUpApp)
    }

    fun setNudgeTime(hour: Int, minute: Int) = viewModelScope.launch {
        repo.setNudgeTime(hour, minute)
        NudgeScheduler.scheduleNext(catchUpApp)
        message = "Daily nudge set for ${formatTime(hour, minute)}"
    }

    fun syncNow() = viewModelScope.launch {
        if (!ContactsSyncer.hasPermission(catchUpApp)) {
            message = "Contacts permission not granted — allow it from the Home tab first"
            return@launch
        }
        val result = ContactsSyncer.sync(catchUpApp)
        message = "Sync done: ${result.newCount} new, ${result.updatedCount} updated"
    }

    fun importInstagram(uri: Uri) = viewModelScope.launch {
        message = "Importing…"
        message = try {
            val r = InstagramImporter.import(catchUpApp, uri)
            if (r.linked == 0 && r.created == 0 && r.skipped == 0) {
                "Nothing imported — is that the following.json file from your Instagram export?"
            } else {
                "Instagram: linked ${r.linked} to contacts, added ${r.created} new, skipped ${r.skipped} already known"
            }
        } catch (e: Exception) {
            "Import failed — pick the following.json file from your Instagram data export"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    val instagramPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importInstagram(uri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        vm.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Daily nudge", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Once a day, Orbit picks a friend from your call rotation and reminds you to reach out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow("Nudge me daily", null, settings.nudgeEnabled) { vm.setNudgeEnabled(it) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = settings.nudgeEnabled) { showTimePicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Nudge time", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        formatTime(settings.nudgeHour, settings.nudgeMinute),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Contacts", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Contacts re-sync automatically every few hours. New people get queued for filing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { vm.syncNow() }) { Text("Sync now") }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Instagram", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Instagram doesn't let apps read your follow list directly, but you can import it from " +
                        "your data export:\n\n1. Instagram → profile → menu → Accounts Center\n" +
                        "2. Your information and permissions → Download your information\n" +
                        "3. Request a JSON export of \"Followers and following\"\n" +
                        "4. Unzip it and pick the following.json file below\n\n" +
                        "Accounts that match a contact's name get linked; the rest are added as new people to file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { instagramPicker.launch(arrayOf("*/*")) }) {
                    Text("Import following.json")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Orbit keeps everything on your phone — nothing is uploaded anywhere.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = settings.nudgeHour,
            initialMinute = settings.nudgeMinute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setNudgeTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) },
        )
    }
}
