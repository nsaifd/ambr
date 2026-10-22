package app.ambr.ui.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.AppContainer
import app.ambr.R
import app.ambr.platform.BackupInfo
import app.ambr.platform.BackupWorker
import app.ambr.platform.DriveAccess
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.components.TopBar
import app.ambr.ui.theme.AmbrColors
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Gets a Drive token, showing Google's consent screen when needed. */
@Composable
private fun rememberDriveAccess(c: AppContainer, onToken: (String) -> Unit, onError: (String) -> Unit): () -> Unit {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        c.backup.tokenFrom(r.data)?.let(onToken) ?: onError("Google sign-in was cancelled")
    }
    return {
        scope.launch {
            when (val a = c.backup.access(activity)) {
                is DriveAccess.Token -> onToken(a.value)
                is DriveAccess.NeedsConsent -> consent.launch(IntentSenderRequest.Builder(a.intent.intentSender).build())
                is DriveAccess.Failed -> onError(a.message)
            }
        }
    }
}

private fun maskEmail(e: String?) = e?.let { val at = it.indexOf('@'); if (at <= 1) it else it.first() + "•".repeat(6) + it.substring(at) }

@Composable
fun BackupScreen(c: AppContainer, onBack: () -> Unit, onRestore: () -> Unit) {
    val s = c.settings
    val on by s.backupOn.collectAsStateWithLifecycle()
    val last by s.lastBackup.collectAsStateWithLifecycle()
    val error by s.backupError.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var photos by remember { mutableStateOf(s.backupPhotos) }
    var wifi by remember { mutableStateOf(s.backupWifiOnly) }

    fun run(token: String, withPhotos: Boolean = photos) = scope.launch {
        busy = true; message = null
        if (s.backupAccount == null) s.backupAccount = c.backup.accountEmail(token)
        runCatching { c.backup.backUp(token, withPhotos) }
            .onSuccess { s.setBackupOn(true); s.setBackupError(null); BackupWorker.schedule(c.app, true, wifi) }
            .onFailure { message = if (it.message == "quota") "Your Google Drive is full. Free up space, or back up without photos." else "Backup didn't finish. Check your connection and try again." }
        busy = false
    }
    val connect = rememberDriveAccess(c, onToken = { run(it) }, onError = { message = it })
    val connectNoPhotos = rememberDriveAccess(c, onToken = { run(it, withPhotos = false) }, onError = { message = it })

    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TopBar("Backup", onBack)
            if (!on) {
                Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(AmbrColors.HoneyTint), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_cloud_upload_alt), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(24.dp))
                    }
                    Text("Your receipts live only on this phone", style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
                    Text("If the phone is lost or broken, the receipts go with it. Back them up to your own Google Drive. It's free.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
                }
                Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Pick your Google account", "Allow Ambr its own private Drive folder", "We back up every night on Wi-Fi").forEachIndexed { i, t ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(AmbrColors.Navy), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text(t, style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextPrimary)
                        }
                    }
                }
            } else {
                val ok = error == null
                Row(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (ok) AmbrColors.CoveredTint else AmbrColors.DangerTint), contentAlignment = Alignment.Center) {
                        Icon(painterResource(if (ok) R.drawable.ic_cloud_check else R.drawable.ic_triangle_warning), null, tint = if (ok) AmbrColors.CoveredInk else AmbrColors.Danger, modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            when { !ok -> "Last backup didn't finish"; last == 0L -> "Backup is on"; else -> "Backed up " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(last)) },
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AmbrColors.TextPrimary,
                        )
                        Text(
                            when (error) { "quota" -> "Your Google Drive is full"; "signin" -> "Sign in to Google again"; null -> "Every night, in your own Google Drive"; else -> "We'll try again tonight" },
                            style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted,
                        )
                    }
                }
                Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Toggle("Include photos", "Receipts and warranty cards", photos) { photos = it; s.backupPhotos = it }
                    Toggle("Only on Wi-Fi", "Saves mobile data", wifi) { wifi = it; s.backupWifiOnly = it; BackupWorker.schedule(c.app, true, it) }
                }
                Row(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val email = s.backupAccount
                    Box(Modifier.size(40.dp).clip(CircleShape).background(AmbrColors.Navy), contentAlignment = Alignment.Center) {
                        Text(email?.first()?.uppercase() ?: "G", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Google account", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                        Text(maskEmail(email) ?: "Connected", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                    }
                    Text("Turn off", style = MaterialTheme.typography.titleSmall, color = AmbrColors.Danger,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { s.setBackupOn(false); s.backupAccount = null; BackupWorker.schedule(c.app, false, wifi) }.padding(8.dp))
                }
                AmbrButton("Restore from Drive", onRestore, style = ButtonStyle.Quiet, icon = R.drawable.ic_cloud_download_alt, height = 52.dp)
            }
            message?.let {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AmbrColors.DangerTint).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(R.drawable.ic_info), null, tint = AmbrColors.Danger, modifier = Modifier.padding(top = 2.dp).size(15.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextPrimary)
                }
            }
            if (error == "quota" || message?.contains("full") == true) AmbrButton("Back up without photos", connectNoPhotos, style = ButtonStyle.Quiet, height = 48.dp)
            Text("Saved in a private folder of your own Google Drive. The people who make Ambr can't open it.", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            AmbrButton(if (busy) "Backing up…" else if (on) "Back up now" else "Turn on backup", connect, style = ButtonStyle.Strong, icon = R.drawable.ic_cloud_upload_alt, enabled = !busy)
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
        }
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = AmbrColors.CoveredFill, checkedThumbColor = AmbrColors.Card))
    }
}

