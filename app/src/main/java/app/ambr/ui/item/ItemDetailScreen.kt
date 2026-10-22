package app.ambr.ui.item

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.data.ArchiveReason
import app.ambr.data.ProofEntity
import app.ambr.data.ProofKind
import app.ambr.domain.CoverageState
import app.ambr.domain.Format
import app.ambr.domain.StatusKind
import app.ambr.domain.Warranty
import app.ambr.platform.Intents
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.components.KeyValueRow
import app.ambr.ui.components.StatusPill
import app.ambr.ui.components.visual
import app.ambr.ui.state.ItemUi
import app.ambr.ui.state.ItemViewModel
import app.ambr.ui.theme.AmbrColors
import coil.compose.AsyncImage
import java.io.File
import java.time.LocalDate

@Composable
fun ItemDetailScreen(
    vm: ItemViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onClaim: () -> Unit,
    onLogRepair: () -> Unit,
    onAddProof: () -> Unit,
    onOpenProof: (Int) -> Unit,
) {
    val ui by vm.item.collectAsStateWithLifecycle()
    val current = ui ?: return
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var sellSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val v = current.item.category.visual(current.item.name)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)).background(v.tone.tile)
                    .statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 52.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(R.drawable.ic_angle_small_left, "Back", onBack)
                    Spacer(Modifier.weight(1f))
                    CircleIconButton(R.drawable.ic_pencil, "Edit", onEdit)
                    Spacer(Modifier.width(8.dp))
                    Box {
                        CircleIconButton(R.drawable.ic_menu_dots, "More: share, mark as sold, delete", { menu = true })
                        DropdownMenu(menu, { menu = false }, containerColor = AmbrColors.Card) {
                            DropdownMenuItem({ Text("Share receipt") }, { menu = false; share(context, current) }, leadingIcon = { MenuIcon(R.drawable.ic_share) })
                            DropdownMenuItem({ Text("Mark as sold or given away") }, { menu = false; sellSheet = true }, leadingIcon = { MenuIcon(R.drawable.ic_archive) })
                            DropdownMenuItem({ Text("Delete", color = AmbrColors.Danger) }, { menu = false; confirmDelete = true }, leadingIcon = { MenuIcon(R.drawable.ic_trash, AmbrColors.Danger) })
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    current.item.photoPath?.let { path ->
                        AsyncImage(File(path), null, Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop)
                    } ?: Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(AmbrColors.Card.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(v.icon), null, tint = v.tone.ink, modifier = Modifier.size(52.dp))
                    }
                    Text(current.item.name, style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag(current.item.room.label)
                        Tag(current.item.category.label)
                    }
                }
            }
        }
        item { StatusCard(current, onClaim, Modifier.padding(horizontal = 16.dp).offset(y = (-32).dp)) }
        item {
            Column(Modifier.padding(horizontal = 16.dp).offset(y = (-18).dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                CheckUpBanner(current, onAllGood = { vm.dismissReminder() })
                QuickActions(current, onLogRepair)
                ProofSection(current.details.proofs, onAddProof, onOpenProof)
                CoverageSection(current.status.coverages)
                DetailsSection(current)
                HistorySection(current)
            }
        }
    }

    if (sellSheet) MarkSoldSheet(current.item.name, onDismiss = { sellSheet = false }) { reason ->
        sellSheet = false
        vm.archive(reason, LocalDate.now())
        onBack()
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        containerColor = AmbrColors.Card,
        title = { Text("Delete ${current.item.name}?") },
        text = { Text("Its receipt, warranty card and service history are removed from this phone. This can't be undone. If you sold it, archive it instead.") },
        confirmButton = { TextButton({ confirmDelete = false; vm.delete(onBack) }) { Text("Delete", color = AmbrColors.Danger, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep it", color = AmbrColors.TextPrimary) } },
    )
}

private fun share(context: android.content.Context, ui: ItemUi) {
    val s = ui.status
    val text = buildString {
        append(ui.item.name)
        ui.item.purchaseDate?.let { append("\nBought ${Format.date(it)}") }
        ui.item.store?.let { append(" at $it") }
        ui.item.serial?.let { append("\nSerial: $it") }
        append("\n${Warranty.headline(s)}")
    }
    Intents.shareFiles(context, ui.details.proofs.sortedBy { it.page }.map { File(it.filePath) }, text)
}

@Composable
private fun MenuIcon(icon: Int, tint: androidx.compose.ui.graphics.Color = AmbrColors.TextPrimary) =
    Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(17.dp))

@Composable
private fun Tag(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(AmbrColors.Card.copy(alpha = 0.75f)).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun CoverageRing(ui: ItemUi, size: androidx.compose.ui.unit.Dp = 104.dp) {
    val s = ui.status
    val left = (1f - (s.fractionUsed ?: 1f)).coerceIn(0f, 1f)
    val color = when (s.kind) {
        StatusKind.Covered -> AmbrColors.CoveredFill
        StatusKind.EndsSoon -> AmbrColors.Honey
        else -> AmbrColors.Control
    }
    val (big, unit) = when {
        s.kind == StatusKind.Expired || s.kind == StatusKind.NoCoverage -> "0" to "left"
        (s.monthsLeft ?: 0) >= 2 -> "${s.monthsLeft}" to "months"
        s.daysLeft != null -> "${s.daysLeft}" to if (s.daysLeft == 1L) "day" else "days"
        else -> "✓" to "covered"
    }
    Box(Modifier.size(size).semantics { contentDescription = "$big $unit left" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(AmbrColors.Divider, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            if (left > 0f) drawArc(color, -90f, 360f * left, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(big, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = AmbrColors.TextPrimary, lineHeight = 30.sp)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
        }
    }
}

@Composable
private fun StatusCard(ui: ItemUi, onClaim: () -> Unit, modifier: Modifier) {
    val s = ui.status
    val longest = ui.details.coverages.filter { it.months > 0 }.maxByOrNull { it.months }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(28.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CoverageRing(ui)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(s.kind)
                Text(
                    when (s.kind) {
                        StatusKind.Covered, StatusKind.EndsSoon -> s.until?.let { "Covered until ${Format.date(it)}" } ?: "Covered"
                        else -> Warranty.headline(s)
                    },
                    style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary,
                )
                val bought = ui.item.purchaseDate?.let { "Bought ${Format.date(it)}" }
                val length = longest?.let { "${Format.warrantyLength(it.months).replace(" years", "-year").replace(" year", "-year")} warranty" }
                listOfNotNull(bought, length).takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                }
            }
        }
        AmbrButton("Something broke", onClaim, icon = R.drawable.ic_wrench_simple)
    }
}

/** The last free check-up window: shown while a warranty ends within 30 days, until "All good". */
@Composable
private fun CheckUpBanner(ui: ItemUi, onAllGood: () -> Unit) {
    if (ui.status.kind != StatusKind.EndsSoon || ui.details.coverages.any { it.reminderDismissed }) return
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Navy).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_bulb), null, tint = AmbrColors.HoneyOnNavy, modifier = Modifier.padding(top = 2.dp).size(20.dp))
            Column {
                Text("Last free check-up window", style = MaterialTheme.typography.titleMedium, color = AmbrColors.Card)
                Text("Anything not working right? A check-up is free for ${ui.status.daysLeft} more days. After that, repairs cost money.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.OnDarkMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AmbrButton("All good", onAllGood, Modifier.weight(1f), style = ButtonStyle.Quiet, height = 44.dp)
            ui.item.servicePhone?.let { phone ->
                AmbrButton("Book check-up", { Intents.dial(context, phone) }, Modifier.weight(1f), icon = R.drawable.ic_phone_call, height = 44.dp)
            }
        }
    }
}

