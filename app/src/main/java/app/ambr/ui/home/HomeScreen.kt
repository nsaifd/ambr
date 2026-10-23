package app.ambr.ui.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.BuildConfig
import app.ambr.R
import app.ambr.data.Category
import app.ambr.domain.Format
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CardShape
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.components.IconTile
import app.ambr.ui.components.InfoBanner
import app.ambr.ui.components.SectionHeader
import app.ambr.ui.components.StatusPill
import app.ambr.ui.components.visual
import app.ambr.ui.state.ItemUi
import app.ambr.ui.state.ItemsViewModel
import app.ambr.ui.state.Overview
import app.ambr.ui.theme.AmbrColors
import java.time.LocalTime

@Composable
fun HomeScreen(
    vm: ItemsViewModel,
    onOpenItem: (Long) -> Unit,
    onSomethingBroke: () -> Unit,
    onSearch: () -> Unit,
    onSeeAll: () -> Unit,
    onAdd: () -> Unit,
    onNotifications: () -> Unit,
) {
    val overview by vm.overview.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Header(onNotifications) }
        if (!overview.loaded) return@LazyColumn
        if (overview.items.isEmpty()) {
            item { EmptyHome(onAdd = onAdd, onSamples = if (BuildConfig.DEBUG) ({ vm.loadSamples(); Unit }) else null) }
            return@LazyColumn
        }
        item { Greeting(overview) }
        item { SearchBar(onSearch) }
        item { ValueCard(overview) }
        item { ActionTiles(onSomethingBroke) }
        item { SectionHeader("Needs attention", if (overview.endingSoon.isNotEmpty()) "See all" else null, onSeeAll) }
        item {
            if (overview.endingSoon.isEmpty()) AllClearCard()
            else AttentionList(overview.endingSoon, onOpenItem)
        }
        item { SectionHeader("Recently added", "Vault", onSeeAll) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(overview.recent, key = { it.id }) { RecentCard(it, onOpenItem) }
            }
        }
    }
}

@Composable
private fun Header(onNotifications: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(AmbrColors.Card)
                .border(1.dp, AmbrColors.Ring, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp)
                .semantics { contentDescription = "Ambr" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_ambr_lockup), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.height(20.dp).width(82.dp))
        }
        Spacer(Modifier.weight(1f))
        CircleIconButton(R.drawable.ic_bell, "Notifications", onNotifications)
    }
}

@Composable
private fun Greeting(o: Overview) {
    val hour = LocalTime.now().hour
    val hello = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
    val title = when (val n = o.endingSoon.size) { 0 -> "Everything is covered"; 1 -> "1 item needs a look"; else -> "$n items need a look" }
    Column(Modifier.padding(horizontal = 2.dp)) {
        Text(hello, style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextMuted)
        Text(title, style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary)
    }
}

@Composable
private fun SearchBar(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp)).background(AmbrColors.Card)
            .border(1.dp, AmbrColors.Ring, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_search), null, tint = AmbrColors.TextMuted, modifier = Modifier.size(18.dp))
        Text("Search fridge, phone, AC…", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextMuted)
    }
}

@Composable
private fun ValueCard(o: Overview) {
    val total = o.items.size.coerceAtLeast(1).toFloat()
    val parts = listOf(o.covered / total to Color(0xFF5FD39A), o.endingSoon.size / total to AmbrColors.HoneyOnNavy, o.expired / total to Color(0xFF4A5674))
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(AmbrColors.Navy).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_shield_check), null, tint = AmbrColors.HoneyOnNavy, modifier = Modifier.size(16.dp))
            Text("Value protected", style = MaterialTheme.typography.labelMedium, color = AmbrColors.OnDarkMuted)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Format.taka(o.totalValue), style = MaterialTheme.typography.displayLarge, color = AmbrColors.Card, modifier = Modifier.weight(1f))
            Text("${o.items.size} items", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.OnDarkMuted)
        }
        Canvas(Modifier.fillMaxWidth().height(10.dp).semantics { contentDescription = "${o.covered} covered, ${o.endingSoon.size} ending soon, ${o.expired} expired" }) {
            val gap = 4.dp.toPx()
            val visible = parts.filter { it.first > 0f }
            val usable = size.width - gap * (visible.size - 1).coerceAtLeast(0)
            var x = 0f
            visible.forEach { (share, color) ->
                val w = usable * share
                drawRoundRect(color, Offset(x, 0f), Size(w, size.height), CornerRadius(size.height / 2))
                x += w + gap
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend("${o.covered} covered", Color(0xFF5FD39A))
            Legend("${o.endingSoon.size} ending soon", AmbrColors.HoneyOnNavy)
            Legend("${o.expired} expired", Color(0xFF8D97B3))
        }
    }
}

