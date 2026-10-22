package app.ambr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AmbrColorScheme = lightColorScheme(
    primary = AmbrColors.Honey,
    onPrimary = AmbrColors.TextPrimary,
    primaryContainer = AmbrColors.HoneyTint,
    onPrimaryContainer = AmbrColors.HoneyInk,
    secondary = AmbrColors.Navy,
    onSecondary = AmbrColors.Card,
    background = AmbrColors.Ground,
    onBackground = AmbrColors.TextPrimary,
    surface = AmbrColors.Card,
    onSurface = AmbrColors.TextPrimary,
    surfaceVariant = AmbrColors.Tile,
    onSurfaceVariant = AmbrColors.TextMuted,
    outline = AmbrColors.FieldBorder,
    outlineVariant = AmbrColors.Ring,
    error = AmbrColors.Danger,
)

private val AmbrShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AmbrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AmbrColorScheme,
        typography = AmbrTypography,
        shapes = AmbrShapes,
        content = content,
    )
}
