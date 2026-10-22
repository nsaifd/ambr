package app.ambr.ui.you

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.domain.Format
import app.ambr.platform.Settings
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.TopBar
import app.ambr.ui.theme.AmbrColors
import java.time.LocalDate

@Composable
fun RemindersScreen(settings: Settings, onBack: () -> Unit) {
    val first by settings.firstReminderDays.collectAsStateWithLifecycle()
    val second by settings.secondReminder.collectAsStateWithLifecycle()
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS) }
    val example = LocalDate.now().plusMonths(5).withDayOfMonth(14)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TopBar("Reminders", onBack)
        Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text("Remind me before a warranty ends", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                Text("Enough time to book a free check-up", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AmbrColors.Ground).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(7, 14, 30, 60).forEach { d ->
                    val on = d == first
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) AmbrColors.Card else AmbrColors.Ground)
                            .selectable(on, role = Role.RadioButton) { settings.setFirstReminderDays(d) },
                        contentAlignment = Alignment.Center,
                    ) { Text("$d days", style = MaterialTheme.typography.labelMedium, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, color = if (on) AmbrColors.TextPrimary else AmbrColors.TextMuted) }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AmbrColors.HoneyTint).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_calendar), null, tint = AmbrColors.HoneyInk, modifier = Modifier.padding(top = 2.dp).size(16.dp))
                Text("If a warranty ends ${Format.date(example)}, you would hear from us on ${Format.date(example.minusDays(first.toLong()))}.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextPrimary)
            }
        }
        Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Second reminder, 7 days before", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                    Text("Only if you didn't tap All good", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                }
                Switch(second, settings::setSecondReminder, colors = SwitchDefaults.colors(checkedTrackColor = AmbrColors.CoveredFill, checkedThumbColor = AmbrColors.Card))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(AmbrColors.Divider))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_moon), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(17.dp))
                Column(Modifier.weight(1f)) {
                    Text("Quiet hours", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                    Text("No notes at night", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                }
                Text("10 pm – 8 am", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted)
            }
        }
        Row(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Navy).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_bell), null, tint = AmbrColors.HoneyOnNavy, modifier = Modifier.padding(top = 2.dp).size(17.dp))
            Column {
                Text("Our promise", style = MaterialTheme.typography.titleMedium, color = AmbrColors.Card)
                Text("At most 2 notes per warranty. Nothing on the end date. Never ads or daily nudges.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.OnDarkMuted)
            }
        }
    }
}
