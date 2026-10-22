package app.ambr.ui.lock

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.ambr.R
import app.ambr.platform.AppLock
import app.ambr.platform.Settings
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.theme.AmbrColors
import kotlinx.coroutines.delay

private tailrec fun Context.fragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.fragmentActivity()
    else -> null
}

fun canUseBiometric(context: Context) = BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

private fun promptBiometric(context: Context, onSuccess: () -> Unit) {
    val activity = context.fragmentActivity() ?: return
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
    })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder().setTitle("Unlock Ambr").setNegativeButtonText("Use PIN").setAllowedAuthenticators(BIOMETRIC_WEAK).build(),
    )
}

/** PIN pad on navy. Shown over everything while the app is locked. */
@Composable
fun LockScreen(lock: AppLock, settings: Settings) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val bio = settings.useBiometric && canUseBiometric(context)
    val keyguard = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { if (it.resultCode == Activity.RESULT_OK) lock.unlock() }
    LaunchedEffect(Unit) { if (bio) promptBiometric(context) { lock.unlock() } }
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (!lock.check(pin)) { wrong = true; delay(450); pin = "" }
        } else if (pin.isNotEmpty()) wrong = false
    }
    Column(
        Modifier.fillMaxSize().background(AmbrColors.Navy).clickable(enabled = false) {}.statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(AmbrColors.NavyRaised), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_lock), null, tint = AmbrColors.HoneyOnNavy, modifier = Modifier.size(28.dp))
        }
        Text(if (wrong) "Wrong PIN. Try again" else "Enter your PIN", style = MaterialTheme.typography.headlineSmall, color = if (wrong) Color(0xFFFFB4AB) else Color.White)
        Text("Ambr is locked to keep your receipts private", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.OnDarkMuted, textAlign = TextAlign.Center)
        PinDots(pin.length, wrong)
        Spacer(Modifier.height(8.dp))
        PinPad(
            onDigit = { if (pin.length < 4) pin += it },
            onDelete = { pin = pin.dropLast(1) },
            onBiometric = if (bio) ({ promptBiometric(context) { lock.unlock() } }) else null,
        )
        Spacer(Modifier.weight(1f))
        val km = context.getSystemService(KeyguardManager::class.java)
        if (km.isDeviceSecure) Text(
            "Forgot PIN? Use your phone's screen lock", style = MaterialTheme.typography.titleSmall, color = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                @Suppress("DEPRECATION")
                km.createConfirmDeviceCredentialIntent("Unlock Ambr", null)?.let { keyguard.launch(it) }
            }.padding(12.dp),
        )
    }
}

@Composable
private fun PinDots(filled: Int, wrong: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) { i ->
            Box(Modifier.size(14.dp).clip(CircleShape).background(when { wrong -> Color(0xFFFFB4AB); i < filled -> AmbrColors.HoneyOnNavy; else -> AmbrColors.NavyRaised }))
        }
    }
}

