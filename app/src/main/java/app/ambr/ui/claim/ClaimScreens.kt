package app.ambr.ui.claim

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.data.ProofEntity
import app.ambr.domain.Format
import app.ambr.domain.StatusKind
import app.ambr.platform.ClaimPdf
import app.ambr.platform.Intents
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.components.KeyValueRow
import app.ambr.ui.components.visual
import app.ambr.ui.state.ItemViewModel
import app.ambr.ui.theme.AmbrColors
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ClaimPackScreen(vm: ItemViewModel, onBack: () -> Unit, onCounter: () -> Unit, onOpenProof: (Int) -> Unit, onAddProof: () -> Unit) {
    val ui by vm.item.collectAsStateWithLifecycle()
    val current = ui ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = current.status
    val proofs = current.details.proofs.sortedBy { it.page }
    var tab by remember { mutableIntStateOf(0) }
    val i = current.item
    val v = i.category.visual(i.name)
    val active = s.kind == StatusKind.Covered || s.kind == StatusKind.EndsSoon

    Column(Modifier.fillMaxSize().background(AmbrColors.Ground)) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)).background(AmbrColors.Navy)
                        .statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircleIconButton(R.drawable.ic_angle_small_left, "Back", onBack, background = AmbrColors.NavyRaised, tint = AmbrColors.Card)
                        Text("Claim pack", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.Card)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(v.icon), null, tint = AmbrColors.OnDarkMuted, modifier = Modifier.size(15.dp))
                        Text("${i.name} · ${i.room.label}", style = MaterialTheme.typography.labelMedium, color = AmbrColors.OnDarkMuted)
                    }
                    Text(
                        if (active) "Covered until\n${s.until?.let(Format::date) ?: "—"}" else app.ambr.domain.Warranty.headline(s),
                        style = MaterialTheme.typography.headlineLarge, color = AmbrColors.Card,
                    )
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(if (active) Color(0x3329B36F) else AmbrColors.NavyRaised).padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(painterResource(if (active) R.drawable.ic_shield_check else R.drawable.ic_info), null, tint = if (active) Color(0xFF7FE0AF) else AmbrColors.OnDarkMuted, modifier = Modifier.size(13.dp))
                        val left = when {
                            !active -> "Repair will likely be paid. Ask anyway; some shops help."
                            (s.monthsLeft ?: 0) >= 2 -> "Repair should be free · ${s.monthsLeft} months left"
                            else -> "Repair should be free · ${s.daysLeft} days left"
                        }
                        Text(left, style = MaterialTheme.typography.labelMedium, color = if (active) Color(0xFFBFF0D6) else AmbrColors.OnDarkMuted)
                    }
                }
            }
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (proofs.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.HoneyTint).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("No receipt saved", style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
                            Text("Many service centers accept the serial number and box sticker. Show them the details below, or snap the warranty card now.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
                            AmbrButton("Add proof", onAddProof, icon = R.drawable.ic_camera, height = 48.dp)
                        }
                    } else {
                        if (proofs.size > 1) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AmbrColors.Ring).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            proofs.forEachIndexed { idx, p ->
                                Box(
                                    Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp)).background(if (idx == tab) AmbrColors.Card else AmbrColors.Ring)
                                        .clickable(role = Role.Tab) { tab = idx },
                                    contentAlignment = Alignment.Center,
                                ) { Text(if (proofs.count { it.kind == p.kind } > 1) "${p.kind.label} ${p.page + 1}" else p.kind.label, style = MaterialTheme.typography.titleSmall, color = AmbrColors.TextPrimary) }
                            }
                        }
                        val p = proofs[tab.coerceAtMost(proofs.lastIndex)]
                        Box(Modifier.fillMaxWidth().height(300.dp).clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).clickable { onOpenProof(proofs.indexOf(p)) }) {
                            AsyncImage(File(p.filePath), p.kind.label, Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Fit)
                            Box(Modifier.align(Alignment.BottomEnd).padding(14.dp).size(40.dp).clip(CircleShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_expand), "Zoom", tint = AmbrColors.TextPrimary, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                    i.serial?.let { serial ->
                        Row(
                            Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Serial number", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted)
                                Text(serial, fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AmbrColors.TextPrimary)
                            }
                            Box(Modifier.size(44.dp).clip(CircleShape).background(AmbrColors.HoneyTint).clickable { Intents.copy(context, "Serial number", serial) }, contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_copy), "Copy serial number", tint = AmbrColors.HoneyInk, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                    val rows = listOfNotNull(
                        i.purchaseDate?.let { "Bought on" to Format.date(it) },
                        i.priceTaka?.let { "Price" to Format.taka(it) },
                        i.store?.let { "Store" to it },
                        i.model?.let { "Model" to it },
                    )
                    if (rows.isNotEmpty()) Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(horizontal = 16.dp, vertical = 2.dp)) {
                        rows.forEachIndexed { idx, (k, value) -> KeyValueRow(k, value, last = idx == rows.lastIndex) }
                    }
                    if (i.serviceName != null || i.servicePhone != null) Row(
                        Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(AmbrColors.Ground), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_tools), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(i.serviceName ?: "Service center", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                            Text(listOfNotNull(i.serviceAddress, i.servicePhone).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                        }
                        i.servicePhone?.let { phone ->
                            Row(
                                Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(AmbrColors.Navy).clickable { Intents.dial(context, phone) }.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(painterResource(R.drawable.ic_phone_call), null, tint = AmbrColors.Card, modifier = Modifier.size(15.dp))
                                Text("Call", style = MaterialTheme.typography.titleSmall, color = AmbrColors.Card)
                            }
                        }
                    }
                    Text("After the visit, log what happened from the item page so your service history stays complete.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(AmbrColors.Ground).padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AmbrButton("Share PDF", {
                scope.launch {
                    val file = withContext(Dispatchers.IO) { ClaimPdf.build(context, current) }
                    Intents.shareFiles(context, listOf(file), "${i.name} warranty claim", mime = "application/pdf")
                }
            }, Modifier.weight(1f), style = ButtonStyle.Quiet, icon = R.drawable.ic_file_pdf)
            AmbrButton("Show at counter", onCounter, Modifier.weight(1f), style = ButtonStyle.Strong, icon = R.drawable.ic_brightness, enabled = proofs.isNotEmpty() || i.serial != null)
        }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Full brightness, plain white, nothing to tap by mistake. For handing the phone across a shop counter. */
