package app.ambr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ambr.domain.Format
import app.ambr.domain.StatusKind
import app.ambr.ui.state.ItemUi
import app.ambr.ui.theme.AmbrColors

/** "Until 14 Mar 2027", "Ends 30 Oct", "Ended 3 Jan 2026". */
fun ItemUi.endLine(): String {
    val until = status.until ?: return when (status.kind) {
        StatusKind.NoCoverage -> "No warranty added"
        StatusKind.Expired -> "Warranty ended"
        else -> "Free services left"
    }
    return when (status.kind) {
        StatusKind.EndsSoon -> "Ends ${Format.dateShort(until)}"
        StatusKind.Expired -> "Ended ${Format.date(until)}"
        else -> "Until ${Format.date(until)}"
    }
}

/** The square card used in Vault, What broke? and Search. */
@Composable
fun ItemGridTile(ui: ItemUi, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String = ui.endLine(), artHeight: Dp = 96.dp) {
    val v = ui.item.category.visual(ui.item.name)
    Column(
        modifier.clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).clickable(onClick = onClick).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(artHeight).clip(RoundedCornerShape(18.dp)).background(v.tone.tile)) {
            Icon(painterResource(v.icon), null, tint = v.tone.ink, modifier = Modifier.size(40.dp).align(Alignment.Center))
            StatusPill(ui.status.kind, small = true, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        }
        Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp)) {
            Text(ui.item.name, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted, maxLines = 1)
        }
    }
}

@Composable
fun ItemGrid(items: List<ItemUi>, onOpen: (Long) -> Unit, subtitle: (ItemUi) -> String = { it.endLine() }) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { ItemGridTile(it, { onOpen(it.id) }, Modifier.weight(1f), subtitle(it)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** A one-line row for lists like Archive and Search results. */
@Composable
fun ItemRow(ui: ItemUi, subtitle: String, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    val v = ui.item.category.visual(ui.item.name)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(v.icon, v.tone.tile, v.tone.ink, size = 44.dp, iconSize = 20.dp, radius = 14.dp)
        Column(Modifier.weight(1f)) {
            Text(ui.item.name, style = MaterialTheme.typography.titleMedium, color = AmbrColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AmbrColors.TextMuted, maxLines = 1)
        }
        trailing?.invoke() ?: StatusPill(ui.status.kind, small = true)
    }
}

@Composable
fun Divided(content: List<@Composable () -> Unit>) {
    Column(Modifier.fillMaxWidth().clip(CardShape).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, CardShape).padding(vertical = 4.dp)) {
        content.forEachIndexed { i, c ->
            if (i > 0) Box(Modifier.padding(start = 70.dp, end = 14.dp).fillMaxWidth().height(1.dp).background(AmbrColors.Divider))
            c()
        }
    }
}
