package app.ambr.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ambr.R
import app.ambr.domain.StatusKind
import app.ambr.ui.theme.AmbrColors

val CardShape = RoundedCornerShape(24.dp)

@Composable
fun AmbrCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AmbrColors.Card)
            .border(1.dp, AmbrColors.Ring, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun StatusPill(kind: StatusKind, modifier: Modifier = Modifier, label: String? = null, small: Boolean = false) {
    val v = kind.visual()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(v.tint)
            .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 3.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(painterResource(v.icon), contentDescription = null, tint = v.ink, modifier = Modifier.size(if (small) 10.dp else 11.dp))
        Text(label ?: v.label, color = v.ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun IconTile(@DrawableRes icon: Int, tile: Color, ink: Color, size: Dp = 48.dp, iconSize: Dp = 22.dp, radius: Dp = 16.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(radius)).background(tile),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun CircleIconButton(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, background: Color = AmbrColors.Card, tint: Color = AmbrColors.TextPrimary) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, if (background == AmbrColors.Card) AmbrColors.Ring else Color.Transparent, CircleShape)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TopBar(title: String?, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(R.drawable.ic_angle_small_left, "Back", onBack)
        Spacer(Modifier.width(12.dp))
        if (title != null) Text(title, style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
        else Spacer(Modifier.weight(1f))
        actions()
    }
}

enum class ButtonStyle { Primary, Strong, Quiet, Danger }

@Composable
fun AmbrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    @DrawableRes icon: Int? = null,
    height: Dp = 56.dp,
    enabled: Boolean = true,
) {
    val (bg, fg) = when (style) {
        ButtonStyle.Primary -> AmbrColors.Honey to AmbrColors.TextPrimary
        ButtonStyle.Strong -> AmbrColors.Navy to AmbrColors.Card
        ButtonStyle.Quiet -> AmbrColors.Ground to AmbrColors.TextPrimary
        ButtonStyle.Danger -> AmbrColors.Danger to AmbrColors.Card
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(height / 2),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg, disabledContainerColor = bg.copy(alpha = 0.35f), disabledContentColor = fg.copy(alpha = 0.6f)),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = MaterialTheme.typography.labelMedium, color = AmbrColors.HoneyInk,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(8.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun <T> ChipGroup(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (on) AmbrColors.Navy else AmbrColors.Tile)
                    .border(BorderStroke(1.dp, if (on) AmbrColors.Navy else AmbrColors.FieldBorder), RoundedCornerShape(22.dp))
                    .clickable(role = Role.RadioButton) { onSelect(o) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), color = if (on) AmbrColors.Card else AmbrColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    unsure: Boolean = false,
    numeric: Boolean = false,
    phone: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = placeholder?.let { { Text(it, color = AmbrColors.TextPlaceholder) } },
            keyboardOptions = when {
                phone -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                numeric -> KeyboardOptions(keyboardType = KeyboardType.Number)
                else -> KeyboardOptions.Default
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = if (unsure) Color(0xFFFFF8EA) else AmbrColors.Tile,
                focusedContainerColor = AmbrColors.Card,
                unfocusedBorderColor = if (unsure) AmbrColors.Honey else AmbrColors.FieldBorder,
                focusedBorderColor = AmbrColors.Navy,
            ),
        )
        if (unsure) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(painterResource(R.drawable.ic_info), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(12.dp))
                Text("Check this", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AmbrColors.HoneyInk)
            }
        }
    }
}

@Composable
fun InfoBanner(text: String, @DrawableRes icon: Int = R.drawable.ic_info, tint: Color = AmbrColors.HoneyTint, ink: Color = AmbrColors.HoneyInk) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(tint).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(icon), null, tint = ink, modifier = Modifier.padding(top = 2.dp).size(15.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AmbrColors.TextPrimary)
    }
}

@Composable
fun KeyValueRow(key: String, value: String, last: Boolean = false, trailing: @Composable (() -> Unit)? = null) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(key, style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextMuted, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = AmbrColors.TextPrimary)
            trailing?.invoke()
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(AmbrColors.Divider))
    }
}
