package app.ambr.domain

import app.ambr.data.CoverageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RemindersTest {
    private val start = LocalDate.of(2025, 1, 10)
    private val end = start.plusMonths(12)
    private fun cov(sent: Int = 0, dismissed: Boolean = false) =
        CoverageEntity(itemId = 1, startDate = start, months = 12, remindersSent = sent, reminderDismissed = dismissed)

    @Test fun firstAtThirtyDays() = assertEquals(1, Reminders.due(cov(), end.minusDays(30), 30, true))
    @Test fun notBeforeWindow() = assertNull(Reminders.due(cov(), end.minusDays(31), 30, true))
    @Test fun firstOnlyOnce() = assertNull(Reminders.due(cov(sent = 1), end.minusDays(20), 30, true))
    @Test fun secondAtSevenDays() = assertEquals(2, Reminders.due(cov(sent = 1), end.minusDays(7), 30, true))
    @Test fun noSecondWhenAllGood() = assertNull(Reminders.due(cov(sent = 1, dismissed = true), end.minusDays(7), 30, true))
    @Test fun noSecondWhenTurnedOff() = assertNull(Reminders.due(cov(sent = 1), end.minusDays(7), 30, false))
    @Test fun neverOnEndDate() = assertNull(Reminders.due(cov(sent = 1), end, 30, true))
    @Test fun neverThird() = assertNull(Reminders.due(cov(sent = 2), end.minusDays(3), 30, true))
    @Test fun countOnlyCoverageIgnored() = assertNull(Reminders.due(cov().copy(months = 0, servicesTotal = 4), end.minusDays(5), 30, true))
}
