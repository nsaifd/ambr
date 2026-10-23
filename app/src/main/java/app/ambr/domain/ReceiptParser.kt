package app.ambr.domain

import java.time.LocalDate
import java.time.Month

/**
 * What the receipt reader found. A field is only filled when the reader is fairly sure;
 * otherwise it stays empty and the `*Options` lists offer what was seen on the paper,
 * so the user can tap the right one instead of typing.
 */
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
    val unsureStore: Boolean = false,
    val unsurePrice: Boolean = false,
    val unsureDate: Boolean = false,
    val nameOptions: List<String> = emptyList(),
    val storeOptions: List<String> = emptyList(),
    val priceOptions: List<Long> = emptyList(),
    val dateOptions: List<LocalDate> = emptyList(),
) {
    /** Fields read with confidence, for the "4 of 5 read" badge. Guesses don't count. */
    val readCount get() = listOf(
        name != null && !unsureName, store != null && !unsureStore, priceTaka != null && !unsurePrice,
        date != null && !unsureDate, warrantyMonths != null,
    ).count { it }

    /** A string likely unique to this receipt, used to spot the same receipt saved twice. */
    val fingerprint: String? get() = invoice?.takeIf { it.length >= 4 }
}

/**
 * Rules for Bangladeshi shop receipts and warranty cards, run on OCR output. Labels win over
 * position ("Dealer: X" beats "the top line"), and a wrong value is worse than an empty one.
 */
object ReceiptParser {
    private val opts = setOf(RegexOption.IGNORE_CASE)

    // Labels, matched at the start of a row: "Dealer Name : Walton Plaza".
    private val storeLabel = Regex("""^\W*(dealer'?s?(\s*name)?|shop(\s*name)?|store(\s*name)?|showroom(\s*name)?|seller|sold\s*by|outlet|retailer|purchased\s*from)\b\s*[:\-.]?\s*(.*)$""", opts)
    private val nameLabel = Regex("""^\W*(product(\s*(name|description))?|item(\s*name)?|description(\s*of\s*goods)?|appliance|goods|model\s*name)\b\s*[:\-.]?\s*(.*)$""", opts)
    private val dateLabel = Regex("""\b(date\s*of\s*(purchase|sale|delivery)|purchase\s*date|sales?\s*date|invoice\s*date|bill\s*date|date)\b""", opts)
    private val strongTotal = Regex("""\b(grand\s*total|net\s*(total|amount|payable|bill)|total\s*(amount|payable|bill)?|amount\s*payable|payable|net)\b""", opts)
    private val weakPrice = Regex("""\b(price|mrp|amount|rate|value)\b""", opts)
    private val notTotal = Regex("""\b(sub\s*-?\s*total|vat|tax|discount|less|change|due|advance|qty|quantity|paid|cash|card|bkash|nagad|round)\b""", opts)
    private val notPurchaseDate = Regex("""\b(mfg|manufactur\w*|exp\w*|valid\w*|upto|up\s*to|till|until|print\w*|due|delivery\s*by)\b""", opts)

    private val money = Regex("""(?:(?:৳|tk\.?|taka|bdt)\s*|(?<![A-WYZa-wyz0-9\-/.,#]))(\d{1,3}(?:,\d{2,3})+|\d+)(\.\d{1,2})?(?![\dA-Za-z])""", opts)
    private val phone = Regex("""(?:\+?88)?0?1[3-9]\d{2}[\s\-]?\d{6}|\b0\d{1,3}[\s\-]\d{5,8}\b""")
    private val numericDate = Regex("""\b(\d{1,2})\s?[/.\-]\s?(\d{1,2})\s?[/.\-]\s?(\d{2,4})\b""")
    private val isoDate = Regex("""\b(20\d{2})[/.\-](\d{1,2})[/.\-](\d{1,2})\b""")
    private val dayMonth = Regex("""\b(\d{1,2})(?:st|nd|rd|th)?[\s\-/.,]*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[\s\-/.,']*(\d{2,4})\b""", opts)
    private val monthDay = Regex("""\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[\s\-.]*(\d{1,2})(?:st|nd|rd|th)?[\s,]+(\d{4})\b""", opts)

