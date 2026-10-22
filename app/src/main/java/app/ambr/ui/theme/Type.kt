package app.ambr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.ambr.R

val PublicSans = FontFamily(
    Font(R.font.public_sans_regular, FontWeight.Normal),
    Font(R.font.public_sans_medium, FontWeight.Medium),
    Font(R.font.public_sans_semibold, FontWeight.SemiBold),
    Font(R.font.public_sans_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, tracking: Double = 0.0, lineHeight: Double? = null) = TextStyle(
    fontFamily = PublicSans,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = tracking.em,
    lineHeight = lineHeight?.let { (size * it).sp } ?: TextStyle.Default.lineHeight,
)

// Mapped from the type scale on the Foundations board.
val AmbrTypography = Typography(
    displayLarge = style(38, FontWeight.Bold, -0.03),
    headlineLarge = style(30, FontWeight.Bold, -0.025, 1.15),
    headlineSmall = style(22, FontWeight.Bold, -0.015, 1.2),
    titleLarge = style(18, FontWeight.Bold),
    titleMedium = style(15, FontWeight.SemiBold),
    bodyLarge = style(16, FontWeight.Normal, lineHeight = 1.45),
    bodyMedium = style(14, FontWeight.Normal, lineHeight = 1.45),
    labelLarge = style(15, FontWeight.Bold),
    labelMedium = style(13, FontWeight.SemiBold),
    labelSmall = style(12, FontWeight.Medium),
)