@Composable
private fun QuickActions(ui: ItemUi, onLogRepair: () -> Unit) {
    val context = LocalContext.current
    val phone = ui.item.servicePhone
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction(R.drawable.ic_phone_call, if (phone != null) "Call service" else "No number", Modifier.weight(1f), enabled = phone != null) { phone?.let { Intents.dial(context, it) } }
        QuickAction(R.drawable.ic_share, "Share receipt", Modifier.weight(1f)) { share(context, ui) }
        QuickAction(R.drawable.ic_time_past, "Log repair", Modifier.weight(1f), onClick = onLogRepair)
    }
}

@Composable
private fun QuickAction(icon: Int, label: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(AmbrColors.Ground), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = if (enabled) AmbrColors.TextPrimary else AmbrColors.TextPlaceholder, modifier = Modifier.size(17.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (enabled) AmbrColors.TextPrimary else AmbrColors.TextMuted)
    }
}

@Composable
private fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) Row(
            Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(painterResource(R.drawable.ic_plus), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(12.dp))
            Text(action, style = MaterialTheme.typography.labelMedium, color = AmbrColors.HoneyInk)
        }
    }
}

@Composable
private fun ProofSection(proofs: List<ProofEntity>, onAdd: () -> Unit, onOpen: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Proof", "Add page", onAdd)
        if (proofs.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.HoneyTint).clickable(onClick = onAdd).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_camera), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text("No receipt yet", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                    Text("Snap the receipt or warranty card. Shops ask for it first.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
                }
            }
        } else {
            val sorted = proofs.sortedBy { it.page }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sorted.size) { i ->
                    val p = sorted[i]
                    Column(
                        Modifier.width(150.dp).clip(RoundedCornerShape(20.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(20.dp))
                            .clickable { onOpen(i) }.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AsyncImage(File(p.filePath), p.kind.label, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(15.dp)).background(AmbrColors.Tile), contentScale = ContentScale.Crop)
                        Row(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(if (p.kind == ProofKind.WarrantyCard) R.drawable.ic_badge_check else R.drawable.ic_receipt), null, tint = AmbrColors.TextSecondary, modifier = Modifier.size(13.dp))
                            Text(p.kind.label, style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverageSection(coverages: List<CoverageState>) {
    if (coverages.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Coverage")
        Column(
            Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            coverages.forEach { c -> CoverageRow(c) }
        }
    }
}

@Composable
private fun CoverageRow(c: CoverageState) {
    val cov = c.coverage
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val title = if (cov.months > 0) "${cov.label} · ${Format.warrantyLength(cov.months)}" else cov.label
            Text(title, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
            val right = when {
                !c.active -> "Ended"
                c.servicesLeft != null && c.end == null -> "${c.servicesLeft} left"
                (c.daysLeft ?: 0) > 60 -> "${(c.daysLeft ?: 0) / 30} mo left"
                else -> "${c.daysLeft} days left"
            }
            Text(right, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (c.active) AmbrColors.CoveredInk else AmbrColors.ExpiredInk)
        }
        val total = cov.servicesTotal
        if (total != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(total) { i ->
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (i < cov.servicesUsed) AmbrColors.Control else AmbrColors.CoveredFill))
                }
            }
            Text("${cov.servicesUsed} of $total used", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
        } else {
            val left = 1f - (c.fractionUsed ?: 1f)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(AmbrColors.Divider)) {
                Box(Modifier.fillMaxWidth(left.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (c.active) AmbrColors.CoveredFill else AmbrColors.Control))
            }
            c.end?.let { Text("${cov.type.label} · ${if (c.active) "until" else "ended"} ${Format.date(it)}", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted) }
        }
    }
}

