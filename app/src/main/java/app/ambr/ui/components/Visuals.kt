package app.ambr.ui.components

import androidx.annotation.DrawableRes
import app.ambr.R
import app.ambr.data.Category
import app.ambr.domain.StatusKind
import app.ambr.ui.theme.AmbrColors
import app.ambr.ui.theme.CategoryTone
import androidx.compose.ui.graphics.Color

data class CategoryVisual(@DrawableRes val icon: Int, val tone: CategoryTone)

fun Category.visual(name: String = ""): CategoryVisual {
    val n = name.lowercase()
    // The item name is a better hint than the category for common appliances.
    val icon = when {
        "fridge" in n || "refrigerator" in n || "freezer" in n -> R.drawable.ic_refrigerator
        " ac" in " $n" || "air con" in n -> R.drawable.ic_air_conditioner
        "fan" in n -> R.drawable.ic_fan
        "wash" in n -> R.drawable.ic_washer
        "bike" in n || "motor" in n || "scooter" in n -> R.drawable.ic_motorcycle
        else -> when (this) {
            Category.Phones -> R.drawable.ic_mobile_notch
            Category.Computers -> R.drawable.ic_laptop
            Category.TvAudio -> R.drawable.ic_screen
            Category.Kitchen -> R.drawable.ic_refrigerator
            Category.Cooling -> R.drawable.ic_air_conditioner
            Category.Laundry -> R.drawable.ic_washer
            Category.Vehicles -> R.drawable.ic_motorcycle
            Category.Furniture, Category.Tools, Category.Other -> R.drawable.ic_box
        }
    }
    val tone = when (this) {
        Category.Cooling -> CategoryTone.Cooling
        Category.Kitchen -> CategoryTone.Kitchen
        Category.TvAudio -> CategoryTone.TvAudio
        Category.Phones -> CategoryTone.Phones
        Category.Vehicles -> CategoryTone.Vehicles
        Category.Laundry -> CategoryTone.Laundry
        Category.Computers -> CategoryTone.Computers
        else -> CategoryTone.Other
    }
    return CategoryVisual(icon, tone)
}

data class StatusVisual(val label: String, @DrawableRes val icon: Int, val ink: Color, val tint: Color)

fun StatusKind.visual(): StatusVisual = when (this) {
    StatusKind.Covered -> StatusVisual("Covered", R.drawable.ic_check, AmbrColors.CoveredInk, AmbrColors.CoveredTint)
    StatusKind.EndsSoon -> StatusVisual("Ends soon", R.drawable.ic_clock_three, AmbrColors.EndsSoonInk, AmbrColors.EndsSoonTint)
    StatusKind.Expired -> StatusVisual("Expired", R.drawable.ic_minus_circle, AmbrColors.ExpiredInk, AmbrColors.ExpiredTint)
    StatusKind.NoCoverage -> StatusVisual("No warranty", R.drawable.ic_info, AmbrColors.ExpiredInk, AmbrColors.ExpiredTint)
}