private enum class RestoreStep { Connect, Looking, Found, None, Restoring, Done, Error }

@Composable
fun RestoreScreen(c: AppContainer, onClose: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(RestoreStep.Connect) }
    var token by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<BackupInfo?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0 to 0) }
    var error by remember { mutableStateOf("") }
    val connect = rememberDriveAccess(c, onToken = { t ->
        token = t; step = RestoreStep.Looking
        scope.launch {
            runCatching { email = c.backup.accountEmail(t); c.backup.find(t) }
                .onSuccess { info = it; step = if (it == null) RestoreStep.None else RestoreStep.Found }
                .onFailure { error = "Couldn't reach Google Drive. Check your connection."; step = RestoreStep.Error }
        }
    }, onError = { error = it; step = RestoreStep.Error })
    LaunchedEffect(Unit) { connect() }

    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (step != RestoreStep.Restoring) CircleIconButton(R.drawable.ic_cross_small, "Close", onClose)
        Spacer(Modifier.weight(1f))
        Box(Modifier.align(Alignment.CenterHorizontally).size(84.dp).clip(RoundedCornerShape(26.dp)).background(AmbrColors.Navy), contentAlignment = Alignment.Center) {
            Icon(painterResource(if (step == RestoreStep.Done) R.drawable.ic_check else R.drawable.ic_cloud_download_alt), null, tint = AmbrColors.HoneyOnNavy, modifier = Modifier.size(36.dp))
        }
        val (title, body) = when (step) {
            RestoreStep.Connect, RestoreStep.Looking -> "Looking for your backup…" to "Checking your Google Drive."
            RestoreStep.Found -> "Welcome back" to "We found your Ambr backup on Google Drive. Restore it and everything is here in a minute."
            RestoreStep.None -> "No backup found" to "There's no Ambr backup in ${maskEmail(email) ?: "this Google account"}. Try another account, or start fresh."
            RestoreStep.Restoring -> "Bringing your things back" to "Keep Ambr open. About a minute on Wi-Fi."
            RestoreStep.Done -> "All restored" to "${progress.second} items are back, with their receipts."
            RestoreStep.Error -> "Couldn't restore" to error
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(body, style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (step == RestoreStep.Found) info?.let { i ->
            Row(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("${i.items}", "items"); Stat("${i.photos}", "photos"); Stat(i.modified.take(10), "last backup")
            }
        }
        if (step == RestoreStep.Restoring) {
            Text("${progress.first} of ${progress.second} items", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            LinearProgressIndicator(
                progress = { if (progress.second == 0) 0f else progress.first / progress.second.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = AmbrColors.Honey, trackColor = AmbrColors.Ring,
            )
        }
        Spacer(Modifier.weight(1f))
        when (step) {
            RestoreStep.Found -> {
                AmbrButton("Restore everything", {
                    val t = token ?: return@AmbrButton
                    val i = info ?: return@AmbrButton
                    step = RestoreStep.Restoring
                    scope.launch {
                        runCatching { c.backup.restore(t, i) { done, total -> progress = done to total } }
                            .onSuccess {
                                c.settings.setBackupOn(true); c.settings.backupAccount = email; c.settings.welcomed = true
                                BackupWorker.schedule(c.app, true, c.settings.backupWifiOnly)
                                step = RestoreStep.Done
                            }
                            .onFailure { error = "The download stopped. Check your connection and try again."; step = RestoreStep.Error }
                    }
                }, icon = R.drawable.ic_cloud_download_alt)
                AmbrButton("Start fresh instead", onClose, style = ButtonStyle.Quiet, height = 48.dp)
            }
            RestoreStep.Done -> AmbrButton("Open Ambr", onDone, style = ButtonStyle.Strong)
            RestoreStep.None, RestoreStep.Error -> {
                AmbrButton("Try again", connect)
                AmbrButton("Start fresh", onClose, style = ButtonStyle.Quiet, height = 48.dp)
            }
            else -> Unit
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
    }
}

@Composable
fun WelcomeScreen(onAdd: () -> Unit, onExplore: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(painterResource(R.drawable.ic_ambr_lockup), "Ambr", tint = Color.Unspecified, modifier = Modifier.padding(top = 12.dp).size(98.dp, 24.dp))
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(32.dp)).background(AmbrColors.Navy), contentAlignment = Alignment.Center) {
            Column(Modifier.clip(RoundedCornerShape(24.dp)).background(AmbrColors.Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE6F0FB)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_fan), null, tint = Color(0xFF2B5F8E), modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Ceiling fan", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                        Text("Bedroom · Vision", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                    }
                }
                app.ambr.ui.components.StatusPill(app.ambr.domain.StatusKind.Covered, label = "Covered · 14 months left")
            }
        }
        Text("When something breaks, Ambr has the proof", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary)
        Text("Works offline. No sign-up. Your receipts stay on your phone.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary)
        Spacer(Modifier.weight(1f))
        AmbrButton("Add your first item", onAdd, icon = R.drawable.ic_camera)
        AmbrButton("I'll explore first", onExplore, style = ButtonStyle.Quiet, height = 52.dp)
        Text("Had Ambr before? Restore from Google Drive", style = MaterialTheme.typography.titleSmall, color = AmbrColors.HoneyInk, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onRestore).padding(12.dp))
    }
}
