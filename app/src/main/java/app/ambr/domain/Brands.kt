package app.ambr.domain

import app.ambr.data.Category

/**
 * What Ambr suggests when it recognises a brand. These are typical terms only; the form always
 * says "usually" and the user picks the real length from their warranty card.
 */
data class BrandHint(
    val brand: String,
    val serviceName: String,
    val usualMonths: Map<Category, Int>,
    /** Extra part coverage, e.g. a compressor, keyed by category. */
    val partCover: Map<Category, Pair<String, Int>> = emptyMap(),
)

object Brands {
    private val all = listOf(
        BrandHint("Walton", "Walton Service Center", mapOf(Category.Kitchen to 12, Category.Cooling to 12, Category.TvAudio to 12, Category.Phones to 12),
            mapOf(Category.Kitchen to ("Compressor" to 120), Category.Cooling to ("Compressor" to 60))),
        BrandHint("Singer", "Singer Service Center", mapOf(Category.Kitchen to 12, Category.Laundry to 12, Category.TvAudio to 12, Category.Cooling to 12),
            mapOf(Category.Kitchen to ("Compressor" to 60))),
        BrandHint("Samsung", "Samsung Service Center", mapOf(Category.Phones to 12, Category.TvAudio to 12, Category.Kitchen to 12, Category.Laundry to 12, Category.Cooling to 12)),
        BrandHint("Gree", "Gree Service Center", mapOf(Category.Cooling to 36), mapOf(Category.Cooling to ("Compressor" to 60))),
        BrandHint("Vision", "Vision Service Center", mapOf(Category.Cooling to 24, Category.Kitchen to 12, Category.TvAudio to 12)),
        BrandHint("LG", "LG Service Center", mapOf(Category.TvAudio to 12, Category.Kitchen to 12, Category.Laundry to 12, Category.Cooling to 12)),
        BrandHint("Sony", "Sony Service Center", mapOf(Category.TvAudio to 12)),
        BrandHint("Xiaomi", "Xiaomi Service Center", mapOf(Category.Phones to 12, Category.TvAudio to 12)),
        BrandHint("Apple", "Apple Authorised Service Provider", mapOf(Category.Phones to 12, Category.Computers to 12)),
        BrandHint("HP", "HP Service Center", mapOf(Category.Computers to 12)),
        BrandHint("Lenovo", "Lenovo Service Center", mapOf(Category.Computers to 12)),
        BrandHint("Yamaha", "Yamaha Service Center", mapOf(Category.Vehicles to 24)),
        BrandHint("Minister", "Minister Service Center", mapOf(Category.Kitchen to 12, Category.TvAudio to 12)),
    )

    private val aliases = mapOf("redmi" to "Xiaomi", "mi " to "Xiaomi", "iphone" to "Apple", "macbook" to "Apple", "galaxy" to "Samsung")

    fun find(text: String?): BrandHint? {
        if (text.isNullOrBlank()) return null
        val t = " ${text.lowercase()} "
        all.firstOrNull { Regex("\\b${it.brand.lowercase()}\\b").containsMatchIn(t) }?.let { return it }
        return aliases.entries.firstOrNull { it.key in t }?.let { a -> all.first { it.brand == a.value } }
    }

    /** Guesses the category from words in the item name. */
    private val categoryWords = listOf(
        Category.Kitchen to listOf("fridge", "refrigerator", "freezer", "oven", "microwave", "blender", "cooker", "kettle", "toaster", "juicer", "dispenser"),
        Category.Cooling to listOf("ac", "air con", "fan", "cooler", "inverter ac"),
        Category.TvAudio to listOf("tv", "television", "speaker", "sound", "headphone", "earbud", "soundbar"),
        Category.Phones to listOf("phone", "smartphone", "iphone", "galaxy", "redmi", "tablet", "ipad", "watch", "power bank"),
        Category.Computers to listOf("laptop", "macbook", "computer", "monitor", "printer", "router", "ups", "keyboard", "mouse", "ssd", "desktop", "projector"),
        Category.Laundry to listOf("wash", "washing", "dryer", "iron"),
        Category.Vehicles to listOf("bike", "motorbike", "motorcycle", "scooter", "car", "bicycle", "cycle"),
    ).map { (c, words) -> c to Regex("\\b(" + words.joinToString("|") + ")s?\\b", RegexOption.IGNORE_CASE) }

    /** Guesses the category from whole words in the item name, so "Bangla Motor" or "card" don't count. */
    fun categoryFor(name: String): Category? = categoryWords.firstOrNull { it.second.containsMatchIn(name) }?.first
}