@Composable
private fun PinPad(onDigit: (String) -> Unit, onDelete: () -> Unit, onBiometric: (() -> Unit)?, dark: Boolean = true) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "bio", "0", "del")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { k ->
                    val visible = k != "bio" || onBiometric != null
                    Box(
                        Modifier.size(72.dp).clip(CircleShape)
                            .background(if (k.length == 1) (if (dark) AmbrColors.NavyRaised else AmbrColors.Card) else Color.Transparent)
                            .clickable(enabled = visible, role = Role.Button) {
                                when (k) { "bio" -> onBiometric?.invoke(); "del" -> onDelete(); else -> onDigit(k) }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            k == "bio" && visible -> Icon(painterResource(R.drawable.ic_fingerprint), "Use fingerprint", tint = if (dark) AmbrColors.HoneyOnNavy else AmbrColors.HoneyInk, modifier = Modifier.size(26.dp))
                            k == "del" -> Icon(painterResource(R.drawable.ic_delete), "Delete", tint = if (dark) Color.White else AmbrColors.TextPrimary, modifier = Modifier.size(24.dp))
                            k.length == 1 -> Text(k, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = if (dark) Color.White else AmbrColors.TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

/** Turn on: choose fingerprint or PIN only and the delay, then pick a PIN twice. */
@Composable
fun AppLockSetupScreen(lock: AppLock, settings: Settings, isPro: Boolean, onGetPro: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val hasBio = remember { canUseBiometric(context) }
    var useBio by remember { mutableStateOf(hasBio) }
    var delaySec by remember { mutableStateOf(60) }
    var step by remember { mutableStateOf(if (lock.enabled) 3 else 0) } // 0 options, 1 pin, 2 confirm, 3 on
    var first by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    LaunchedEffect(pin) {
        if (pin.length < 4) return@LaunchedEffect
        delay(150)
        if (step == 1) { first = pin; pin = ""; step = 2 }
        else if (step == 2) {
            if (pin == first) { lock.turnOn(pin, useBio, delaySec); step = 3 } else { mismatch = true; pin = ""; first = ""; step = 1 }
        }
    }

    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(R.drawable.ic_angle_small_left, "Back", { if (step in 1..2) { step = 0; pin = "" } else onBack() })
            Spacer(Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(50)).background(AmbrColors.HoneyTint).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(painterResource(R.drawable.ic_crown), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(12.dp))
                Text("Pro", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AmbrColors.HoneyInk)
            }
        }
        when (step) {
            0 -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(80.dp).clip(RoundedCornerShape(26.dp)).background(AmbrColors.Navy), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_lock), null, tint = AmbrColors.HoneyOnNavy, modifier = Modifier.size(36.dp))
                }
                Text("Lock Ambr", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("Receipts show what your home owns and what it cost. Keep them for your eyes only.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                if (hasBio) Option(R.drawable.ic_fingerprint, "Fingerprint and PIN", "Fastest. PIN if fingerprint fails", useBio) { useBio = true }
                Option(R.drawable.ic_key, "PIN only", "A 4-digit code just for Ambr", !useBio) { useBio = false }
                Text("Lock when I leave the app", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AmbrColors.Ring).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0 to "Right away", 60 to "After 1 min", 300 to "After 5 min").forEach { (s, label) ->
                        val on = s == delaySec
                        Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) AmbrColors.Card else AmbrColors.Ring).selectable(on, role = Role.RadioButton) { delaySec = s }, contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, color = if (on) AmbrColors.TextPrimary else AmbrColors.TextMuted)
                        }
                    }
                }
            }
            1, 2 -> Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(Modifier.weight(1f))
                Text(if (step == 1) "Pick a 4-digit PIN" else "Enter it again", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary)
                Text(if (mismatch && step == 1) "The PINs didn't match. Try again." else "Not your phone's PIN. Just for Ambr.", style = MaterialTheme.typography.bodyMedium, color = if (mismatch && step == 1) AmbrColors.Danger else AmbrColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { i -> Box(Modifier.size(14.dp).clip(CircleShape).background(if (i < pin.length) AmbrColors.Navy else AmbrColors.Control)) }
                }
                PinPad({ if (pin.length < 4) pin += it }, { pin = pin.dropLast(1) }, null, dark = false)
                Spacer(Modifier.weight(1f))
            }
            else -> Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(80.dp).clip(CircleShape).background(AmbrColors.CoveredTint), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(30.dp))
                }
                Text("App lock is on", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary)
                Text("Ambr asks for your PIN${if (settings.useBiometric && hasBio) " or fingerprint" else ""} when you come back.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.weight(1f))
            }
        }
        when (step) {
            0 -> if (isPro) AmbrButton("Turn on app lock", { step = 1; mismatch = false }, icon = R.drawable.ic_lock) else AmbrButton("Get Pro to use app lock", onGetPro, icon = R.drawable.ic_crown)
            3 -> {
                AmbrButton("Done", onBack, style = app.ambr.ui.components.ButtonStyle.Strong)
                AmbrButton("Turn off app lock", { lock.turnOff(); onBack() }, style = app.ambr.ui.components.ButtonStyle.Quiet, height = 48.dp)
            }
        }
    }
}

@Composable
private fun Option(icon: Int, title: String, sub: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(2.dp, if (on) AmbrColors.Navy else AmbrColors.Ring, CardShape)
            .selectable(on, role = Role.RadioButton, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(AmbrColors.Ground), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
        }
        Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) AmbrColors.Navy else AmbrColors.Control, CircleShape).padding(4.dp).clip(CircleShape).background(if (on) AmbrColors.Navy else Color.Transparent))
    }
}
