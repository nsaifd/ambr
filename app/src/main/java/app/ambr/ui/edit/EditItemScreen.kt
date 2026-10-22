package app.ambr.ui.edit

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.data.Category
import app.ambr.data.CoverageEntity
import app.ambr.data.CoverageType
import app.ambr.data.Room
import app.ambr.domain.Brands
import app.ambr.domain.Format
import app.ambr.domain.Warranty
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.ChipGroup
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.components.LabeledField
import app.ambr.ui.state.Draft
import app.ambr.ui.state.EditorViewModel
import app.ambr.ui.state.Field
import app.ambr.ui.theme.AmbrColors
import coil.compose.AsyncImage
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val LENGTHS = listOf(6, 12, 24, 36, 60)

@Composable
fun EditItemScreen(
    vm: EditorViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onLimit: () -> Unit,
    onDelete: (() -> Unit)?,
    onScanSerial: (() -> Unit)?,
) {
    val d by vm.draft.collectAsStateWithLifecycle()
    var more by rememberSaveable { mutableStateOf(!vm.isNew) }
    var pickDate by remember { mutableStateOf(false) }
    var coverageSheet by remember { mutableStateOf<CoverageEntity?>(null) }
    var newCoverage by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircleIconButton(if (vm.isNew) R.drawable.ic_angle_small_left else R.drawable.ic_cross_small, if (vm.isNew) "Back" else "Cancel", onBack)
                Text(if (vm.isNew) "New item" else "Edit item", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
                if (d.readCount > 0) Row(
                    Modifier.clip(RoundedCornerShape(50)).background(AmbrColors.CoveredTint).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_sparkles), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(12.dp))
                    Text("${d.readCount} of 5 read", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AmbrColors.CoveredInk)
                }
            }
            if (d.newProofs.isNotEmpty()) {
                if (d.readCount > 0) Column(Modifier.padding(horizontal = 2.dp)) {
                    Text("Check the details", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary)
                    Text(if (d.unsure.isEmpty()) "We read your receipt. Have a quick look." else "We read your receipt. Fix anything marked in amber.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary)
                } else Column(
                    Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.HoneyTint).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(R.drawable.ic_check), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(11.dp))
                        Text("Photo saved as proof", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AmbrColors.CoveredInk)
                    }
                    Text("Couldn't read this one", style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
                    Text("Just type the name. Everything else is optional and you can add it later.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
                }
                ProofStrip(d)
            }

            FormCard {
                LabeledField("Item name", d.name, { v -> vm.edit(Field.Name) { it.copy(name = v) } }, placeholder = "e.g. Walton fridge", unsure = Field.Name in d.unsure)
                LabeledField("Store", d.store, { v -> vm.edit(Field.Store) { it.copy(store = v) } }, placeholder = "Where you bought it", unsure = Field.Store in d.unsure)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LabeledField("Price (৳)", d.price, { v -> vm.edit(Field.Price) { it.copy(price = v.filter { c -> c.isDigit() || c == ',' }) } }, Modifier.weight(1f), placeholder = "0", unsure = Field.Price in d.unsure, numeric = true)
                    DateField("Bought on", d.purchaseDate, Field.Date in d.unsure, Modifier.weight(1f)) { pickDate = true }
                }
            }

            SectionLabel("Warranty length")
            val main = d.coverages.firstOrNull { it.label == "Full warranty" && it.type != CoverageType.FreeServices }
            ChipGroup(LENGTHS + 0, main?.months ?: 0, { if (it == 0) "None" else Format.warrantyLength(it) }) { m ->
                if (m == 0) main?.let { vm.replaceCoverage(it, null) } else vm.setMainMonths(m)
            }
            BrandHintCard(d, onUse = { months -> vm.setMainMonths(months) }, onAddPart = { label, months ->
                vm.addCoverage(CoverageEntity(itemId = d.id, type = CoverageType.Manufacturer, label = label, startDate = d.purchaseDate ?: LocalDate.now(), months = months))
            })
            d.coverages.filter { it !== main }.forEach { c ->
                CoverageRow(c) { coverageSheet = c }
            }
            Row(
                Modifier.clip(RoundedCornerShape(22.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.FieldBorder, RoundedCornerShape(22.dp))
                    .clickable { newCoverage = true }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(R.drawable.ic_plus), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(13.dp))
                Text("Add coverage", style = MaterialTheme.typography.titleSmall, color = AmbrColors.TextPrimary)
            }

            SectionLabel("Where is it?")
            ChipGroup(Room.entries, d.room, { it.label }) { r -> vm.edit { it.copy(room = r) } }

            SectionLabel("What kind of thing?")
            val guessed = d.category ?: Brands.categoryFor(d.name)
            ChipGroup(Category.entries, guessed, { it.label }) { c -> vm.edit { it.copy(category = c) } }

            Row(
                Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).clickable { more = !more }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("More details", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                    Text("Brand, model, serial, service center, notes", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                }
                Icon(painterResource(if (more) R.drawable.ic_angle_small_down else R.drawable.ic_angle_small_right), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            AnimatedVisibility(more) {
                FormCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledField("Brand", d.brand, { v -> vm.edit(Field.Brand) { it.copy(brand = v) } }, Modifier.weight(1f), placeholder = d.hint?.brand ?: "Walton", unsure = Field.Brand in d.unsure)
                        LabeledField("Model", d.model, { v -> vm.edit(Field.Model) { it.copy(model = v) } }, Modifier.weight(1f), unsure = Field.Model in d.unsure)
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledField("Serial number", d.serial, { v -> vm.edit(Field.Serial) { it.copy(serial = v) } }, Modifier.weight(1f), placeholder = "On the box or back sticker", unsure = Field.Serial in d.unsure)
                        if (onScanSerial != null) Box(
                            Modifier.padding(bottom = if (Field.Serial in d.unsure) 22.dp else 0.dp).size(56.dp).clip(RoundedCornerShape(16.dp)).background(AmbrColors.Navy).clickable(onClick = onScanSerial),
                            contentAlignment = Alignment.Center,
                        ) { Icon(painterResource(R.drawable.ic_barcode_read), "Scan barcode", tint = AmbrColors.Card, modifier = Modifier.size(20.dp)) }
                    }
                    LabeledField("Service center", d.serviceName, { v -> vm.edit { it.copy(serviceName = v) } }, placeholder = d.hint?.serviceName ?: "Name")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledField("Phone", d.servicePhone, { v -> vm.edit { it.copy(servicePhone = v) } }, Modifier.weight(1f), numeric = true)
                        LabeledField("Area", d.serviceAddress, { v -> vm.edit { it.copy(serviceAddress = v) } }, Modifier.weight(1f))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Notes", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
                        OutlinedTextField(
                            d.notes, { v -> vm.edit { it.copy(notes = v) } }, Modifier.fillMaxWidth().height(110.dp), shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = AmbrColors.Tile, focusedContainerColor = AmbrColors.Card, unfocusedBorderColor = AmbrColors.FieldBorder, focusedBorderColor = AmbrColors.Navy),
                        )
                    }
                }
            }
            if (onDelete != null) Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onDelete).padding(14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_trash), null, tint = AmbrColors.Danger, modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(8.dp))
                Text("Delete this item", style = MaterialTheme.typography.titleMedium, color = AmbrColors.Danger)
            }
        }
        Box(Modifier.fillMaxWidth().background(AmbrColors.Ground).padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding()) {
            AmbrButton(if (vm.isNew) "Save item" else "Save changes", {
                vm.save { r ->
                    when (r) {
                        is app.ambr.ui.state.SaveResult.Saved -> onSaved(r.id)
                        app.ambr.ui.state.SaveResult.LimitReached -> onLimit()
                    }
                }
            }, icon = R.drawable.ic_check, enabled = d.canSave)
        }
    }

    if (pickDate) DatePick(d.purchaseDate, { pickDate = false }) { vm.setDate(it); pickDate = false }
    if (newCoverage || coverageSheet != null) CoverageSheet(
        existing = coverageSheet,
        start = d.purchaseDate ?: LocalDate.now(),
        onDismiss = { newCoverage = false; coverageSheet = null },
        onRemove = coverageSheet?.let { c -> { vm.replaceCoverage(c, null); coverageSheet = null } },
    ) { c ->
        coverageSheet?.let { vm.replaceCoverage(it, c) } ?: vm.addCoverage(c.copy(itemId = d.id))
        newCoverage = false; coverageSheet = null
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary, modifier = Modifier.padding(top = 6.dp, start = 4.dp))

