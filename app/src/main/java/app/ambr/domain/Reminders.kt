package app.ambr.domain

import app.ambr.data.CoverageEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * At most two notes per warranty: one `firstDays` before it ends, and one 7 days before
 * unless the user tapped "All good". Nothing on the end date itself.
 */
object Reminders {
    const val SECOND_DAYS = 7L

    /** Returns the reminder number (1 or 2) due today, or null. */
    fun due(c: CoverageEntity, today: LocalDate, firstDays: Int, second: Boolean): Int? {
        val end = Warranty.end(c) ?: return null
        val left = ChronoUnit.DAYS.between(today, end)
        if (left <= 0) return null
        return when {
            second && !c.reminderDismissed && c.remindersSent < 2 && left <= SECOND_DAYS -> 2
            c.remindersSent == 0 && left <= firstDays && left > SECOND_DAYS -> 1
            // The item was added late, already inside the first window but past the second one.
            c.remindersSent == 0 && left <= SECOND_DAYS && !second -> 1
            else -> null
        }
    }
}
