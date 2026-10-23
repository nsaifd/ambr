package app.ambr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrLayoutTest {
    @Test
    fun joinsTableCellsOnTheSameRow() {
        val rows = OcrLayout.rows(
            listOf(
                OcrLine("1,950", 900, 402, 980, 430),
                OcrLine("MaxGreen Mini UPS", 100, 400, 500, 428),
                OcrLine("Grand Total", 600, 500, 760, 528),
                OcrLine("2,020", 900, 503, 980, 531),
            ),
        )
        assertEquals(listOf("MaxGreen Mini UPS  1,950", "Grand Total  2,020"), rows)
    }
}