@Composable
fun ShowCounterScreen(vm: ItemViewModel, onClose: () -> Unit) {
    val ui by vm.item.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.activity()?.window
        val before = window?.attributes?.screenBrightness
        window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }; w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        onDispose {
            window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = before ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }; w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }
    val current = ui ?: return
    val proofs = current.details.proofs.sortedBy { it.page }
    val pager = rememberPagerState { proofs.size.coerceAtLeast(1) }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(R.drawable.ic_cross_small, "Close", onClose)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(AmbrColors.HoneyTint).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(R.drawable.ic_brightness), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(13.dp))
                Text("Brightness at max", style = MaterialTheme.typography.labelMedium, color = AmbrColors.HoneyInk)
            }
        }
        Text(listOfNotNull(current.item.name, current.item.purchaseDate?.let { "bought ${Format.date(it)}" }).joinToString(" · "), style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextSecondary)
        current.status.until?.takeIf { current.status.kind == StatusKind.Covered || current.status.kind == StatusKind.EndsSoon }?.let {
            Text("Covered until ${Format.date(it)}", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.CoveredInk)
        }
        if (proofs.isNotEmpty()) {
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                AsyncImage(File(proofs[page].filePath), proofs[page].kind.label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        } else Spacer(Modifier.weight(1f))
        current.item.serial?.let {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AmbrColors.Tile).padding(16.dp)) {
                Text("Serial number", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted)
                Text(it, fontFamily = FontFamily.Monospace, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AmbrColors.TextPrimary)
            }
        }
        if (proofs.size > 1) Text(
            if (pager.currentPage < proofs.lastIndex) "Swipe for the ${proofs[pager.currentPage + 1].kind.label.lowercase()}" else "Swipe back for the ${proofs[0].kind.label.lowercase()}",
            style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted, modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** Full-screen proof pages with pinch to zoom and double tap to reset. */
@Composable
fun ProofViewerScreen(vm: ItemViewModel, start: Int, onClose: () -> Unit, onDelete: (ProofEntity) -> Unit) {
    val ui by vm.item.collectAsStateWithLifecycle()
    val proofs = ui?.details?.proofs?.sortedBy { it.page } ?: return
    if (proofs.isEmpty()) { onClose(); return }
    val pager = rememberPagerState(initialPage = start.coerceIn(0, proofs.lastIndex)) { proofs.size }
    var confirm by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color(0xFF0E1320))) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page -> ZoomableImage(File(proofs[page].filePath)) }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIconButton(R.drawable.ic_cross_small, "Close", onClose, background = AmbrColors.NavyRaised, tint = Color.White)
            Text("${proofs[pager.currentPage].kind.label} · ${pager.currentPage + 1} of ${proofs.size}", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
            CircleIconButton(R.drawable.ic_trash, "Delete this page", { confirm = true }, background = AmbrColors.NavyRaised, tint = Color.White)
        }
    }
    if (confirm) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirm = false },
        containerColor = AmbrColors.Card,
        title = { Text("Delete this page?") },
        text = { Text("The photo is removed from this phone. The item stays.") },
        confirmButton = { androidx.compose.material3.TextButton({ confirm = false; onDelete(proofs[pager.currentPage]) }) { Text("Delete", color = AmbrColors.Danger, fontWeight = FontWeight.Bold) } },
        dismissButton = { androidx.compose.material3.TextButton({ confirm = false }) { Text("Keep", color = AmbrColors.TextPrimary) } },
    )
}

@Composable
private fun ZoomableImage(file: File) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    AsyncImage(
        file, null,
        Modifier.fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero }) }
            .transformable(state, canPan = { scale > 1f })
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
        contentScale = ContentScale.Fit,
    )
}