    private const val NUM = """(\d{1,2}|one|two|three|four|five|six|seven|ten|twelve)"""
    private val warrantyBefore = Regex("""$NUM\s*\(?\d*\)?\s*(years?|yrs?|months?|mons?|mos?)\.?\s*(full\s*|parts?\s*|service\s*|replacement\s*)?(warranty|warrenty|guarantee|guaranty)""", opts)
    private val warrantyAfter = Regex("""(warranty|warrenty|guarantee|guaranty)(\s*period)?\s*[:\-]?\s*$NUM\s*(years?|yrs?|months?|mons?|mos?)""", opts)

    private val serial = Regex("""\b(?:s/?n|serial(?:[ \t]*(?:no|number))?\.?|imei(?:[ \t]*\d)?|esn)[ \t]*[:#\-.]?[ \t]*([A-Z0-9][A-Z0-9\-]{5,})""", opts)
    private val model = Regex("""\bmodel(?:[ \t]*(?:no|number))?\.?[ \t]*[:#\-]?[ \t]*([A-Z0-9][A-Z0-9\-/]{2,})""", opts)
    // Codes stay on their own row ([ \t] not \s), and "INV-123" is a code, not the word "Inv".
    private val invoice = Regex("""\b(?:invoice|inv\.?|bill|memo|receipt|challan)(?![\-A-Za-z])[ \t]*(?:no\.?|#|number|num)?[ \t]*[:#\-.]?[ \t]*([A-Z0-9][A-Z0-9\-/]{3,})""", opts)
    private val trailingMoney = Regex("""(\s+(?:\d+\s*(?:x|pcs?|nos?)\s*)?(?:৳|tk\.?|bdt)?\s*\d[\d,]*(?:\.\d{1,2})?)+\s*$""", opts)
    private val storeWords = Regex("""\b(plaza|electronics?|shop|store|mart|enterprises?|traders?|trading|showroom|house|cent(er|re)|ltd|limited|corporation|gallery|world|zone|point|digital|telecom|distribution|bazar|bazaar|market|tech|technology|technologies|solutions?|systems?|computers?|gadgets?)\b""", opts)
    private val tableHeader = Regex("""\b(description|particulars|product|item|goods)\b""", opts)
    private val tableColumns = Regex("""\b(qty|quantity|unit|price|rate|amount|total|w\.?\s*days|warranty)\b""", opts)
    private val extraRow = Regex("""\b(delivery|handling|charge|shipping|installation|discount|vat|courier|packing|carrying)\b""", opts)
    private val addressRow = Regex("""\b(office|road|floor|avenue|sarani|lane|block|sector|level|address|hotline|remarks?|terms|policy|scan|sold\s*to|customer|order)\b""", opts)
    private val warrantyDays = Regex("""\b(w\.?\s*days|warranty\s*\(?\s*days\s*\)?|warr\.?\s*days)\b""", opts)
    private val knownStores = listOf(
        "Star Tech", "Ryans", "Techland", "Pickaboo", "Daraz", "Walton Plaza", "Singer", "Transcom Digital", "Transcom Electronics",
        "Best Electronics", "Rangs", "Gadget & Gear", "Apple Gadgets", "Vision Emporium", "Global Brand", "Smart Technologies",
        "Computer Source", "Samsung Plaza", "Ultra Technology", "Sumash Tech", "Potaka IT", "Vertech",
    ).map { it to Regex("\\b" + it.replace(" ", "\\s*")+ "\\b", RegexOption.IGNORE_CASE) }
    private val noise = Regex("""\b(vat|bin|tin|mushak|tel|phone|mobile|cell|hotline|www|email|cash|change|thank|thanks|qty|quantity|price|amount|rate|date|time|invoice|bill|memo|customer|address|sl|no|item|description|total|signature|sign|authori[sz]ed|counter|cashier|terms|condition|page)\b|@|\.com""", opts)

