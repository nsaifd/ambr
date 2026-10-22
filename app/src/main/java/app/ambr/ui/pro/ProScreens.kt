package app.ambr.ui.pro

import android.app.Activity
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import app.ambr.BuildConfig
import app.ambr.R
import app.ambr.platform.Billing
import app.ambr.platform.PurchaseState
import app.ambr.platform.Settings
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.state.ItemsViewModel
import app.ambr.ui.theme.AmbrColors

private val PERKS = listOf(
    R.drawable.ic_apps to "Unlimited items",
    R.drawable.ic_fingerprint to "App lock with fingerprint",
    R.drawable.ic_download to "Export everything to PDF or CSV",
    R.drawable.ic_users to "Share the vault with family",
)

private fun priceLabel(play: String?) = play ?: "৳499"

@Composable
fun ProScreen(billing: Billing, settings: Settings, onClose: () -> Unit) {
    val state by billing.state.collectAsStateWithLifecycle()
    val price by billing.price.collectAsStateWithLifecycle()
    val pro by settings.isPro.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) { billing.connect(); onDispose { billing.reset() } }

    when {
        state == PurchaseState.Success || (pro && state != PurchaseState.Buying) -> { Success(onClose); return }
        state == PurchaseState.Failed || state == PurchaseState.Pending -> { Failed(state == PurchaseState.Pending, onRetry = { billing.reset() }, onRestore = { billing.restore() }, onClose = onClose); return }
    }

    Column(Modifier.fillMaxSize().background(AmbrColors.Ground)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)).background(AmbrColors.Navy)
                    .statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircleIconButton(R.drawable.ic_cross_small, "Close", onClose, background = AmbrColors.NavyRaised, tint = Color.White)
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(AmbrColors.Honey), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_crown), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(26.dp))
                }
                Text("Ambr Pro", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("One payment. Yours forever. No subscription.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFD5DBEA))
            }
            Column(
                Modifier.padding(horizontal = 16.dp).offset(y = (-32).dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(AmbrColors.Card)
                    .border(1.dp, AmbrColors.Ring, RoundedCornerShape(28.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                PERKS.forEachIndexed { i, (icon, label) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AmbrColors.Divider))
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(AmbrColors.HoneyTint), contentAlignment = Alignment.Center) {
                            Icon(painterResource(icon), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(16.dp))
                        }
                        Text(label, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
                        if (i == PERKS.lastIndex) Text("Soon", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AmbrColors.TextSecondary,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFECEEF2)).padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            Row(
                Modifier.padding(horizontal = 16.dp).offset(y = (-18).dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AmbrColors.CoveredTint).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(painterResource(R.drawable.ic_shield_check), null, tint = AmbrColors.CoveredInk, modifier = Modifier.padding(top = 2.dp).size(15.dp))
                Text("Free stays useful: your first ${Settings.FREE_LIMIT} items always keep their claim pack, share and call, even if you never pay.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF11573A))
            }
        }
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val unavailable = state == PurchaseState.Unavailable
            AmbrButton(
                if (state == PurchaseState.Buying) "Opening Google Play…" else "Get Pro for ${priceLabel(price)} once",
                { activity?.let(billing::buy) }, height = 60.dp, icon = R.drawable.ic_crown, enabled = state != PurchaseState.Buying,
            )
            if (unavailable) Text("Google Play isn't reachable right now. Check your connection, or that Play Store is signed in.", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Already bought it? Restore purchase", style = MaterialTheme.typography.titleSmall, color = AmbrColors.TextSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { billing.restore() }.padding(12.dp))
            // Lets testers try Pro features before the Play listing exists.
            if (BuildConfig.DEBUG) Text("Unlock Pro (debug build only)", style = MaterialTheme.typography.labelMedium, color = AmbrColors.HoneyInk, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { settings.setPro(true) }.padding(8.dp))
        }
    }
}

@Composable
private fun Success(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.align(Alignment.CenterHorizontally).size(88.dp).clip(CircleShape).background(AmbrColors.Honey), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_crown), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(36.dp))
        }
        Text("You're on Pro", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Thank you for supporting Ambr. Everything below is unlocked for good.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PERKS.dropLast(1).forEach { (_, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(15.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        AmbrButton("Continue", onDone, style = ButtonStyle.Strong)
    }
}

@Composable
private fun Failed(pending: Boolean, onRetry: () -> Unit, onRestore: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CircleIconButton(R.drawable.ic_cross_small, "Close", onClose)
        Spacer(Modifier.weight(1f))
        Box(Modifier.align(Alignment.CenterHorizontally).size(80.dp).clip(CircleShape).background(if (pending) AmbrColors.EndsSoonTint else AmbrColors.DangerTint), contentAlignment = Alignment.Center) {
            Icon(painterResource(if (pending) R.drawable.ic_clock_three else R.drawable.ic_exclamation), null, tint = if (pending) AmbrColors.EndsSoonInk else AmbrColors.Danger, modifier = Modifier.size(30.dp))
        }
        Text(if (pending) "Payment is pending" else "Payment didn't go through", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(if (pending) "Google Play is still confirming it." else "Nothing was charged. Your ${Settings.FREE_LIMIT} free items work exactly as before.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.HoneyTint).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Paid with mobile balance?", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
            Text("Google Play may show it as pending for a few minutes. Pro turns on by itself once it clears. No need to pay twice.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
        }
        Spacer(Modifier.weight(1f))
        if (!pending) AmbrButton("Try again", onRetry)
        AmbrButton("Restore an earlier purchase", onRestore, style = ButtonStyle.Quiet, height = 48.dp)
    }
}

@Composable
fun LimitReachedScreen(vm: ItemsViewModel, onPro: () -> Unit, onArchive: () -> Unit, onClose: () -> Unit) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.weight(1f))
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(Settings.FREE_LIMIT) { Box(Modifier.size(12.dp).clip(CircleShape).background(AmbrColors.Honey)) }
        }
        Text("You've saved ${overview.items.size} items", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(
            "That's the free limit. Your ${Settings.FREE_LIMIT} items keep everything: claim pack, sharing and reminders. To add this one and more, get Pro once.",
            style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_pencil), null, tint = AmbrColors.TextSecondary, modifier = Modifier.size(16.dp))
            Text("Your new item is kept as a draft until you decide", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
        }
        Spacer(Modifier.weight(1f))
        AmbrButton("Get Pro once", onPro, icon = R.drawable.ic_crown)
        AmbrButton("Archive an old item to make room", onArchive, style = ButtonStyle.Quiet, icon = R.drawable.ic_archive, height = 52.dp)
        Text("Not now", style = MaterialTheme.typography.titleSmall, color = AmbrColors.TextSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClose).padding(12.dp))
    }
}
