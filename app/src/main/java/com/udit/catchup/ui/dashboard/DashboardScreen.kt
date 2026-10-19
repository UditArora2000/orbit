package com.udit.catchup.ui.dashboard

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udit.catchup.CatchUpApp
import com.udit.catchup.data.CatchUpLog
import com.udit.catchup.data.ContactsSyncer
import com.udit.catchup.data.LogWithName
import com.udit.catchup.data.Person
import com.udit.catchup.ui.components.Avatar
import com.udit.catchup.ui.components.LabeledProgress
import com.udit.catchup.ui.components.StatTile
import com.udit.catchup.ui.components.dial
import com.udit.catchup.ui.components.openInstagram
import com.udit.catchup.ui.components.relativeTime
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val catchUpApp = app as CatchUpApp
    private val dao = catchUpApp.database.personDao()
    private val settings = catchUpApp.settings

    private fun <T> Flow<T>.state(initial: T) =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val activeCount = dao.observeActiveCount().state(0)
    val untriagedCount = dao.observeUntriagedCount().state(0)
    val callableCount = dao.observeCallableCount().state(0)
    val hangoutCount = dao.observeHangoutCount().state(0)
    val cycleDoneCount = dao.observeCycleDoneCount().state(0)
    val totalCatchUps = dao.observeTotalCatchUps().state(0)
    val nextUp = dao.observeNextUp().state(null)
    val recentLogs = dao.observeRecentLogs(8).state(emptyList<LogWithName>())
    val cycleNumber = settings.settings.map { it.cycleNumber }.state(1)

    var syncMessage by mutableStateOf<String?>(null)
        private set

    fun syncNow() = viewModelScope.launch {
        val result = ContactsSyncer.sync(getApplication())
        if (result.newCount > 0) {
            syncMessage = "Found ${result.newCount} new contact${if (result.newCount == 1) "" else "s"} to file"
        }
    }

    fun markCaughtUp(person: Person) = viewModelScope.launch {
        dao.insertLog(CatchUpLog(personId = person.id))
        dao.markContacted(person.id, System.currentTimeMillis())
    }

    fun startNextRound() = viewModelScope.launch {
        dao.resetCycle()
        settings.incrementCycle()
    }
}

private fun greeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..10 -> "Good morning"
        in 11..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Up late?"
    }
}

@Composable
fun DashboardScreen(
    onGoTriage: () -> Unit,
    onGoFriends: () -> Unit,
    vm: DashboardViewModel = viewModel(),
) {
    val context = LocalContext.current
    var hasContactsPerm by remember { mutableStateOf(ContactsSyncer.hasPermission(context)) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasContactsPerm = ContactsSyncer.hasPermission(context)
    }

    LaunchedEffect(hasContactsPerm) {
        if (hasContactsPerm) vm.syncNow()
    }

    val active by vm.activeCount.collectAsStateWithLifecycle()
    val untriaged by vm.untriagedCount.collectAsStateWithLifecycle()
    val callable by vm.callableCount.collectAsStateWithLifecycle()
    val hangout by vm.hangoutCount.collectAsStateWithLifecycle()
    val cycleDone by vm.cycleDoneCount.collectAsStateWithLifecycle()
    val totalCatchUps by vm.totalCatchUps.collectAsStateWithLifecycle()
    val nextUp by vm.nextUp.collectAsStateWithLifecycle()
    val logs by vm.recentLogs.collectAsStateWithLifecycle()
    val cycleNumber by vm.cycleNumber.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    "ORBIT",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(greeting(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Who are we catching up with today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                vm.syncMessage?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (!hasContactsPerm) {
            item {
                Card(shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Connect your contacts", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Orbit reads your contacts so you can file each person and build your call rotation. Everything stays on this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            val perms = buildList {
                                add(Manifest.permission.READ_CONTACTS)
                                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permLauncher.launch(perms.toTypedArray())
                        }) {
                            Text("Allow access")
                        }
                    }
                }
            }
        } else if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            item {
                Card(shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Enable notifications", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Daily nudges and new-contact alerts need notification access.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        // Call rotation — the hero card.
        item {
            val gradient = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer,
                )
            )
            val onHero = MaterialTheme.colorScheme.onPrimaryContainer
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(gradient)
                    .padding(20.dp)
            ) {
                Text(
                    "Call rotation · Round $cycleNumber",
                    style = MaterialTheme.typography.titleMedium,
                    color = onHero,
                )
                Spacer(Modifier.height(12.dp))
                when {
                    callable == 0 -> {
                        Text(
                            "No one in your rotation yet. While filing, flip on “Can I call them?” to add friends here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onHero.copy(alpha = 0.85f),
                        )
                    }
                    cycleDone >= callable -> {
                        LabeledProgress(cycleDone, callable, "$cycleDone of $callable called this round")
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Round $cycleNumber complete",
                            style = MaterialTheme.typography.titleLarge,
                            color = onHero,
                        )
                        Text(
                            "You called everyone in your orbit. Take a bow, then go again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onHero.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.startNextRound() }) {
                            Text("Start round ${cycleNumber + 1}")
                        }
                    }
                    else -> {
                        LabeledProgress(cycleDone, callable, "$cycleDone of $callable called this round")
                        nextUp?.let { person ->
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(person, 64.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Next up",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = onHero.copy(alpha = 0.7f),
                                    )
                                    Text(
                                        person.displayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = onHero,
                                    )
                                    Text(
                                        person.lastContactedAt?.let { "Last caught up ${relativeTime(it)}" }
                                            ?: "First catch-up since you started tracking",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onHero.copy(alpha = 0.8f),
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!person.phoneNumber.isNullOrBlank()) {
                                    Button(onClick = { dial(context, person.phoneNumber) }) {
                                        Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Call")
                                    }
                                } else if (person.instagramUsername != null) {
                                    Button(onClick = { openInstagram(context, person.instagramUsername) }) {
                                        Text("Open Instagram")
                                    }
                                }
                                OutlinedButton(onClick = { vm.markCaughtUp(person) }) {
                                    Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Caught up")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filing progress
        item {
            Card(shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(20.dp)) {
                    Text("File your people", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val filed = active - untriaged
                    LabeledProgress(filed, active, "$filed of $active filed")
                    if (untriaged > 0) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onGoTriage) {
                            Text("Continue filing ($untriaged left)")
                        }
                    } else if (active > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "All filed — new contacts will be queued up automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Stat tiles
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = "$callable",
                    label = "can call",
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = "$hangout",
                    label = "hangout crew",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = "$totalCatchUps",
                    label = "catch-ups",
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (logs.isNotEmpty()) {
            item {
                Text("Recent catch-ups", style = MaterialTheme.typography.titleMedium)
            }
            items(logs, key = { it.id }) { log ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.TaskAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            log.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            relativeTime(log.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
