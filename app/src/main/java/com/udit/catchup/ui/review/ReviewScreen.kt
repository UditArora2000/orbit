package com.udit.catchup.ui.review

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udit.catchup.CatchUpApp
import com.udit.catchup.data.Person
import com.udit.catchup.ui.components.Avatar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as CatchUpApp).database.personDao()

    val skipped = dao.observeSkipped()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Person>())
    val archived = dao.observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Person>())

    fun restore(person: Person) = viewModelScope.launch {
        dao.restore(person.id, System.currentTimeMillis())
    }

    fun fileNext(person: Person) = viewModelScope.launch {
        dao.bringToFront(person.id, System.currentTimeMillis())
    }
}

@Composable
fun ReviewScreen(
    mode: String,
    onOpenPerson: (Long) -> Unit,
    onBack: () -> Unit,
    vm: ReviewViewModel = viewModel(),
) {
    val isSkipped = mode == "skipped"
    val skipped by vm.skipped.collectAsStateWithLifecycle()
    val archived by vm.archived.collectAsStateWithLifecycle()
    val people = if (isSkipped) skipped else archived

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (isSkipped) "Skipped" else "Archived",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Text(
            if (isSkipped) {
                "Skipped people are still in your filing queue — they come back around after everyone else. Tap one to fill them out now, or move them to the front."
            } else {
                "Archived people are hidden from filing and your friends list (meant for businesses and numbers that aren't people). Restore someone to put them back in the queue."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        if (people.isEmpty()) {
            Text(
                if (isSkipped) "Nothing skipped right now." else "Nothing archived right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(people, key = { it.id }) { person ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPerson(person.id) },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(person)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.displayName, style = MaterialTheme.typography.titleMedium)
                            if (isSkipped) {
                                Text(
                                    if (person.skipCount == 1) "Skipped once"
                                    else "Skipped ${person.skipCount} times",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else if (!person.phoneNumber.isNullOrBlank()) {
                                Text(
                                    person.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (isSkipped) {
                            OutlinedButton(onClick = {
                                vm.fileNext(person)
                                onBack()
                            }) {
                                Text("File next")
                            }
                        } else {
                            OutlinedButton(onClick = { vm.restore(person) }) {
                                Text("Restore")
                            }
                        }
                    }
                }
            }
        }
    }
}
