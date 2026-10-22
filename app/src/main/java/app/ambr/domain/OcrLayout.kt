package app.ambr.domain

/** One line of text from the OCR engine with its box on the page, in pixels. */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val height get() = (bottom - top).coerceAtLeast(1)
    val centerY get() = (top + bottom) / 2f
}

/**
 * Receipts are tables: the item name sits on the left and its price on the right, and OCR
 * returns them as separate pieces. This puts pieces that share a row back on one line,
 * left to right, so "TOTAL" and "52,990.00" end up together again.
 */
object OcrLayout {
    fun rows(lines: List<OcrLine>): List<String> {
        if (lines.isEmpty()) return emptyList()
        val typical = lines.map { it.height }.sorted()[lines.size / 2].toFloat()
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in lines.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()
            val rowCenter = row?.let { r -> r.map { it.centerY }.average().toFloat() }
            // Same row when the centres are within about half a line of each other.
            if (row != null && rowCenter != null && kotlin.math.abs(line.centerY - rowCenter) <= typical * 0.55f) row.add(line)
            else rows.add(mutableListOf(line))
        }
        return rows.map { r -> r.sortedBy { it.left }.joinToString("  ") { it.text.trim() } }.filter { it.isNotBlank() }
    }
}