@Composable
private fun Legend(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD5DBEA))
    }
}

@Composable
private fun ActionTiles(onSomethingBroke: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(CardShape).background(AmbrColors.Honey)
            .clickable(onClick = onSomethingBroke).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(AmbrColors.Navy), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_wrench_simple), null, tint = AmbrColors.HoneyOnNavy, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Something broke?", style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
                Text("Find its proof in two taps", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4A3300))
            }
            Icon(painterResource(R.drawable.ic_angle_small_right), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AttentionList(items: List<ItemUi>, onOpen: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(vertical = 4.dp)) {
        items.forEachIndexed { i, ui ->
            if (i > 0) Box(Modifier.padding(start = 74.dp, end = 14.dp).fillMaxWidth().height(1.dp).background(AmbrColors.Divider))
            val v = ui.item.category.visual(ui.item.name)
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(ui.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconTile(v.icon, v.tone.tile, v.tone.ink)
                Column(Modifier.weight(1f)) {
                    Text(ui.item.name, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary)
                    Text("${ui.item.room.label} · ends ${ui.status.until?.let { Format.dateShort(it) } ?: ""}", style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted)
                }
                Column(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(AmbrColors.HoneyTint),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Text("${ui.status.daysLeft ?: 0}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AmbrColors.HoneyInk)
                    Text("DAYS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmbrColors.HoneyInk)
                }
            }
        }
    }
}

@Composable
private fun AllClearCard() {
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(AmbrColors.CoveredTint), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_check), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(22.dp))
        }
        Column {
            Text("All good", style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary)
            Text("Nothing ends in the next 30 days. We'll tell you when something does.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
        }
    }
}

@Composable
private fun RecentCard(ui: ItemUi, onOpen: (Long) -> Unit) {
    val v = ui.item.category.visual(ui.item.name)
    Column(
        Modifier.width(150.dp).clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape)
            .clickable { onOpen(ui.id) }.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(18.dp)).background(v.tone.tile), contentAlignment = Alignment.Center) {
            Icon(painterResource(v.icon), null, tint = v.tone.ink, modifier = Modifier.size(40.dp))
        }
        Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ui.item.name, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, maxLines = 1)
            StatusPill(ui.status.kind, small = true)
        }
    }
}

@Composable
private fun EmptyHome(onAdd: () -> Unit, onSamples: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(28.dp)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Nothing saved yet", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary)
            Text("Start with the most expensive thing you own. It takes 15 seconds.", style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextSecondary)
        }
        Text("START WITH", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted, modifier = Modifier.padding(start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Fridge" to Category.Kitchen, "TV" to Category.TvAudio, "Phone" to Category.Phones, "AC" to Category.Cooling).forEach { (name, cat) ->
                val v = cat.visual(name)
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(20.dp))
                        .clickable(onClick = onAdd).padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconTile(v.icon, v.tone.tile, v.tone.ink, size = 44.dp, radius = 14.dp)
                    Text(name, style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextPrimary)
                }
            }
        }
        InfoBanner("Thermal receipts fade within months. Snap them soon after you buy.")
        if (onSamples != null) AmbrButton("Load sample items (debug)", onSamples, style = ButtonStyle.Quiet, height = 48.dp)
    }
}