@Composable
private fun DetailsSection(ui: ItemUi) {
    val context = LocalContext.current
    val i = ui.item
    val rows = buildList<Pair<String, String>> {
        listOfNotNull(i.brand, i.model).takeIf { it.isNotEmpty() }?.let { add("Brand · model" to it.joinToString(" ")) }
        i.serial?.let { add("Serial" to it) }
        i.priceTaka?.let { add("Price" to Format.taka(it)) }
        i.purchaseDate?.let { add("Bought on" to Format.date(it)) }
        i.store?.let { add("Store" to it) }
        i.serviceName?.let { add("Service center" to it) }
    }
    if (rows.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Details")
        Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(horizontal = 16.dp, vertical = 2.dp)) {
            rows.forEachIndexed { idx, (k, v) ->
                KeyValueRow(k, v, last = idx == rows.lastIndex, trailing = if (k == "Serial") {
                    {
                        Box(
                            Modifier.padding(start = 8.dp).size(32.dp).clip(CircleShape).background(AmbrColors.HoneyTint).clickable { Intents.copy(context, "Serial number", v) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(painterResource(R.drawable.ic_copy), "Copy serial number", tint = AmbrColors.HoneyInk, modifier = Modifier.size(15.dp)) }
                    }
                } else null)
            }
        }
        i.notes?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun HistorySection(ui: ItemUi) {
    data class Event(val date: LocalDate?, val title: String, val sub: String?, val icon: Int, val ok: Boolean)
    val events = ui.details.repairs.sortedByDescending { it.date }.map {
        val cost = if (it.costTaka > 0) Format.taka(it.costTaka) else "free"
        Event(it.date, "${it.problem} · ${it.outcome.label.lowercase()} ${if (it.outcome.name == "Pending") "" else cost}".trim(), it.notes, R.drawable.ic_check, true)
    } + listOfNotNull(ui.item.purchaseDate?.let { Event(it, "Bought", ui.item.store, R.drawable.ic_box, false) })
    if (events.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Service history")
        Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp)) {
            events.forEachIndexed { idx, e ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(if (e.ok) AmbrColors.CoveredTint else AmbrColors.Ground), contentAlignment = Alignment.Center) {
                            Icon(painterResource(e.icon), null, tint = if (e.ok) AmbrColors.CoveredInk else AmbrColors.TextSecondary, modifier = Modifier.size(12.dp))
                        }
                        if (idx < events.lastIndex) Box(Modifier.width(2.dp).height(36.dp).background(AmbrColors.Divider))
                    }
                    Column(Modifier.padding(bottom = if (idx < events.lastIndex) 10.dp else 0.dp)) {
                        e.date?.let { Text(Format.date(it), style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted) }
                        Text(e.title, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                        e.sub?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkSoldSheet(name: String, onDismiss: () -> Unit, onPick: (ArchiveReason) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AmbrColors.Card) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("What happened to $name?", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary)
            Text("It moves to Archive with its receipt. Reminders stop. You can bring it back any time.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
            AmbrButton("I sold it", { onPick(ArchiveReason.Sold) }, style = ButtonStyle.Strong)
            AmbrButton("I gave it away", { onPick(ArchiveReason.GaveAway) }, style = ButtonStyle.Quiet)
        }
    }
}
