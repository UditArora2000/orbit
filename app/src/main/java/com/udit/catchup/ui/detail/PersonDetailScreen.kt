package com.udit.catchup.ui.detail

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.udit.catchup.ui.components.PersonEditor
import com.udit.catchup.ui.components.dial
import com.udit.catchup.ui.components.openInstagram
import com.udit.catchup.ui.components.relativeTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as CatchUpApp).database.personDao()
    private val idFlow = MutableStateFlow<Long?>(null)

    val person = idFlow.filterNotNull()
        .flatMapLatest { dao.observePerson(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun load(id: Long) {
        idFlow.value = id
    }

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

    fun markCaughtUp(person: Person) = viewModelScope.launch {
        dao.insertLog(CatchUpLog(personId = person.id))
        dao.markContacted(person.id, System.currentTimeMillis())
    }

    fun archive(person: Person) = viewModelScope.launch {
        dao.update(person.copy(status = Person.STATUS_ARCHIVED, updatedAt = System.currentTimeMillis()))
    }
}

@Composable
fun PersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    vm: PersonDetailViewModel = viewModel(),
) {
    LaunchedEffect(personId) { vm.load(personId) }
    val person by vm.person.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val p = person ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            if (!p.phoneNumber.isNullOrBlank()) {
                Button(onClick = { dial(context, p.phoneNumber) }) {
                    Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call")
                }
            } else if (p.instagramUsername != null) {
                Button(onClick = { openInstagram(context, p.instagramUsername) }) {
                    Text("Instagram")
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        PersonEditor(
            person = p,
            saveLabel = "Save",
            onSave = { how, rel, notes, hang, call ->
                vm.save(p, how, rel, notes, hang, call)
                onBack()
            },
            secondaryActions = {
                OutlinedButton(onClick = { vm.markCaughtUp(p) }) { Text("Caught up ✓") }
            },
        )

        Spacer(Modifier.height(12.dp))
        Text(
            p.lastContactedAt?.let { "Last caught up ${relativeTime(it)}" } ?: "No catch-ups logged yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            vm.archive(p)
            onBack()
        }) {
            Text("Archive this person")
        }
    }
}
