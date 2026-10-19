package com.udit.catchup.ui.triage

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udit.catchup.CatchUpApp
import com.udit.catchup.data.Person
import com.udit.catchup.ui.components.LabeledProgress
import com.udit.catchup.ui.components.PersonEditor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TriageViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as CatchUpApp).database.personDao()

    val next = dao.observeNextUntriaged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val untriagedCount = dao.observeUntriagedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val activeCount = dao.observeActiveCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val skippedCount = dao.observeSkippedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val archivedCount = dao.observeArchivedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun save(
        person: Person,
        how: String,
        relationship: String,
        notes: String,
        canHangout: Boolean,
        canCall: Boolean,
    ) = viewModelScope.launch {
        dao.update(
            person.copy(
                howWeKnow = how,
                relationship = relationship,
                notes = notes,
                canHangout = canHangout,
                canCall = canCall,
                status = Person.STATUS_TRIAGED,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    fun skip(person: Person) = viewModelScope.launch {
        dao.skip(person.id, System.currentTimeMillis())
    }

    fun archive(person: Person) = viewModelScope.launch {
        dao.update(person.copy(status = Person.STATUS_ARCHIVED, updatedAt = System.currentTimeMillis()))
    }
}

@Composable
fun TriageScreen(
    onOpenSkipped: () -> Unit,
    onOpenArchived: () -> Unit,
    vm: TriageViewModel = viewModel(),
) {
    val next by vm.next.collectAsStateWithLifecycle()
    val untriaged by vm.untriagedCount.collectAsStateWithLifecycle()
    val active by vm.activeCount.collectAsStateWithLifecycle()
    val skippedCount by vm.skippedCount.collectAsStateWithLifecycle()
    val archivedCount by vm.archivedCount.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("File your people", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "One at a time: say how you know them and whether they're call or hangout material.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        val filed = active - untriaged
        LabeledProgress(filed, active, "$filed of $active filed")
        if (skippedCount > 0 || archivedCount > 0) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (skippedCount > 0) {
                    OutlinedButton(onClick = onOpenSkipped) { Text("Skipped ($skippedCount)") }
                }
                if (archivedCount > 0) {
                    OutlinedButton(onClick = onOpenArchived) { Text("Archived ($archivedCount)") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        val person = next
        if (person == null) {
            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("All caught up", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "When you save a new contact, it'll show up here (and you'll get a notification) so you can file them too.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            PersonEditor(
                person = person,
                saveLabel = "Done — next",
                onSave = { how, rel, notes, hang, call ->
                    vm.save(person, how, rel, notes, hang, call)
                },
                secondaryActions = {
                    OutlinedButton(onClick = { vm.skip(person) }) { Text("Skip") }
                    TextButton(onClick = { vm.archive(person) }) { Text("Archive") }
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Skip sends them to the back of the queue. Archive is for businesses, spam, and numbers that aren't really people.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