@Composable
private fun ProofStrip(d: Draft) {
    val first = d.newProofs.first()
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(File(first.filePath), null, Modifier.size(56.dp, 72.dp).clip(RoundedCornerShape(12.dp)).background(AmbrColors.Tile), contentScale = ContentScale.Crop)
        Column(Modifier.weight(1f)) {
            val pages = d.newProofs.size
            Text("${first.kind.label} · $pages ${if (pages == 1) "page" else "pages"}", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
            Text(listOfNotNull(d.store.ifBlank { null }, d.purchaseDate?.let(Format::date)).joinToString(", ").ifBlank { "Saved with this item" }, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
        }
    }
}

@Composable
private fun DateField(label: String, date: LocalDate?, unsure: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
        Row(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                .background(if (unsure) androidx.compose.ui.graphics.Color(0xFFFFF8EA) else AmbrColors.Tile)
                .border(1.dp, if (unsure) AmbrColors.Honey else AmbrColors.FieldBorder, RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(date?.let(Format::date) ?: "Pick a date", style = MaterialTheme.typography.bodyLarge, color = if (date == null) AmbrColors.TextPlaceholder else AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_calendar), null, tint = AmbrColors.TextMuted, modifier = Modifier.size(16.dp))
        }
        if (unsure) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(painterResource(R.drawable.ic_info), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(12.dp))
            Text("Check this", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AmbrColors.HoneyInk)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePick(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({ state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss() }) {
                Text("Done", color = AmbrColors.HoneyInk, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = AmbrColors.TextSecondary) } },
    ) { DatePicker(state) }
}

