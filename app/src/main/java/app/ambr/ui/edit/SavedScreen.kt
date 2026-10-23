package app.ambr.ui.edit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.domain.Format
import app.ambr.domain.StatusKind
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.IconTile
import app.ambr.ui.components.visual
import app.ambr.ui.state.ItemViewModel
import app.ambr.ui.theme.AmbrColors

@Composable
fun SavedScreen(vm: ItemViewModel, firstReminderDays: Int, onAddAnother: () -> Unit, onDone: () -> Unit) {
    val ui by vm.item.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val pop = remember { Animatable(0.6f) }
    // Ask for notifications right after the first save, when the reason is obvious.
    val notify = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) notify.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        pop.animateTo(1f, spring(dampingRatio = 0.45f))
    }
    val current = ui ?: return
    val i = current.item
    val v = i.category.visual(i.name)
    val s = current.status
    Column(
        Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.scale(pop.value).size(84.dp).clip(CircleShape).background(AmbrColors.CoveredFill), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check), null, tint = AmbrColors.Card, modifier = Modifier.size(30.dp))
            }
            Text(if (i.name.length <= 22) "${i.name} saved" else "Item saved", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary)
            val until = s.until
            if ((s.kind == StatusKind.Covered || s.kind == StatusKind.EndsSoon) && until != null) {
                Text(buildAnnotatedString {
                    append("Covered until ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(Format.date(until)) }
                }, style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextPrimary)
                val remind = until.minusDays(firstReminderDays.toLong())
                if (remind.isAfter(java.time.LocalDate.now())) Text("We'll remind you on ${Format.date(remind)}.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted, textAlign = TextAlign.Center)
            } else {
                Text(app.ambr.domain.Warranty.headline(s), style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(v.icon, v.tone.tile, v.tone.ink)
            Column(Modifier.weight(1f)) {
                Text(i.name, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                val main = current.details.coverages.filter { it.months > 0 }.maxOfOrNull { it.months }
                Text(listOfNotNull(i.room.label, i.priceTaka?.let(Format::taka), main?.let(Format::warrantyLength)).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
            }
        }
        Spacer(Modifier.weight(1f))
        AmbrButton("Add another", onAddAnother, style = ButtonStyle.Quiet, icon = R.drawable.ic_camera)
        AmbrButton("Done", onDone, style = ButtonStyle.Strong)
    }
}
