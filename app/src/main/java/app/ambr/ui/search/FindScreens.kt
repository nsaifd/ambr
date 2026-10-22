package app.ambr.ui.search

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.domain.Format
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.components.Divided
import app.ambr.ui.components.ItemGrid
import app.ambr.ui.components.ItemRow
import app.ambr.ui.components.TopBar
import app.ambr.ui.components.endLine
import app.ambr.ui.state.ItemUi
import app.ambr.ui.state.ItemsViewModel
import app.ambr.ui.theme.AmbrColors
import app.ambr.ui.vault.VaultSort
import app.ambr.ui.vault.sortedBy

/** Two taps to the claim pack: pick the thing that broke, by picture. */
@Composable
fun WhatBrokeScreen(vm: ItemsViewModel, onBack: () -> Unit, onSearch: () -> Unit, onPick: (Long) -> Unit, onAll: () -> Unit) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    // Things still covered come first: those are the ones worth claiming.
    val items = overview.items.sortedBy(VaultSort.EndingSoonest)
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(R.drawable.ic_angle_small_left, "Back", onBack)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(22.dp))
                        .clickable(onClick = onSearch).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_search), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(15.dp))
                    Text("Search instead", style = MaterialTheme.typography.titleSmall, color = AmbrColors.TextPrimary)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("What broke?", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary)
                Text("Tap it. We'll open the receipt, the serial number and the service center.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary)
            }
        }
        item { ItemGrid(items.take(12), onPick) { it.item.room.label } }
        if (items.size > 12) item { AmbrButton("Show all ${items.size} items", onAll, style = ButtonStyle.Quiet, height = 48.dp) }
    }
}

private fun ItemUi.matches(q: String): Boolean {
    val i = item
    val hay = listOfNotNull(i.name, i.brand, i.model, i.serial, i.store, i.notes, i.category.label, i.room.label) + details.proofs.mapNotNull { it.ocrText }
    return q.split(' ').filter { it.isNotBlank() }.all { word -> hay.any { it.contains(word, ignoreCase = true) } }
}

@Composable
fun SearchScreen(vm: ItemsViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val q = query.trim()
    val results = if (q.isEmpty()) emptyList() else overview.items.filter { it.matches(q) }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircleIconButton(R.drawable.ic_angle_small_left, "Back", onBack)
            Row(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Navy, RoundedCornerShape(24.dp)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(painterResource(R.drawable.ic_search), null, tint = AmbrColors.TextMuted, modifier = Modifier.size(16.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Name, brand, serial, shop…", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextPlaceholder)
                    BasicTextField(
                        query, { query = it }, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = AmbrColors.TextPrimary), cursorBrush = SolidColor(AmbrColors.Honey),
                    )
                }
                if (query.isNotEmpty()) Box(Modifier.size(28.dp).clip(CircleShape).background(AmbrColors.Ground).clickable { query = "" }, contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_cross_small), "Clear search", tint = AmbrColors.TextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
        when {
            q.isEmpty() -> {
                Text("Search looks inside receipts too, so a shop name or invoice number works.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted, modifier = Modifier.padding(horizontal = 4.dp))
            }
            results.isEmpty() -> Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Nothing matches \"$q\"", style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
                Text("Try the brand, or part of the serial number.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted)
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Text(if (results.size == 1) "1 result" else "${results.size} results", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    Divided(results.map { ui -> { ItemRow(ui, "${ui.item.room.label} · ${ui.endLine()}", { onOpen(ui.id) }) } })
                }
            }
        }
    }
}

@Composable
fun ArchiveScreen(vm: ItemsViewModel, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val archived by vm.archived.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TopBar("Archive", onBack) }
        item { Text("Things you sold or gave away. Their receipts stay here in case a buyer asks.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary, modifier = Modifier.padding(horizontal = 4.dp)) }
        if (archived.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_archive), null, tint = AmbrColors.Control, modifier = Modifier.size(40.dp))
                Text("Archive is empty", style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
                Text("When you sell something, mark it as sold from its page.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextMuted)
            }
        } else item {
            Divided(archived.map { ui ->
                {
                    val how = if (ui.item.archivedReason == app.ambr.data.ArchiveReason.Sold) "Sold" else "Given away"
                    ItemRow(ui, "$how ${ui.item.archivedOn?.let(Format::date) ?: ""}".trim(), { onOpen(ui.id) }) {
                        Text(
                            "Restore", style = MaterialTheme.typography.labelMedium, color = AmbrColors.HoneyInk,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(AmbrColors.HoneyTint).clickable { vm.unarchive(ui.item) }.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            })
        }
    }
}