@Composable
private fun BrandHintCard(d: Draft, onUse: (Int) -> Unit, onAddPart: (String, Int) -> Unit) {
    val hint = d.hint ?: return
    val cat = d.category ?: Brands.categoryFor(d.name) ?: return
    val months = hint.usualMonths[cat] ?: return
    val part = hint.partCover[cat]
    val hasPart = part != null && d.coverages.any { it.label == part.first }
    val noun = d.name.ifBlank { cat.label }.split(' ').lastOrNull()?.lowercase() ?: "item"
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AmbrColors.HoneyTint).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_bulb), null, tint = AmbrColors.HoneyInk, modifier = Modifier.padding(top = 2.dp).size(16.dp))
            val partText = part?.let { " and ${Format.warrantyLength(it.second)} on the ${it.first.lowercase()}" } ?: ""
            Text("${hint.brand} ${noun}s usually have ${Format.warrantyLength(months)} full warranty$partText. Check your warranty card.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val main = d.coverages.firstOrNull { it.label == "Full warranty" }
            if (main?.months != months) HintButton("Use ${Format.warrantyLength(months)}") { onUse(months) }
            if (part != null && !hasPart) HintButton("Add ${part.first.lowercase()} · ${Format.warrantyLength(part.second)}") { onAddPart(part.first, part.second) }
        }
    }
}

@Composable
private fun HintButton(text: String, onClick: () -> Unit) {
    Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(AmbrColors.Card).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(R.drawable.ic_plus), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = AmbrColors.TextPrimary)
    }
}

@Composable
private fun CoverageRow(c: CoverageEntity, onEdit: () -> Unit) {
    val end = Warranty.end(c)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(18.dp)).clickable(onClick = onEdit).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            val title = if (c.servicesTotal != null) "${c.label} · ${c.servicesTotal} visits" else "${c.label} · ${Format.warrantyLength(c.months)}"
            Text(title, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
            val sub = when {
                c.servicesTotal != null -> "${c.servicesUsed} used"
                end != null -> "${c.type.label} · until ${Format.date(end)}"
                else -> c.type.label
            }
            Text(sub, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
        }
        Icon(painterResource(R.drawable.ic_pencil), "Edit coverage", tint = AmbrColors.TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverageSheet(existing: CoverageEntity?, start: LocalDate, onDismiss: () -> Unit, onRemove: (() -> Unit)?, onSave: (CoverageEntity) -> Unit) {
    var type by remember { mutableStateOf(existing?.type ?: CoverageType.Manufacturer) }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var months by remember { mutableStateOf(existing?.months?.takeIf { it > 0 } ?: 12) }
    var visits by remember { mutableStateOf(existing?.servicesTotal ?: 4) }
    var used by remember { mutableStateOf(existing?.servicesUsed ?: 0) }
    val free = type == CoverageType.FreeServices
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AmbrColors.Card) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (existing == null) "Add coverage" else "Edit coverage", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary)
            Text("Type", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
            ChipGroup(CoverageType.entries, type, { it.label }) { type = it }
            LabeledField(if (free) "What's included" else "What it covers", label, { label = it }, placeholder = if (free) "Free servicing" else "Compressor, panel, battery…")
            if (free) {
                Text("Free visits", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
                ChipGroup(listOf(1, 2, 3, 4, 6, 12), visits, { "$it" }) { visits = it; used = used.coerceAtMost(it) }
                Text("Already used", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
                ChipGroup((0..visits).toList(), used, { "$it" }) { used = it }
            } else {
                Text("Length", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
                ChipGroup(listOf(3, 6, 12, 24, 36, 60, 120), months, { Format.warrantyLength(it) }) { months = it }
            }
            val summary = if (free) "$visits free ${if (visits == 1) "visit" else "visits"}, ${visits - used} left"
            else "${label.ifBlank { "Covered" }} until ${Format.date(start.plusMonths(months.toLong()))}"
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AmbrColors.CoveredTint).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(painterResource(R.drawable.ic_shield_check), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(16.dp))
                Text(summary, style = MaterialTheme.typography.titleSmall, color = AmbrColors.CoveredInk)
            }
            AmbrButton(if (existing == null) "Add coverage" else "Save coverage", {
                onSave(
                    (existing ?: CoverageEntity(itemId = 0, startDate = start, months = 0)).copy(
                        type = type,
                        label = label.trim().ifBlank { if (free) "Free servicing" else type.label + " warranty" },
                        months = if (free) 0 else months,
                        servicesTotal = if (free) visits else null,
                        servicesUsed = if (free) used else 0,
                    ),
                )
            })
            if (onRemove != null) AmbrButton("Remove coverage", onRemove, style = ButtonStyle.Quiet, height = 48.dp)
        }
    }
}