    fun parse(text: String, today: LocalDate = LocalDate.now()): ReceiptFields {
        val rows = text.lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotEmpty() }
        if (rows.isEmpty()) return ReceiptFields()

        val store = findStore(rows)
        val itemRow = findItemRow(rows)
        val name = findName(rows, store.first, itemRow)
        val price = itemRow?.let { r -> amounts(r).lastOrNull()?.takeIf { it >= 100 }?.let { it to true } } ?: findPrice(rows)
        val date = findDate(rows, today)
        val months = findWarranty(text) ?: itemRow?.let { warrantyFromDays(rows, it) }
        val joined = rows.joinToString("\n")
        fun Regex.firstCode() = findAll(joined).map { it.groupValues[1] }.firstOrNull { v -> v.any(Char::isDigit) }

        return ReceiptFields(
            name = name.first,
            store = store.first,
            priceTaka = price.first,
            date = date.first,
            warrantyMonths = months,
            brand = (Brands.find(listOfNotNull(name.first, store.first).joinToString(" ")) ?: Brands.find(joined))?.brand,
            model = model.firstCode(),
            serial = serial.firstCode(),
            invoice = invoice.firstCode(),
            unsureName = name.first != null && !name.second,
            unsureStore = store.first != null && !store.second,
            unsurePrice = price.first != null && !price.second,
            unsureDate = date.first != null && !date.second,
            nameOptions = nameOptions(rows, store.first),
            storeOptions = storeCandidates(rows).take(4),
            priceOptions = priceOptions(rows),
            dateOptions = allDates(rows, today).map { it.first }.distinct().take(4),
        )
    }

    // --- store ---

    private fun labelled(rows: List<String>, label: Regex): String? {
        rows.forEachIndexed { i, r ->
            val m = label.find(r) ?: return@forEachIndexed
            val value = m.groupValues.last().trim(' ', ':', '-', '.')
            val v = value.ifBlank { rows.getOrNull(i + 1).orEmpty() }
            if (v.count(Char::isLetter) >= 3) return v
        }
        return null
    }

    private fun storeCandidates(rows: List<String>): List<String> =
        rows.take(6).filter { r ->
            r.length in 3..45 && letterShare(r) >= 0.6f && r.count(Char::isLetter) >= 3 &&
                !noise.containsMatchIn(r) && amounts(r).isEmpty() && !hasDate(r) && !phone.containsMatchIn(r)
        }.map(::tidy).distinct()

    private fun findStore(rows: List<String>): Pair<String?, Boolean> {
        labelled(rows, storeLabel)?.let { return tidy(it.replace(phone, "")) to true }
        val all = rows.joinToString(" ")
        knownStores.firstOrNull { it.second.containsMatchIn(all) }?.let { return it.first to true }
        val candidates = storeCandidates(rows)
        candidates.firstOrNull { storeWords.containsMatchIn(it) }?.let { return it to true }
        return candidates.firstOrNull() to false
    }

    // --- name ---

    private fun cleanName(s: String) = tidy(
        s.replace(Regex("""\b(s/?n|serial|imei)\b.*$""", RegexOption.IGNORE_CASE), "").replace(trailingMoney, "").replace(Regex("""^\s*\d{1,3}[.)]?\s+"""), ""),
    )

    /**
     * The first bought item in the receipt's table: the row after the header ("Description …
     * Qty … Price") that has words and an amount, skipping delivery and similar charges.
     */
    private fun findItemRow(rows: List<String>): String? {
        val header = rows.indexOfFirst { tableHeader.containsMatchIn(it) && tableColumns.findAll(it).count() >= 1 && !storeLabel.containsMatchIn(it) }
        if (header < 0) return null
        return rows.drop(header + 1).takeWhile { !strongTotal.containsMatchIn(it) || notTotal.containsMatchIn(it) }.firstOrNull { r ->
            cleanName(r).count(Char::isLetter) >= 4 && amounts(r).isNotEmpty() && !extraRow.containsMatchIn(r) && !addressRow.containsMatchIn(r)
        }
    }

    private fun findName(rows: List<String>, store: String?, itemRow: String?): Pair<String?, Boolean> {
        labelled(rows, nameLabel)?.let { v -> cleanName(v).takeIf { it.count(Char::isLetter) >= 3 }?.let { return it to true } }
        itemRow?.let { return cleanName(it) to true }
        rows.firstOrNull { r ->
            Brands.categoryFor(r) != null && tidy(r) != store && !strongTotal.containsMatchIn(r) && !storeLabel.containsMatchIn(r) &&
                !addressRow.containsMatchIn(r) && letterShare(r) >= 0.3f
        }?.let { return cleanName(it) to true }
        return null to false
    }

    /** Star Tech and others print warranty as days in a "W.Days" column: 365 → 12 months. */
    private fun warrantyFromDays(rows: List<String>, itemRow: String): Int? {
        if (rows.none { warrantyDays.containsMatchIn(it) }) return null
        // Drop the row number and every word or code, leaving the bare numbers: "365 1 1,950 1,950".
        val numbers = itemRow.replace(Regex("""^\s*\d{1,3}[.)]?\s+"""), "").replace(Regex("""\S*[A-Za-z]\S*"""), " ")
        val days = Regex("""(?<![\d,.])\d{2,4}(?![\d,.])""").findAll(numbers).map { it.value.toInt() }.firstOrNull { it in 7..3650 } ?: return null
        return Math.round(days / 30.4f).takeIf { it > 0 }
    }

    // --- price ---

    private fun fixDigits(s: String) = s.replace(Regex("""(?<=\d)[Oo](?=[\d,.])|(?<=[\d,])[Oo]\b|(?<=\d)[Il](?=\d)""")) { if (it.value in "Oo") "0" else "1" }

    /** Money amounts on a row, ignoring phone numbers, dates and tiny numbers like quantities. */
    private fun amounts(row: String): List<Long> {
        val r = fixDigits(row).replace(phone, " ").replace(numericDate, " ").replace(isoDate, " ").replace(dayMonth, " ")
        return money.findAll(r).mapNotNull { m ->
            val digits = m.groupValues[1].replace(",", "")
            val v = digits.toLongOrNull() ?: return@mapNotNull null
            val looksLikeMoney = m.groupValues[2].isNotEmpty() || m.groupValues[1].contains(',') || m.value.trim().first().let { it == '৳' || it.isLetter() }
            // A bare 4-digit number could be a year or a code; accept it only when it is written like money.
            // A bare 4-digit number from 1990 to 2099 is more likely a year than a price.
            v.takeIf { it in 50..9_99_99_999 && digits.length <= 9 && !(digits.length == 4 && !looksLikeMoney && v in 1990..2099) }
        }.toList()
    }

    private fun findPrice(rows: List<String>): Pair<Long?, Boolean> {
        // Bottom-most total wins: "Total" often appears above "Grand total" or "Net payable".
        for (i in rows.indices.reversed()) {
            val r = rows[i]
            if (!strongTotal.containsMatchIn(r) || notTotal.containsMatchIn(r)) continue
            val v = amounts(r).maxOrNull() ?: rows.getOrNull(i + 1)?.takeIf { it.count(Char::isLetter) <= 3 }?.let { amounts(it).maxOrNull() }
            if (v != null) return v to true
        }
        val labelled = rows.filter { weakPrice.containsMatchIn(it) && !notTotal.containsMatchIn(it) }.flatMap(::amounts).distinct()
        if (labelled.size == 1) return labelled.first() to false
        return null to false
    }

    private fun priceOptions(rows: List<String>): List<Long> =
        rows.flatMap { r -> if (notTotal.containsMatchIn(r) && !strongTotal.containsMatchIn(r)) emptyList() else amounts(r) }
            .filter { it >= 100 }.distinct().sortedDescending().take(4)

    // --- date ---

    private fun hasDate(r: String) = numericDate.containsMatchIn(r) || isoDate.containsMatchIn(r) || dayMonth.containsMatchIn(r) || monthDay.containsMatchIn(r)

    /** Every valid date on the paper with whether it is unambiguous, in reading order. */
    private fun allDates(rows: List<String>, today: LocalDate, only: (String) -> Boolean = { true }): List<Pair<LocalDate, Boolean>> {
        val out = mutableListOf<Pair<LocalDate, Boolean>>()
        rows.filter(only).filterNot { notPurchaseDate.containsMatchIn(it) }.forEach { r ->
            isoDate.findAll(r).forEach { m -> safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), today)?.let { out += it to true } }
            dayMonth.findAll(r).forEach { m -> safeDate(year(m.groupValues[3]), month(m.groupValues[2]), m.groupValues[1].toInt(), today)?.let { out += it to true } }
            monthDay.findAll(r).forEach { m -> safeDate(m.groupValues[3].toInt(), month(m.groupValues[1]), m.groupValues[2].toInt(), today)?.let { out += it to true } }
            numericDate.findAll(r).forEach { m ->
                val a = m.groupValues[1].toInt(); val b = m.groupValues[2].toInt(); val y = year(m.groupValues[3])
                // Bangladesh writes the day first. When both readings work it is only a guess.
                val dayFirst = safeDate(y, b, a, today)
                val monthFirst = safeDate(y, a, b, today)
                when {
                    dayFirst != null -> out += dayFirst to (monthFirst == null || a == b)
                    monthFirst != null -> out += monthFirst to false
                }
            }
        }
        return out
    }

    private fun findDate(rows: List<String>, today: LocalDate): Pair<LocalDate?, Boolean> {
        allDates(rows, today) { dateLabel.containsMatchIn(it) }.firstOrNull()?.let { return it }
        val all = allDates(rows, today)
        val distinct = all.map { it.first }.distinct()
        return when {
            distinct.isEmpty() -> null to false
            distinct.size == 1 -> all.first()
            // Several dates and none labelled: the earliest printed one, but ask the user to check.
            else -> all.first().first to false
        }
    }

    private fun month(s: String) = Month.entries.first { it.name.startsWith(s.uppercase().take(3)) }.value
    private fun year(s: String) = s.toInt().let { if (it < 100) 2000 + it else it }
    private fun safeDate(y: Int, m: Int, d: Int, today: LocalDate): LocalDate? =
        if (y in 2000..today.year) runCatching { LocalDate.of(y, m, d) }.getOrNull()?.takeIf { !it.isAfter(today) } else null

    // --- warranty ---

    private fun number(s: String) = s.toIntOrNull() ?: when (s.lowercase()) {
        "one" -> 1; "two" -> 2; "three" -> 3; "four" -> 4; "five" -> 5; "six" -> 6; "seven" -> 7; "ten" -> 10; "twelve" -> 12; else -> null
    }

    private fun toMonths(n: String, unit: String): Int? {
        val v = number(n) ?: return null
        val months = if (unit.lowercase().startsWith("y")) v * 12 else v
        return months.takeIf { it in 1..240 }
    }

    private fun findWarranty(text: String): Int? {
        val t = text.replace('\n', ' ')
        return warrantyBefore.find(t)?.let { toMonths(it.groupValues[1], it.groupValues[2]) }
            ?: warrantyAfter.find(t)?.let { toMonths(it.groupValues[3], it.groupValues[4]) }
    }

    // --- helpers ---

    private fun letterShare(s: String): Float {
        val visible = s.count { !it.isWhitespace() }
        return if (visible == 0) 0f else s.count { it.isLetter() || it == '&' || it == '\'' || it == '.' }.toFloat() / visible
    }

    /** "WALTON PLAZA" → "Walton Plaza"; keeps model codes like GS-18XFV32 as they are. */
    private fun tidy(s: String): String = s.trim().trim('-', ':', '*', '.', ',', '|').split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { w ->
        if (w.any(Char::isDigit) || w.contains('-') || w.contains('/') || w.length <= 4) w
        else if (w.all { !it.isLetter() || it.isUpperCase() }) w.lowercase().replaceFirstChar { it.uppercase() }
        else w
    }
}
