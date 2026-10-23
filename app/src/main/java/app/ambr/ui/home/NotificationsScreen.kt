package app.ambr.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.domain.Format
import app.ambr.domain.StatusKind
import app.ambr.platform.Settings
import app.ambr.ui.components.Divided
import app.ambr.ui.components.ItemRow
import app.ambr.ui.components.TopBar
import app.ambr.ui.state.ItemsViewModel
import app.ambr.ui.theme.AmbrColors
import java.time.LocalDate

/** Everything that needs the user's attention, newest first: warranties ending, just ended, backup. */
@Composable
fun NotificationsScreen(vm: ItemsViewModel, settings: Settings, onBack: () -> Unit, onOpen: (Long) -> Unit, onBackup: () -> Unit) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    val backupOn by settings.backupOn.collectAsStateWithLifecycle()
    val backupError by settings.backupError.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val ending = overview.endingSoon
    val ended = overview.items
        .filter { it.status.kind == StatusKind.Expired && it.status.until?.let { u -> !u.isBefore(today.minusDays(60)) } == true }
        .sortedByDescending { it.status.until }
    val backupNote = when {
        backupError == "quota" -> "Your Google Drive is full, so last night's backup didn't finish."
        backupError != null -> "Last night's backup didn't finish. Tap to try again."
        !backupOn && overview.items.isNotEmpty() -> "Your receipts live only on this phone. Turn on backup."
        else -> null
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TopBar("Notifications", onBack) }
        backupNote?.let { note ->
            item {
                Divided(listOf {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().clickable(onClick = onBackup).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(if (backupError != null) AmbrColors.DangerTint else AmbrColors.HoneyTint), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_cloud_upload_alt), null, tint = if (backupError != null) AmbrColors.Danger else AmbrColors.HoneyInk, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Backup", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                            Text(note, style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
                        }
                    }
                })
            }
        }
        if (ending.isNotEmpty()) {
            item { Label("Ending soon") }
            item {
                Divided(ending.map { ui ->
                    {
                        val days = ui.status.daysLeft ?: 0
                        ItemRow(ui, "${if (days == 1L) "1 day" else "$days days"} left · ends ${ui.status.until?.let(Format::date) ?: ""}", { onOpen(ui.id) })
                    }
                })
            }
        }
        if (ended.isNotEmpty()) {
            item { Label("Recently ended") }
            item { Divided(ended.map { ui -> { ItemRow(ui, "Ended ${ui.status.until?.let(Format::date) ?: ""}", { onOpen(ui.id) }) } }) }
        }
        if (backupNote == null && ending.isEmpty() && ended.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(AmbrColors.CoveredTint), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_bell), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(28.dp))
                }
                Text("You're all caught up", style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
                Text("We'll tell you ${settings.firstReminderDays.value} days before a warranty ends.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Label(text: String) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
