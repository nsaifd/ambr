package app.ambr.ui.theme

import androidx.compose.ui.graphics.Color

// Values come from design/tokens.json.
object AmbrColors {
    val Honey = Color(0xFFEF9426)
    val HoneyPressed = Color(0xFFD27F1C)
    val HoneyTint = Color(0xFFFFF4DE)
    val HoneyInk = Color(0xFF7A4A00)
    val HoneyOnNavy = Color(0xFFFFC24D)

    val Navy = Color(0xFF1F2A44)
    val NavyPressed = Color(0xFF141C30)
    val NavyRaised = Color(0xFF2B3756)
    val NavyLogo = Color(0xFF1D263B)

    val Ground = Color(0xFFF4F5F8)
    val Card = Color(0xFFFFFFFF)
    val Tile = Color(0xFFF7F8FA)
    val Ring = Color(0xFFE8EAF0)
    val Divider = Color(0xFFEEF0F4)
    val FieldBorder = Color(0xFFE3E6EC)
    val Control = Color(0xFFCDD2DC)

    val TextPrimary = Color(0xFF1D2433)
    val TextSecondary = Color(0xFF4A5163)
    val TextMuted = Color(0xFF646B7E)
    val TextPlaceholder = Color(0xFF9AA0AE)
    val OnDarkMuted = Color(0xFFB9C1D6)

    val CoveredFill = Color(0xFF1B7F4C)
    val CoveredInk = Color(0xFF17704A)
    val CoveredTint = Color(0xFFE4F4EA)
    val EndsSoonInk = Color(0xFF8F5000)
    val EndsSoonTint = Color(0xFFFFEFD3)
    val ExpiredInk = Color(0xFF5F6577)
    val ExpiredTint = Color(0xFFECEEF2)
    val Danger = Color(0xFFB42318)
    val DangerTint = Color(0xFFFEE4E2)
}

enum class CategoryTone(val tile: Color, val ink: Color) {
    Cooling(Color(0xFFE6F0FB), Color(0xFF2B5F8E)),
    Kitchen(Color(0xFFE2F4EE), Color(0xFF1F6E58)),
    TvAudio(Color(0xFFECE9FB), Color(0xFF5146A8)),
    Phones(Color(0xFFFDEBDD), Color(0xFF9A4A16)),
    Vehicles(Color(0xFFF5EEDB), Color(0xFF7A6420)),
    Laundry(Color(0xFFE3EFF7), Color(0xFF2F5F7A)),
    Computers(Color(0xFFE8EDF4), Color(0xFF3E5568)),
    Other(Color(0xFFECEEF2), Color(0xFF5F6577)),
}
