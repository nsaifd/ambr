package app.ambr.ui.you

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.BuildConfig
import app.ambr.R
import app.ambr.platform.Settings
import app.ambr.ui.components.CardShape
import app.ambr.ui.state.ItemsViewModel
import app.ambr.ui.theme.AmbrColors
import java.text.DateFormat
import java.util.Date

data class YouRow(val label: String, val icon: Int, val value: String = "", val tag: String? = null, val onClick: () -> Unit)

@Composable
fun YouScreen(
    vm: ItemsViewModel,
    settings: Settings,
    onBackup: () -> Unit,
    onPro: () -> Unit,
    onReminders: () -> Unit,
    onAppLock: () -> Unit,
    onExport: () -> Unit,
    onPrivacy: () -> Unit,
    onArchive: () -> Unit,
    onHelp: () -> Unit,
    onRate: () -> Unit,
    onAbout: () -> Unit,
) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    val archived by vm.archived.collectAsStateWithLifecycle()
    val pro by settings.isPro.collectAsStateWithLifecycle()
    val backupOn by settings.backupOn.collectAsStateWithLifecycle()
    val lastBackup by settings.lastBackup.collectAsStateWithLifecycle()
    val first by settings.firstReminderDays.collectAsStateWithLifecycle()
    val lock by settings.appLock.collectAsStateWithLifecycle()

    val groups = listOf(
        "Settings" to listOf(
            YouRow("Reminders", R.drawable.ic_bell, "$first days before", onClick = onReminders),
            YouRow("App lock", R.drawable.ic_lock, if (lock) "On" else "", tag = if (pro) null else "Pro", onClick = onAppLock),
            YouRow("Household", R.drawable.ic_users, tag = "Soon", onClick = {}),
        ),
        "Your data" to listOf(
            YouRow("Export everything", R.drawable.ic_download, "PDF, CSV", onClick = onExport),
            YouRow("Privacy", R.drawable.ic_shield_check, onClick = onPrivacy),
            YouRow("Archive", R.drawable.ic_archive, "${archived.size}", onClick = onArchive),
        ),
        "Support" to listOf(
            YouRow("Help & feedback", R.drawable.ic_interrogation, onClick = onHelp),
            YouRow("Rate Ambr", R.drawable.ic_star, onClick = onRate),
            YouRow("About Ambr", R.drawable.ic_info, BuildConfig.VERSION_NAME, onClick = onAbout),
        ),
    )

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("You", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary, modifier = Modifier.padding(horizontal = 2.dp)) }
        item {
            Row(
                Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).clickable(onClick = onBackup).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (backupOn) AmbrColors.CoveredTint else AmbrColors.HoneyTint), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_cloud_upload_alt), null, tint = if (backupOn) AmbrColors.CoveredInk else AmbrColors.HoneyInk, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(if (backupOn) "Your data is backed up" else "Back up to Google Drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AmbrColors.TextPrimary)
                    Text(
                        when {
                            !backupOn -> "If you lose this phone, your receipts go with it"
                            lastBackup == 0L -> "Google Drive · waiting for the first backup"
                            else -> "Google Drive · " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastBackup))
                        },
                        style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted,
                    )
                }
                Icon(painterResource(R.drawable.ic_angle_small_right), null, tint = AmbrColors.TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        item {
            val used = overview.items.size
            Column(
                Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Navy).clickable(onClick = onPro).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AmbrColors.NavyRaised), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_crown), null, tint = AmbrColors.HoneyOnNavy, modifier = Modifier.size(18.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (pro) "Ambr Pro" else "Free plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AmbrColors.Card)
                        Text(if (pro) "Unlimited items · thank you" else "$used of ${Settings.FREE_LIMIT} items used", style = MaterialTheme.typography.labelMedium, color = AmbrColors.OnDarkMuted)
                    }
                    if (!pro) Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(AmbrColors.Honey).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text("Get Pro", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AmbrColors.TextPrimary)
                    }
                }
                if (!pro) Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(AmbrColors.NavyRaised)) {
                    Box(Modifier.fillMaxWidth((used.toFloat() / Settings.FREE_LIMIT).coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(AmbrColors.HoneyOnNavy))
                }
            }
        }
        groups.forEach { (title, rows) ->
            item(key = title) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                    Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(vertical = 4.dp)) {
                        rows.forEachIndexed { i, r ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AmbrColors.Divider))
                            Row(
                                Modifier.fillMaxWidth().clickable(enabled = r.tag != "Soon", onClick = r.onClick).padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AmbrColors.Ground), contentAlignment = Alignment.Center) {
                                    Icon(painterResource(r.icon), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(17.dp))
                                }
                                Text(r.label, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
                                r.tag?.let { Tag(it) }
                                if (r.value.isNotEmpty()) Text(r.value, style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted)
                                Icon(painterResource(R.drawable.ic_angle_small_right), null, tint = AmbrColors.TextPlaceholder, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Ambr ${BuildConfig.VERSION_NAME} · Icons: Uicons by Flaticon", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
    }
}

@Composable
private fun Tag(text: String) {
    val pro = text == "Pro"
    Text(
        text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        color = if (pro) AmbrColors.HoneyInk else AmbrColors.TextSecondary,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (pro) AmbrColors.HoneyTint else Color(0xFFECEEF2)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
