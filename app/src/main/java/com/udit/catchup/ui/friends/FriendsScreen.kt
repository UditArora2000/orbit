package com.udit.catchup.ui.friends

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udit.catchup.CatchUpApp
import com.udit.catchup.data.CatchUpLog
import com.udit.catchup.data.Person
import com.udit.catchup.ui.components.Avatar
import com.udit.catchup.ui.components.dial
import com.udit.catchup.ui.components.openInstagram
import com.udit.catchup.ui.components.relativeTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as CatchUpApp).database.personDao()

    val search = MutableStateFlow("")
    val filter = MutableStateFlow("ALL")

    val friends = combine(search, filter) { s, f -> s to f }
        .flatMapLatest { (s, f) -> dao.observeFriends(f, s.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Person>())

    fun markCaughtUp(person: Person) = viewModelScope.launch {
        dao.insertLog(CatchUpLog(personId = person.id))
        dao.markContacted(person.id, System.currentTimeMillis())
    }
}

@Composable
fun FriendsScreen(
    onOpenPerson: (Long) -> Unit,
    vm: FriendsViewModel = viewModel(),
) {
    val friends by vm.friends.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Friends", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { vm.search.value = it },
            placeholder = { Text("Search your people…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CircleShape,
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == "ALL",
                onClick = { vm.filter.value = "ALL" },
                label = { Text("All") },
            )
            FilterChip(
                selected = filter == "CALL",
                onClick = { vm.filter.value = "CALL" },
                label = { Text("Can call") },
            )
            FilterChip(
                selected = filter == "HANGOUT",
                onClick = { vm.filter.value = "HANGOUT" },
                label = { Text("Can hang out") },
            )
        }
        Spacer(Modifier.height(8.dp))

        if (friends.isEmpty()) {
            Text(
                "No one here yet — file people first, then they show up in this list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(friends, key = { it.id }) { person ->
                FriendRow(
                    person = person,
                    onClick = { onOpenPerson(person.id) },
                    onCall = {
                        if (!person.phoneNumber.isNullOrBlank()) dial(context, person.phoneNumber)
                        else openInstagram(context, person.instagramUsername)
                    },
                    onCaughtUp = { vm.markCaughtUp(person) },
                )
            }
        }
    }
}

@Composable
private fun FriendRow(
    person: Person,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onCaughtUp: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(person)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(person.displayName, style = MaterialTheme.typography.titleMedium)
                    if (person.contactedThisCycle) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Caught up this round",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                val subtitle = listOf(person.relationship, person.howWeKnow)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    person.lastContactedAt?.let { "Last caught up ${relativeTime(it)}" } ?: "No catch-ups yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!person.phoneNumber.isNullOrBlank()) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Outlined.Call, contentDescription = "Call ${person.displayName}")
                }
            } else if (person.instagramUsername != null) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Open Instagram profile")
                }
            }
            FilledTonalIconButton(onClick = onCaughtUp) {
                Icon(Icons.Outlined.TaskAlt, contentDescription = "Mark caught up")
            }
        }
    }
}
