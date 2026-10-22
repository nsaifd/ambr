package app.ambr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {
    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun readsTypicalWaltonReceipt() {
        val r = ReceiptParser.parse(
            """
            WALTON PLAZA
            Mirpur 10, Dhaka
            Inv: WP-26-10117
            20/10/2026 17:05
            WFE-3E8 GDEN-XX Fridge
            1 x 52,990.00
            NET 52,990.00
            1 year warranty
            """.trimIndent(),
            today,
        )
        assertEquals("Walton Plaza", r.store)
        assertEquals(52_990L, r.priceTaka)
        assertEquals(LocalDate.of(2026, 10, 20), r.date)
        assertEquals(12, r.warrantyMonths)
        assertEquals("Walton", r.brand)
        assertEquals("WP-26-10117", r.invoice)
        assertTrue(r.name!!.contains("Fridge"))
        assertEquals(5, r.readCount)
        assertFalse(r.unsureDate)
    }

    @Test
    fun ambiguousDateIsFlagged() {
        val r = ReceiptParser.parse("SHOP\n05/04/2026\nTOTAL 1,200", today)
        assertEquals(LocalDate.of(2026, 4, 5), r.date)
        assertTrue(r.unsureDate)
    }

    @Test
    fun wordDateAndYearsWarranty() {
        val r = ReceiptParser.parse("GREE BRAND SHOP\nDate: 14 Mar 2024\nGrand Total Tk 78,500\nWarranty: 3 years", today)
        assertEquals(LocalDate.of(2024, 3, 14), r.date)
        assertEquals(78_500L, r.priceTaka)
        assertEquals(36, r.warrantyMonths)
    }

    @Test
    fun totalOnNextLine() {
        val r = ReceiptParser.parse("STAR TECH\nTOTAL\n1,25,000.00", today)
        assertEquals(1_25_000L, r.priceTaka)
    }

    @Test
    fun serialAndModel() {
        val r = ReceiptParser.parse("Model: GS-18XFV32\nS/N: GR18K24030917", today)
        assertEquals("GS-18XFV32", r.model)
        assertEquals("GR18K24030917", r.serial)
    }

    @Test
    fun emptyTextFindsNothing() {
        val r = ReceiptParser.parse("   ", today)
        assertNull(r.price())
        assertEquals(0, r.readCount)
    }

    private fun ReceiptFields.price() = priceTaka
}
