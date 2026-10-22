package app.ambr.domain

import app.ambr.data.CoverageEntity
import app.ambr.data.CoverageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WarrantyTest {
    private val today = LocalDate.of(2026, 10, 22)
    private fun cov(start: LocalDate, months: Int, total: Int? = null, used: Int = 0) =
        CoverageEntity(itemId = 1, startDate = start, months = months, servicesTotal = total, servicesUsed = used,
            type = if (months == 0) CoverageType.FreeServices else CoverageType.Manufacturer)

    @Test fun coveredForMonths() {
        val s = Warranty.status(listOf(cov(LocalDate.of(2024, 3, 14), 36)), today)
        assertEquals(StatusKind.Covered, s.kind)
        assertEquals(LocalDate.of(2027, 3, 14), s.until)
        assertEquals("Covered for 4 more months", Warranty.headline(s))
    }

    @Test fun endsSoonWithin30Days() {
        val s = Warranty.status(listOf(cov(LocalDate.of(2024, 11, 6), 24)), today)
        assertEquals(StatusKind.EndsSoon, s.kind)
        assertEquals(15L, s.daysLeft)
        assertEquals("Covered for 15 more days", Warranty.headline(s))
    }

    @Test fun expiredUsesLatestEnd() {
        val s = Warranty.status(listOf(cov(LocalDate.of(2024, 1, 3), 24)), today)
        assertEquals(StatusKind.Expired, s.kind)
        assertEquals("Warranty ended on 3 Jan 2026", Warranty.headline(s))
    }

    @Test fun longestActiveCoverageWins() {
        val full = cov(LocalDate.of(2025, 11, 9), 12)
        val compressor = cov(LocalDate.of(2025, 11, 9), 120)
        val s = Warranty.status(listOf(full, compressor), today)
        assertEquals(StatusKind.Covered, s.kind)
        assertEquals(LocalDate.of(2035, 11, 9), s.until)
    }

    @Test fun freeServicesCountOnly() {
        val left = Warranty.status(listOf(cov(LocalDate.of(2025, 6, 5), 0, total = 4, used = 2)), today)
        assertEquals(StatusKind.Covered, left.kind)
        assertNull(left.until)
        val used = Warranty.status(listOf(cov(LocalDate.of(2025, 6, 5), 0, total = 4, used = 4)), today)
        assertEquals(StatusKind.Expired, used.kind)
    }

    @Test fun noCoverage() {
        assertEquals(StatusKind.NoCoverage, Warranty.status(emptyList(), today).kind)
    }
}
