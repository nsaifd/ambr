package app.ambr.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormatTest {
    @Test fun takaUsesSouthAsianGrouping() {
        assertEquals("৳0", Format.taka(0))
        assertEquals("৳499", Format.taka(499))
        assertEquals("৳4,200", Format.taka(4200))
        assertEquals("৳78,500", Format.taka(78500))
        assertEquals("৳4,85,000", Format.taka(485000))
        assertEquals("৳1,23,45,678", Format.taka(12345678))
    }

    @Test fun datesAreSpelledOut() {
        assertEquals("14 Mar 2027", Format.date(LocalDate.of(2027, 3, 14)))
        assertEquals("6 Nov", Format.dateShort(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 10, 22)))
    }

    @Test fun warrantyLengths() {
        assertEquals("1 year", Format.warrantyLength(12))
        assertEquals("3 years", Format.warrantyLength(36))
        assertEquals("6 months", Format.warrantyLength(6))
    }
}
