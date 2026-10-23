package app.ambr.ui.you

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.BuildConfig
import app.ambr.R
import app.ambr.platform.Export
import app.ambr.platform.Intents
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.TopBar
import app.ambr.ui.state.ItemsViewModel
import app.ambr.ui.theme.AmbrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where feedback goes. Left blank until there is a public support address. */
const val SUPPORT_EMAIL = ""

@Composable
private fun Page(title: String, onBack: () -> Unit, bottom: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TopBar(title, onBack)
            content()
        }
        bottom?.let { Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { it() } }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
        }
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = AmbrColors.CoveredFill, checkedThumbColor = AmbrColors.Card))
    }
}

@Composable
private fun WhiteCard(content: @Composable () -> Unit) = Column(
    Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
) { content() }

@Composable
fun ExportScreen(vm: ItemsViewModel, isPro: Boolean, onGetPro: () -> Unit, onBack: () -> Unit) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    val archived by vm.archived.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdf by remember { mutableStateOf(true) }
    var photos by remember { mutableStateOf(true) }
    var withArchived by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val items = overview.items + if (withArchived) archived else emptyList()

    Page("Export everything", onBack, bottom = {
        if (isPro) AmbrButton(if (busy) "Preparing…" else "Export ${items.size} items as ${if (pdf) "PDF" else "CSV"}", {
            busy = true
            scope.launch {
                val file = withContext(Dispatchers.IO) { if (pdf) Export.pdf(context, items, photos) else Export.csv(context, items) }
                busy = false
                Intents.shareFiles(context, listOf(file), "My Ambr export", mime = if (pdf) "application/pdf" else "text/csv")
            }
        }, style = ButtonStyle.Strong, icon = R.drawable.ic_download, enabled = !busy && items.isNotEmpty())
        else AmbrButton("Get Pro to export", onGetPro, icon = R.drawable.ic_crown)
    }) {
        Text("A copy of all ${items.size} items for your records, an insurance claim or a new phone.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary, modifier = Modifier.padding(horizontal = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormatCard(R.drawable.ic_file_pdf, "PDF", "Easy to read and print", pdf, Color(0xFFB42318), Modifier.weight(1f)) { pdf = true }
            FormatCard(R.drawable.ic_file_csv, "CSV", "Opens in Excel or Sheets", !pdf, AmbrColors.CoveredInk, Modifier.weight(1f)) { pdf = false }
        }
        WhiteCard {
            if (pdf) SwitchRow("Include receipt photos", "One photo per item. Bigger file", photos) { photos = it }
            SwitchRow("Include archived items", "Things you sold or gave away", withArchived) { withArchived = it }
        }
    }
}

@Composable
private fun FormatCard(icon: Int, title: String, sub: String, on: Boolean, ink: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(CardShape).background(AmbrColors.Card).border(2.dp, if (on) AmbrColors.Navy else AmbrColors.Ring, CardShape)
            .selectable(on, role = Role.RadioButton, onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(icon), null, tint = ink, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
    }
}

const val PRIVACY_URL = "https://docs.google.com/document/d/11ym5RwBZ6A0QVU6CT2eO6jscY8H1OhNhhdZnYkO3o0Y/edit?usp=sharing"

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Page("Privacy", onBack, bottom = {
        AmbrButton("Read the full privacy policy", {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))) }
        }, style = ButtonStyle.Quiet, height = 52.dp)
    }) {
        Row(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Navy).padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(painterResource(R.drawable.ic_shield_check), null, tint = AmbrColors.HoneyOnNavy, modifier = Modifier.size(24.dp))
            Text("Your receipts never leave this phone unless you back them up.", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        WhiteCard {
            listOf(
                Triple(R.drawable.ic_user, "No account", "Ambr has no sign-up and no server. We don't know who you are."),
                Triple(R.drawable.ic_sparkles, "Reading happens on your phone", "Receipt text and barcodes are read on the device, even offline."),
                Triple(R.drawable.ic_cloud_upload_alt, "Backup goes to your own Drive", "In a private app folder only Ambr can open. We can't see it."),
                Triple(R.drawable.ic_eye_crossed, "No ads, no tracking", "Nothing is sold or shared. Ever."),
            ).forEach { (icon, title, body) ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(AmbrColors.CoveredTint), contentAlignment = Alignment.Center) {
                        Icon(painterResource(icon), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(16.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
                    }
                }
            }
        }
    }
}

private val FAQ = listOf(
    "My receipt is faded. What now?" to "Save the photo anyway and type the item name. A warranty card or the box sticker with the serial also counts as proof at most service centers.",
    "What if I lose my phone?" to "With backup on, install Ambr on the new phone and sign in to the same Google account. We find your backup and restore every item.",
    "Which brands does Ambr know?" to "Walton, Singer, Samsung, Gree, Vision and more. For these we suggest the usual warranty and fill in the service center.",
    "Can my family see my items?" to "Today you can share any receipt as a PDF. A shared family vault is coming with Ambr Pro.",
    "Why only 10 items for free?" to "Ambr has no ads and no server costs to cover with your data. Pro is one payment that keeps it that way.",
)

