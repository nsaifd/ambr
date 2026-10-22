package app.ambr.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.domain.Format
import app.ambr.domain.StatusKind
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.components.ItemGrid
import app.ambr.ui.state.ItemUi
import app.ambr.ui.state.ItemsViewModel
import app.ambr.ui.theme.AmbrColors

enum class VaultFilter(val label: String) { All("All"), Covered("Covered"), EndsSoon("Ends soon"), Expired("Expired") }

enum class VaultSort(val label: String) { EndingSoonest("Ending soonest"), Newest("Newest first"), Name("Name, A to Z"), Price("Price, highest first") }

private fun ItemUi.matches(f: VaultFilter) = when (f) {
    VaultFilter.All -> true
    VaultFilter.Covered -> status.kind == StatusKind.Covered
    VaultFilter.EndsSoon -> status.kind == StatusKind.EndsSoon
    VaultFilter.Expired -> status.kind == StatusKind.Expired || status.kind == StatusKind.NoCoverage
}

fun List<ItemUi>.sortedBy(sort: VaultSort): List<ItemUi> = when (sort) {
    // Active items by days left, then expired ones, most recently ended first.
    VaultSort.EndingSoonest -> sortedWith(compareBy<ItemUi>({ it.status.kind == StatusKind.Expired || it.status.kind == StatusKind.NoCoverage }, { it.status.daysLeft ?: Long.MAX_VALUE }))
    VaultSort.Newest -> sortedByDescending { it.item.createdAt }
    VaultSort.Name -> sortedBy { it.item.name.lowercase() }
    VaultSort.Price -> sortedByDescending { it.item.priceTaka ?: 0 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(vm: ItemsViewModel, onOpenItem: (Long) -> Unit, onSearch: () -> Unit, onArchive: () -> Unit) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    val archived by vm.archived.collectAsStateWithLifecycle()
    var byRoom by rememberSaveable { mutableStateOf(true) }
    var filter by rememberSaveable { mutableStateOf(VaultFilter.All) }
    var sort by rememberSaveable { mutableStateOf(VaultSort.EndingSoonest) }
    var sortOpen by rememberSaveable { mutableStateOf(false) }

    val all = overview.items
    val shown = all.filter { it.matches(filter) }.sortedBy(sort)
    val groups = if (byRoom) shown.groupBy { it.item.room.label } else shown.groupBy { it.item.category.label }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 2.dp)) {
                    Text("Vault", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary)
                    Text("${all.size} items · ${Format.taka(all.sumOf { it.item.priceTaka ?: 0 })}", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleIconButton(R.drawable.ic_search, "Search", onSearch)
                    CircleIconButton(R.drawable.ic_sort_amount_down, "Sort: ${sort.label}", { sortOpen = true })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AmbrColors.Ring).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Segment("By room", byRoom, Modifier.weight(1f)) { byRoom = true }
                Segment("By category", !byRoom, Modifier.weight(1f)) { byRoom = false }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VaultFilter.entries.forEach { f -> FilterChip(f.label, all.count { it.matches(f) }, f == filter) { filter = f } }
            }
        }
        groups.forEach { (name, list) ->
            item(key = "g-$name") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
                        Text(if (list.size == 1) "1 item" else "${list.size} items", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted)
                    }
                    ItemGrid(list, onOpenItem)
                }
            }
        }
        if (overview.loaded && shown.isEmpty()) item {
            Row(
                Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(AmbrColors.CoveredTint), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(20.dp))
                }
                Text(
                    when (filter) {
                        VaultFilter.Expired -> "No expired items. Nice."
                        VaultFilter.EndsSoon -> "All good. Nothing ending soon."
                        VaultFilter.Covered -> "Nothing covered right now."
                        VaultFilter.All -> "Nothing saved yet. Tap + to add your first item."
                    },
                    style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary,
                )
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).clickable(onClick = onArchive).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AmbrColors.Ground), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_archive), null, tint = AmbrColors.TextSecondary, modifier = Modifier.size(17.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Archive", style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                    Text("Sold or given away · ${archived.size}", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                }
                Icon(painterResource(R.drawable.ic_angle_small_right), null, tint = AmbrColors.TextMuted, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (sortOpen) {
        ModalBottomSheet(onDismissRequest = { sortOpen = false }, containerColor = AmbrColors.Card) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sort by", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                VaultSort.entries.forEach { s ->
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (s == sort) AmbrColors.HoneyTint else AmbrColors.Card)
                            .selectable(s == sort, role = Role.RadioButton) { sort = s; sortOpen = false }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.label, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
                        if (s == sort) Icon(painterResource(R.drawable.ic_check), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Segment(text: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(42.dp).clip(RoundedCornerShape(14.dp)).background(if (on) AmbrColors.Card else AmbrColors.Ring)
            .selectable(on, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, color = if (on) AmbrColors.TextPrimary else AmbrColors.TextMuted)
    }
}

@Composable
private fun FilterChip(label: String, count: Int, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (on) AmbrColors.Navy else AmbrColors.Card)
            .border(1.dp, if (on) AmbrColors.Navy else AmbrColors.FieldBorder, RoundedCornerShape(20.dp))
            .selectable(on, role = Role.RadioButton, onClick = onClick).padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (on) AmbrColors.Card else AmbrColors.TextPrimary)
        Box(
            Modifier.widthIn(min = 24.dp).height(24.dp).clip(RoundedCornerShape(12.dp)).background(if (on) AmbrColors.Honey else AmbrColors.Ground).padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (on) AmbrColors.TextPrimary else AmbrColors.TextSecondary)
        }
    }
}
