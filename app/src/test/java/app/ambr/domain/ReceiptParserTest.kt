package app.ambr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {
    private val today = LocalDate.of(2026, 10, 22)

    /** A Star Tech invoice, as the rows come out of OcrLayout once the photo is upright. */
    private val starTech = """
        STAR TECH
        Head Office: 28, Navana Zohura Square, (6th Floor), Kazi Nazrul Islam Avenue, Bangla Motor, Dhaka-1000.
        Corporate Office: Multiplan Center, Shop # 1120, Level # 11, 69-71, New Elephant Road, Dhaka 1205.
        Hotline: 16793, 09678002003.
        INVOICE
        Invoice  INV-STLEC-2026-177205  S. Order No:  SO-STLEC-ONLINE-2026-174181
        Sold To:  Saif D. 01777117537  Stock Location:  Central E-Commerce
        Address:  Reza Plaza 56 (4th Floor), Pragati Sarani,  Date:  14-Oct-2026
        Road 12, Block-J Baridhara Dhaka 1219  Sales Person:  H.Habib1920
        Phone No:  01777117537  Entry By:  Online
        Remarks:  Order #1111986 Total- 2020 Paid- 0? Due-  Payment Terms:  Online Sales
        2,020?  Promised Date:  14-Oct-2026
        S/L No  Product Description  W.Days  Quantity  Unit Price  Total Price
        1  MaxGreen DU/BU-B10400 10400mAh Mini UPS for Router  365  1  1,950  1,950
        S/N STLDUB10400AH2013
        2  Home Delivery and Handling Charge  0  1  70  70
        Grand Total  2,020
        In Word Taka: TWO THOUSAND TWENTY TAKA ONLY
        Scan for Warranty Policy
        Signature & Company Stamp  Authorized Signature and Company Stamp
        1. PLEASE KEEP THE FULL BOX FOR WARRANTY PURPOSE.
        2. Goods once sold will not be refunded & changed.
        Warranty Complain receiving hour 11.00 AM to 6.00 PM
    """.trimIndent()

    @Test
    fun readsStarTechInvoice() {
        val r = ReceiptParser.parse(starTech, today)
        assertEquals("Star Tech", r.store)
        assertEquals("MaxGreen DU/BU-B10400 10400mAh Mini UPS for Router", r.name)
        assertEquals(1_950L, r.priceTaka)
        assertEquals(LocalDate.of(2026, 10, 14), r.date)
        assertEquals(12, r.warrantyMonths)
        assertEquals("STLDUB10400AH2013", r.serial)
        assertEquals("INV-STLEC-2026-177205", r.invoice)
        assertEquals(5, r.readCount)
        assertTrue(2_020L in r.priceOptions)
    }

    @Test
    fun addressWordsAreNotProducts() {
        // "Bangla Motor" is a place, not a motorbike.
        val r = ReceiptParser.parse("SHOP\nKazi Nazrul Islam Avenue, Bangla Motor, Dhaka", today)
        assertNull(r.name)
    }

    @Test
    fun readsTypicalWaltonReceipt() {
        val r = ReceiptParser.parse(
            """
            WALTON PLAZA
            Mirpur 10, Dhaka
            Inv: WP-26-10117
            20/10/2026 17:05
            WFE-3E8 GDEN-XX Fridge  1 x 52,990.00
            NET  52,990.00
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
        assertFalse(r.unsureDate)
    }

    @Test
    fun readsWarrantyCardLabels() {
        val r = ReceiptParser.parse(
            """
            WARRANTY CARD
            Customer Name: Rahim Uddin
            Product: Split Air Conditioner
            Model No: GS-18XFV32
            Serial No: GR18K24030917
            Dealer: Gree Brand Shop, Bashundhara City
            Date of Purchase: 14/03/2024
            Mfg Date: 02/01/2024
            Warranty Period: 3 Years
            """.trimIndent(),
            today,
        )
        assertEquals("Split Air Conditioner", r.name)
        assertEquals("Gree Brand Shop, Bashundhara City", r.store)
        assertEquals(LocalDate.of(2024, 3, 14), r.date)
        assertEquals(36, r.warrantyMonths)
        assertEquals("GS-18XFV32", r.model)
        assertEquals("GR18K24030917", r.serial)
    }

    @Test
    fun noTotalMeansNoGuessedPrice() {
        // Without a total line the reader must not pick the biggest number it sees.
        val r = ReceiptParser.parse("SOME SHOP\nRef 552210\nCall 16793\nThank you", today)
        assertNull(r.priceTaka)
    }

    @Test
    fun ambiguousDateIsFlagged() {
        val r = ReceiptParser.parse("SHOP\nDate: 05/04/2026\nTOTAL 1,200", today)
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
    fun emptyTextFindsNothing() {
        val r = ReceiptParser.parse("   ", today)
        assertNull(r.priceTaka)
        assertEquals(0, r.readCount)
    }
}
