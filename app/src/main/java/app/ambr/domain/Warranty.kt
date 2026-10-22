package app.ambr.domain

import app.ambr.data.CoverageEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class StatusKind { Covered, EndsSoon, Expired, NoCoverage }

data class CoverageState(
    val coverage: CoverageEntity,
    val end: LocalDate?,
    val active: Boolean,
    val daysLeft: Long?,
    /** 0..1 share of the coverage period already used; null for count-only coverages. */
    val fractionUsed: Float?,
    val servicesLeft: Int?,
)

data class ItemStatus(
    val kind: StatusKind,
    /** End of the longest active dated coverage, or the latest end when expired. */
    val until: LocalDate?,
    val daysLeft: Long?,
    val monthsLeft: Long?,
    val fractionUsed: Float?,
    val coverages: List<CoverageState>,
)

object Warranty {
    const val SOON_DAYS = 30L

    fun end(c: CoverageEntity): LocalDate? = if (c.months > 0) c.startDate.plusMonths(c.months.toLong()) else null

    fun coverageState(c: CoverageEntity, today: LocalDate): CoverageState {
        val end = end(c)
        val servicesLeft = c.servicesTotal?.let { (it - c.servicesUsed).coerceAtLeast(0) }
        val dateActive = end?.let { !today.isAfter(it) }
        val active = when {
            end != null && servicesLeft != null -> dateActive == true && servicesLeft > 0
            end != null -> dateActive == true
            servicesLeft != null -> servicesLeft > 0
            else -> false
        }
        val daysLeft = end?.let { ChronoUnit.DAYS.between(today, it).coerceAtLeast(0) }
        val fraction = end?.let {
            val total = ChronoUnit.DAYS.between(c.startDate, it).coerceAtLeast(1)
            val used = ChronoUnit.DAYS.between(c.startDate, today).coerceIn(0, total)
            used.toFloat() / total
        }
        return CoverageState(c, end, active, daysLeft, fraction, servicesLeft)
    }

    fun status(coverages: List<CoverageEntity>, today: LocalDate = LocalDate.now()): ItemStatus {
        val states = coverages.map { coverageState(it, today) }
        if (states.isEmpty()) return ItemStatus(StatusKind.NoCoverage, null, null, null, null, states)

        val activeDated = states.filter { it.active && it.end != null }
        val anyActive = states.any { it.active }
        if (!anyActive) {
            val lastEnd = states.mapNotNull { it.end }.maxOrNull()
            return ItemStatus(StatusKind.Expired, lastEnd, 0, 0, 1f, states)
        }
        val best = activeDated.maxByOrNull { it.end!! }
            ?: return ItemStatus(StatusKind.Covered, null, null, null, null, states)
        val until = best.end!!
        val days = best.daysLeft ?: 0
        val kind = if (days <= SOON_DAYS) StatusKind.EndsSoon else StatusKind.Covered
        return ItemStatus(kind, until, days, ChronoUnit.MONTHS.between(today, until), best.fractionUsed, states)
    }

    /** The plain-words line shown at the top of Item detail and Claim pack. */
    fun headline(s: ItemStatus): String = when (s.kind) {
        StatusKind.NoCoverage -> "No warranty added yet"
        StatusKind.Expired -> s.until?.let { "Warranty ended on ${Format.date(it)}" } ?: "Warranty has ended"
        else -> when {
            s.until == null -> "Covered"
            (s.monthsLeft ?: 0) >= 2 -> "Covered for ${s.monthsLeft} more months"
            (s.daysLeft ?: 0) == 0L -> "Covered until today"
            (s.daysLeft ?: 0) == 1L -> "Covered for 1 more day"
            else -> "Covered for ${s.daysLeft} more days"
        }
    }
}
