package app.ambr.domain

import java.time.LocalDate
import java.time.Month

/** What the receipt reader found. Null means not found; the `unsure` flags mark weak guesses. */
data class ReceiptFields(
    val name: String? = null,
    val store: String? = null,
    val priceTaka: Long? = null,
    val date: LocalDate? = null,
    val warrantyMonths: Int? = null,
    val brand: String? = null,
    val model: String? = null,
    val serial: String? = null,
    val invoice: String? = null,
    val unsureName: Boolean = false,
    val unsurePrice: Boolean = false,
    val unsureDate: Boolean = false,
) {
    /** How many of the five main fields were found, for the "4 of 5 read" badge. */
    val readCount get() = listOf(name, store, priceTaka, date, warrantyMonths).count { it != null }

    /** A string likely unique to this receipt, used to spot the same receipt saved twice. */
    val fingerprint: String? get() = invoice?.takeIf { it.length >= 4 }
}

/** Plain-text rules for Bangladeshi shop receipts. Works on OCR output, so it expects noise. */
object ReceiptParser {
    private val totalWords = Regex("""\b(grand\s*total|net\s*(amount|payable|total)?|total\s*(amount|payable)?|amount\s*payable|payable|net)\b""", RegexOption.IGNORE_CASE)
    private val money = Regex("""(?:৳|tk\.?|bdt)?\s*(\d{1,3}(?:,\d{2,3})+|\d+)(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE)
    private val numericDate = Regex("""\b(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})\b""")
    private val isoDate = Regex("""\b(20\d{2})[/.\-](\d{1,2})[/.\-](\d{1,2})\b""")
    private val wordDate = Regex("""\b(\d{1,2})\s*[- ]?\s*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[,.\- ]*\s*(\d{2,4})\b""", RegexOption.IGNORE_CASE)
    private val warranty = Regex("""(\d{1,2})\s*(years?|yrs?|months?|mos?)\.?\s*(full\s*)?(warranty|guarantee|service\s*warranty)""", RegexOption.IGNORE_CASE)
    private val warrantyAfter = Regex("""(warranty|guarantee)\s*[:\-]?\s*(\d{1,2})\s*(years?|yrs?|months?|mos?)""", RegexOption.IGNORE_CASE)
    private val serial = Regex("""\b(?:s/?n|serial(?:\s*no\.?)?|imei)\s*[:#\-]?\s*([A-Z0-9\-]{6,})""", RegexOption.IGNORE_CASE)
    private val model = Regex("""\bmodel(?:\s*no\.?)?\s*[:#\-]?\s*([A-Z0-9][A-Z0-9\-/]{2,})""", RegexOption.IGNORE_CASE)
    private val invoice = Regex("""\b(?:inv(?:oice)?|bill|memo|receipt)\s*(?:no\.?|#|number)?\s*[:#\-]?\s*([A-Z0-9][A-Z0-9\-/]{3,})""", RegexOption.IGNORE_CASE)
    private val trailingMoney = Regex("""\s+(?:\d+\s*x\s*)?(?:৳|tk\.?|bdt)?\s*\d[\d,]*(?:\.\d{1,2})?\s*$""", RegexOption.IGNORE_CASE)
    private val noise = Regex("""(?i)(vat|bin|tin|mushak|tel|phone|mobile|hotline|www\.|@|cash|change|thank|qty|price|amount|date|time|invoice|bill|memo|customer|address|sl\b|item|description)""")

    fun parse(text: String, today: LocalDate = LocalDate.now()): ReceiptFields {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ReceiptFields()

        val (price, priceSure) = findPrice(lines)
        val (date, dateSure) = findDate(text, today)
        val store = findStore(lines)
        val (name, nameSure) = findName(lines, store)
        val months = warranty.find(text)?.let { toMonths(it.groupValues[1], it.groupValues[2]) }
            ?: warrantyAfter.find(text)?.let { toMonths(it.groupValues[2], it.groupValues[3]) }
        val brandHint = Brands.find(listOfNotNull(name, store).joinToString(" ")) ?: Brands.find(text)

        return ReceiptFields(
            name = name,
            store = store,
            priceTaka = price,
            date = date,
            warrantyMonths = months,
            brand = brandHint?.brand,
            model = model.find(text)?.groupValues?.get(1),
            serial = serial.find(text)?.groupValues?.get(1),
            invoice = invoice.find(text)?.groupValues?.get(1)?.takeIf { it.any(Char::isDigit) },
            unsureName = name != null && !nameSure,
            unsurePrice = price != null && !priceSure,
            unsureDate = date != null && !dateSure,
        )
    }

    private fun toMonths(n: String, unit: String): Int? {
        val v = n.toIntOrNull() ?: return null
        return if (unit.lowercase().startsWith("y")) v * 12 else v
    }

    private fun amounts(line: String): List<Long> = money.findAll(line).mapNotNull { m ->
        val digits = m.groupValues[1].replace(",", "")
        // Skip things that look like years, phone numbers or quantities.
        digits.toLongOrNull()?.takeIf { it in 50..99_99_999 && digits.length <= 8 }
    }.toList()

    private fun findPrice(lines: List<String>): Pair<Long?, Boolean> {
        // A total line, or the line right after a bare "TOTAL" label.
        lines.forEachIndexed { i, line ->
            if (totalWords.containsMatchIn(line) && !line.contains("sub", ignoreCase = true)) {
                val here = amounts(line).maxOrNull() ?: lines.getOrNull(i + 1)?.let { amounts(it).maxOrNull() }
                if (here != null) return here to true
            }
        }
        val best = lines.filterNot { numericDate.containsMatchIn(it) }.flatMap(::amounts).maxOrNull()
        return best to false
    }

    private fun findDate(text: String, today: LocalDate): Pair<LocalDate?, Boolean> {
        isoDate.find(text)?.let { m ->
            val d = safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
            if (d != null && !d.isAfter(today)) return d to true
        }
        wordDate.find(text)?.let { m ->
            val month = Month.entries.first { it.name.startsWith(m.groupValues[2].uppercase().take(3)) }
            val d = safeDate(year(m.groupValues[3]), month.value, m.groupValues[1].toInt())
            if (d != null && !d.isAfter(today)) return d to true
        }
        numericDate.findAll(text).forEach { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            val y = year(m.groupValues[3])
            // Bangladesh writes day first. If both readings are valid it is still a guess.
            val dayFirst = safeDate(y, b, a)?.takeIf { !it.isAfter(today) }
            val monthFirst = safeDate(y, a, b)?.takeIf { !it.isAfter(today) }
            when {
                dayFirst != null -> return dayFirst to (monthFirst == null || a == b || a > 12)
                monthFirst != null -> return monthFirst to false
            }
        }
        return null to false
    }

    private fun year(s: String) = s.toInt().let { if (it < 100) 2000 + it else it }

    private fun safeDate(y: Int, m: Int, d: Int): LocalDate? =
        if (y in 2000..2100) runCatching { LocalDate.of(y, m, d) }.getOrNull() else null

    private fun findStore(lines: List<String>): String? =
        lines.take(4).firstOrNull { l ->
            l.length in 3..40 && l.count(Char::isLetter) >= 3 && !noise.containsMatchIn(l) && !numericDate.containsMatchIn(l) && amounts(l).isEmpty()
        }?.let(::tidy)

    private fun findName(lines: List<String>, store: String?): Pair<String?, Boolean> {
        val known = lines.firstOrNull { Brands.categoryFor(it) != null && it != store && !totalWords.containsMatchIn(it) }
        if (known != null) return tidy(known.replace(trailingMoney, "")) to true
        // Otherwise the first line with letters followed by an amount on the same or next line.
        lines.forEachIndexed { i, l ->
            if (i == 0 || l == store || noise.containsMatchIn(l) || totalWords.containsMatchIn(l)) return@forEachIndexed
            val letters = l.count(Char::isLetter)
            if (letters >= 4 && (amounts(l).isNotEmpty() || lines.getOrNull(i + 1)?.let { amounts(it).isNotEmpty() } == true)) {
                return tidy(l.replace(trailingMoney, "")) to false
            }
        }
        return null to false
    }

    /** "WALTON PLAZA" → "Walton Plaza"; keeps model codes like GS-18XFV32 as they are. */
    private fun tidy(s: String): String = s.trim().trim('-', ':', '*', '.').split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { w ->
        if (w.any(Char::isDigit) || w.contains('-') || w.length <= 2) w
        else if (w.all { !it.isLetter() || it.isUpperCase() }) w.lowercase().replaceFirstChar { it.uppercase() }
        else w
    }
}
