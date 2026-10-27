package com.udit.catchup.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.udit.catchup.data.Person
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// --- Small helpers ---

fun dial(context: Context, number: String?) {
    if (number.isNullOrBlank()) return
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
}

fun openInstagram(context: Context, username: String?) {
    if (username.isNullOrBlank()) return
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$username")))
}

fun relativeTime(ts: Long): String =
    DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        .toString()

fun formatTime(hour: Int, minute: Int): String =
    LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("h:mm a"))

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .take(2).map { it.first().uppercaseChar() }.joinToString("")
        .ifEmpty { "?" }

// --- Shared composables ---

// Soft earthy pairs (background to text); each person keeps a stable color via name hash.
private val AvatarColors = listOf(
    Color(0xFFD7E8CD) to Color(0xFF121F0E),
    Color(0xFFEFE3C2) to Color(0xFF3B2F05),
    Color(0xFFF4DCCD) to Color(0xFF33190A),
    Color(0xFFBCEBEF) to Color(0xFF002022),
    Color(0xFFC9E6FF) to Color(0xFF001E31),
    Color(0xFFE1E0F0) to Color(0xFF191A2C),
)

@Composable
fun Avatar(person: Person, size: Dp = 48.dp) {
    if (person.photoUri != null) {
        AsyncImage(
            model = person.photoUri,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        val (bg, fg) = AvatarColors[abs(person.displayName.hashCode()) % AvatarColors.size]
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials(person.displayName),
                style = MaterialTheme.typography.titleMedium,
                color = fg,
            )
        }
    }
}

@Composable
fun StatTile(
    value: String,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = content,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
fun LabeledProgress(done: Int, total: Int, caption: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * The shared "fill out this person" form, used by both the triage flow and the
 * person detail screen. Field state resets whenever a different person comes in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonEditor(
    person: Person,
    saveLabel: String,
    onSave: (how: String, relationship: String, notes: String, canHangout: Boolean, canCall: Boolean, isTight: Boolean) -> Unit,
    secondaryActions: (@Composable RowScope.() -> Unit)? = null,
) {
    var how by remember(person.id) { mutableStateOf(person.howWeKnow) }
    var relationship by remember(person.id) { mutableStateOf(person.relationship) }
    var notes by remember(person.id) { mutableStateOf(person.notes) }
    var canHangout by remember(person.id) { mutableStateOf(person.canHangout) }
    var canCall by remember(person.id) { mutableStateOf(person.canCall) }
    var isTight by remember(person.id) { mutableStateOf(person.isTight) }

    Card(shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(person, 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(person.displayName, style = MaterialTheme.typography.titleLarge)
                    if (!person.phoneNumber.isNullOrBlank()) {
                        Text(
                            person.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (person.instagramUsername != null) {
                        Text(
                            "@${person.instagramUsername} on Instagram",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = how,
                onValueChange = { how = it },
                label = { Text("How do you know them?") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "Relationship",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Person.RELATIONSHIPS.forEach { r ->
                    FilterChip(
                        selected = relationship == r,
                        onClick = { relationship = if (relationship == r) "" else r },
                        label = { Text(r) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            SwitchRow("Can I hang out with them?", null, canHangout) { canHangout = it }
            SwitchRow(
                "Can I call them?",
                "They'll go into your call rotation",
                canCall,
            ) { canCall = it }
            SwitchRow(
                "Are we tight?",
                "Your inner circle",
                isTight,
            ) { isTight = it }

            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                secondaryActions?.invoke(this)
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    onSave(how.trim(), relationship, notes.trim(), canHangout, canCall, isTight)
                }) {
                    Text(saveLabel)
                }
            }
        }
    }
}
