package app.ambr.ui.repair

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.data.Category
import app.ambr.data.RepairEntity
import app.ambr.data.RepairOutcome
import app.ambr.domain.Format
import app.ambr.domain.StatusKind
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.components.LabeledField
import app.ambr.ui.components.visual
import app.ambr.ui.edit.DatePick
import app.ambr.ui.state.ItemViewModel
import app.ambr.ui.theme.AmbrColors
import java.time.LocalDate

private fun problemsFor(c: Category) = when (c) {
    Category.Cooling -> listOf("Not cooling", "Noise", "Water leak", "Won't start", "Remote")
    Category.Kitchen -> listOf("Not cooling", "Noise", "Leak", "Door seal", "Won't start")
    Category.TvAudio -> listOf("No picture", "No sound", "Lines on screen", "Won't start", "Remote")
    Category.Phones -> listOf("Screen", "Battery", "Charging", "Camera", "Software")
    Category.Computers -> listOf("Won't start", "Screen", "Battery", "Keyboard", "Slow")
    Category.Laundry -> listOf("Won't spin", "Leak", "Noise", "Won't start", "Error code")
    Category.Vehicles -> listOf("Engine", "Brakes", "Electrical", "Free service", "Tyres")
    else -> listOf("Not working", "Noise", "Broken part", "Leak")
} + "Other"

private data class OutcomeStyle(val icon: Int, val ink: Color, val tint: Color)

private fun RepairOutcome.style() = when (this) {
    RepairOutcome.Fixed -> OutcomeStyle(R.drawable.ic_check, AmbrColors.CoveredInk, AmbrColors.CoveredTint)
    RepairOutcome.Replaced -> OutcomeStyle(R.drawable.ic_refresh, Color(0xFF2B5F8E), Color(0xFFE6F0FB))
    RepairOutcome.Rejected -> OutcomeStyle(R.drawable.ic_cross_small, AmbrColors.Danger, AmbrColors.DangerTint)
    RepairOutcome.Pending -> OutcomeStyle(R.drawable.ic_clock_three, AmbrColors.EndsSoonInk, AmbrColors.EndsSoonTint)
}

@Composable
fun LogRepairScreen(vm: ItemViewModel, onBack: () -> Unit) {
    val ui by vm.item.collectAsStateWithLifecycle()
    val current = ui ?: return
    val i = current.item
    val v = i.category.visual(i.name)
    val covered = current.status.kind == StatusKind.Covered || current.status.kind == StatusKind.EndsSoon
    val freeLeft = current.status.coverages.firstOrNull { (it.servicesLeft ?: 0) > 0 }
    var problems by rememberSaveable { mutableStateOf(setOf<String>()) }
    var outcome by rememberSaveable { mutableStateOf(RepairOutcome.Fixed) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var cost by rememberSaveable { mutableStateOf("0") }
    var notes by rememberSaveable { mutableStateOf("") }
    var countService by rememberSaveable { mutableStateOf(freeLeft != null) }
    var pickDate by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircleIconButton(R.drawable.ic_angle_small_left, "Back", onBack)
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(v.tone.tile).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(painterResource(v.icon), null, tint = v.tone.ink, modifier = Modifier.size(12.dp))
                    Text(i.name, style = MaterialTheme.typography.labelMedium, color = v.tone.ink)
                }
            }
            Column(Modifier.padding(horizontal = 2.dp)) {
                Text("How did it go?", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary)
                Text("It takes 20 seconds and builds a service history buyers trust.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary)
            }
            Label("What went wrong?")
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                problemsFor(i.category).forEach { p ->
                    val on = p in problems
                    Box(
                        Modifier.height(42.dp).clip(RoundedCornerShape(21.dp)).background(if (on) AmbrColors.Navy else AmbrColors.Tile)
                            .border(1.dp, if (on) AmbrColors.Navy else AmbrColors.FieldBorder, RoundedCornerShape(21.dp))
                            .clickable(role = Role.Checkbox) { problems = if (on) problems - p else problems + p }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(p, style = MaterialTheme.typography.titleSmall, color = if (on) AmbrColors.Card else AmbrColors.TextPrimary) }
                }
            }
            Label("Outcome")
            RepairOutcome.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { o ->
                        val on = o == outcome
                        val st = o.style()
                        Row(
                            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(18.dp)).background(if (on) AmbrColors.Card else AmbrColors.Tile)
                                .border(2.dp, if (on) st.ink else Color.Transparent, RoundedCornerShape(18.dp))
                                .selectable(on, role = Role.RadioButton) { outcome = o }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(if (on) st.ink else st.tint), contentAlignment = Alignment.Center) {
                                Icon(painterResource(st.icon), null, tint = if (on) Color.White else st.ink, modifier = Modifier.size(15.dp))
                            }
                            Text(o.label, style = MaterialTheme.typography.labelLarge, color = AmbrColors.TextPrimary)
                        }
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Date", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
                        Box(
                            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(AmbrColors.Tile).border(1.dp, AmbrColors.FieldBorder, RoundedCornerShape(16.dp))
                                .clickable { pickDate = true }.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { Text(if (date == LocalDate.now()) "Today, ${Format.dateShort(date)}" else Format.date(date), style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextPrimary) }
                    }
                    LabeledField("Cost (৳)", cost, { cost = it.filter(Char::isDigit) }, Modifier.weight(1f), numeric = true)
                }
                if (covered && (cost.toLongOrNull() ?: 0) == 0L) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(painterResource(R.drawable.ic_shield_check), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(13.dp))
                    Text("Free, because it was under warranty", style = MaterialTheme.typography.labelMedium, color = AmbrColors.CoveredInk)
                }
                if (freeLeft != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Count as a free service", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                        Text("${freeLeft.servicesLeft} of ${freeLeft.coverage.servicesTotal} left", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                    }
                    Switch(countService, { countService = it }, colors = SwitchDefaults.colors(checkedTrackColor = AmbrColors.Navy, checkedThumbColor = AmbrColors.Card))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Notes", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
                    OutlinedTextField(
                        notes, { notes = it }, Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Who came, what they changed, job card number", color = AmbrColors.TextPlaceholder) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = AmbrColors.Tile, focusedContainerColor = AmbrColors.Card, unfocusedBorderColor = AmbrColors.FieldBorder, focusedBorderColor = AmbrColors.Navy),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding()) {
            AmbrButton("Save to history", {
                vm.logRepair(
                    RepairEntity(itemId = i.id, date = date, problem = problems.joinToString(", ").ifBlank { "Repair" }, outcome = outcome, costTaka = cost.toLongOrNull() ?: 0, notes = notes.trim().ifBlank { null }),
                    countService && freeLeft != null,
                )
                onBack()
            }, icon = R.drawable.ic_check)
        }
    }
    if (pickDate) DatePick(date, { pickDate = false }) { date = it; pickDate = false }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