@Composable
fun HelpScreen(onBack: () -> Unit, onFeedback: () -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableIntStateOf(0) }
    Page("Help", onBack) {
        Text("COMMON QUESTIONS", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted, modifier = Modifier.padding(start = 4.dp))
        Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(horizontal = 16.dp, vertical = 4.dp)) {
            FAQ.forEachIndexed { i, (q, a) ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AmbrColors.Divider))
                Row(Modifier.fillMaxWidth().clickable { open = if (open == i) -1 else i }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(q, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
                    Icon(painterResource(if (open == i) R.drawable.ic_angle_small_down else R.drawable.ic_angle_small_right), null, tint = AmbrColors.TextMuted, modifier = Modifier.size(18.dp))
                }
                AnimatedVisibility(open == i) { Text(a, style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary, modifier = Modifier.padding(bottom = 14.dp)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HelpTile(R.drawable.ic_comment_alt, "Send feedback", "Ideas, bugs, a story", AmbrColors.HoneyTint, AmbrColors.HoneyInk, Modifier.weight(1f), onFeedback)
            HelpTile(R.drawable.ic_envelope, "Email us", "Reply within 2 days", Color(0xFFE6F0FB), Color(0xFF2B5F8E), Modifier.weight(1f)) { email(context, "Ambr help", "") }
        }
    }
}

private fun email(context: android.content.Context, subject: String, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply { if (SUPPORT_EMAIL.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL)) }.putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, body)
    runCatching { context.startActivity(intent) }
}

@Composable
private fun HelpTile(icon: Int, title: String, sub: String, tint: Color, ink: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint), contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, tint = ink, modifier = Modifier.size(17.dp)) }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AmbrColors.TextPrimary)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
    }
}

@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf("Idea") }
    var text by remember { mutableStateOf("") }
    var device by remember { mutableStateOf(true) }
    val kinds = listOf(Triple("Idea", R.drawable.ic_bulb, AmbrColors.HoneyInk), Triple("Bug", R.drawable.ic_triangle_warning, AmbrColors.Danger), Triple("Story", R.drawable.ic_hand_holding_heart, AmbrColors.CoveredInk))
    Page("Send feedback", onBack, bottom = {
        AmbrButton("Send", {
            val extra = if (device) "\n\n—\nAmbr ${BuildConfig.VERSION_NAME} · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}" else ""
            email(context, "Ambr feedback: $kind", text + extra)
            onBack()
        }, style = ButtonStyle.Strong, icon = R.drawable.ic_envelope, enabled = text.isNotBlank())
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            kinds.forEach { (k, icon, ink) ->
                val on = k == kind
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(AmbrColors.Card).border(2.dp, if (on) AmbrColors.Navy else AmbrColors.Ring, RoundedCornerShape(20.dp))
                        .selectable(on, role = Role.RadioButton) { kind = k }.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(painterResource(icon), null, tint = ink, modifier = Modifier.size(20.dp))
                    Text(k, style = MaterialTheme.typography.titleSmall, color = AmbrColors.TextPrimary)
                }
            }
        }
        Text(when (kind) { "Bug" -> "What went wrong?"; "Story" -> "Did Ambr help you? Tell us"; else -> "What would make Ambr better?" }, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, modifier = Modifier.padding(start = 4.dp))
        OutlinedTextField(
            text, { text = it }, Modifier.fillMaxWidth().height(170.dp), shape = RoundedCornerShape(18.dp),
            placeholder = { Text("Write as much or as little as you like", color = AmbrColors.TextPlaceholder) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = AmbrColors.Card, focusedContainerColor = AmbrColors.Card, unfocusedBorderColor = AmbrColors.FieldBorder, focusedBorderColor = AmbrColors.Navy),
        )
        WhiteCard { SwitchRow("Include phone model and app version", "Helps us fix bugs. Never your items.", device) { device = it } }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, onDeleteAll: () -> Unit) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    Page("About", onBack) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_ambr_lockup), "Ambr", tint = Color.Unspecified, modifier = Modifier.height(36.dp).size(148.dp, 36.dp))
            Text("Proof for everything you own", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary)
            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
        }
        Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(vertical = 4.dp)) {
            listOf(
                Triple("Icons", "Uicons by Flaticon", "https://www.flaticon.com/uicons"),
                Triple("Font", "Public Sans, SIL Open Font License", "https://github.com/uswds/public-sans"),
                Triple("Rate Ambr", "On Google Play", "market://details?id=${context.packageName}"),
            ).forEachIndexed { i, (label, sub, url) ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AmbrColors.Divider))
                Row(Modifier.fillMaxWidth().clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                        Text(sub, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                    }
                    Icon(painterResource(R.drawable.ic_angle_small_right), null, tint = AmbrColors.TextPlaceholder, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { confirm = true }.padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_trash), null, tint = AmbrColors.Danger, modifier = Modifier.size(15.dp))
            Spacer(Modifier.size(8.dp))
            Text("Delete all my data", style = MaterialTheme.typography.titleMedium, color = AmbrColors.Danger)
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        containerColor = AmbrColors.Card,
        title = { Text("Delete everything?") },
        text = { Text("Every item, receipt photo and setting on this phone is erased. Your Google Drive backup, if any, is not touched. This can't be undone.") },
        confirmButton = { TextButton({ confirm = false; onDeleteAll() }) { Text("Delete everything", color = AmbrColors.Danger, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton({ confirm = false }) { Text("Cancel", color = AmbrColors.TextPrimary) } },
    )
}

fun openPlayStore(context: android.content.Context) {
    val id = context.packageName
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))) }
        .onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))) } }
}

