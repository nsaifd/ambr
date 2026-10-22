package app.ambr.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val full = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val short = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    /** "14 Mar 2027". Never 14/03/27, because day and month order confuses people. */
    fun date(d: LocalDate): String = d.format(full)

    /** "14 Mar" when the year is the current one, else the full date. */
    fun dateShort(d: LocalDate, today: LocalDate = LocalDate.now()): String =
        if (d.year == today.year) d.format(short) else d.format(full)

    /** Taka with South Asian grouping: ৳4,85,000. */
    fun taka(amount: Long): String {
        val negative = amount < 0
        val digits = kotlin.math.abs(amount).toString()
        val grouped = if (digits.length <= 3) digits else {
            val head = digits.dropLast(3)
            val tail = digits.takeLast(3)
            head.reversed().chunked(2).joinToString(",").reversed() + "," + tail
        }
        return (if (negative) "-" else "") + "৳" + grouped
    }

    fun warrantyLength(months: Int): String = when {
        months <= 0 -> "Free services"
        months % 12 == 0 -> if (months == 12) "1 year" else "${months / 12} years"
        months == 1 -> "1 month"
        else -> "$months months"
    }
}
