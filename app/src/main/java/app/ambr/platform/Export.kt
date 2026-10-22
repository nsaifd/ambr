package app.ambr.platform

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import app.ambr.domain.Format
import app.ambr.domain.Warranty
import app.ambr.ui.state.ItemUi
import java.io.File
import java.time.LocalDate

/** "Export everything": one CSV for spreadsheets, or one PDF with a page per item. */
object Export {
    private fun dir(context: Context) = File(context.cacheDir, "shared").apply { mkdirs() }

    fun csv(context: Context, items: List<ItemUi>): File {
        fun cell(v: Any?) = "\"" + (v?.toString() ?: "").replace("\"", "\"\"") + "\""
        val header = listOf("Name", "Category", "Room", "Brand", "Model", "Serial", "Price (BDT)", "Bought on", "Store", "Warranty status", "Covered until", "Service center", "Service phone", "Archived", "Notes")
        val rows = items.map { ui ->
            val i = ui.item
            listOf(i.name, i.category.label, i.room.label, i.brand, i.model, i.serial, i.priceTaka, i.purchaseDate, i.store,
                Warranty.headline(ui.status), ui.status.until, i.serviceName, i.servicePhone, i.archivedReason?.name, i.notes).joinToString(",") { cell(it) }
        }
        return File(dir(context), "Ambr-export-${LocalDate.now()}.csv").apply {
            // BOM so Excel opens the taka sign and Bangla names correctly.
            writeText("﻿" + (listOf(header.joinToString(",") { cell(it) }) + rows).joinToString("\r\n"))
        }
    }

    fun pdf(context: Context, items: List<ItemUi>, withPhotos: Boolean): File {
        val w = 595; val h = 842; val m = 48f
        val doc = PdfDocument()
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(0x1D, 0x24, 0x33) }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.rgb(0x64, 0x6B, 0x7E) }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; color = Color.rgb(0x1D, 0x24, 0x33) }
        val honey = Paint().apply { color = Color.rgb(0xEF, 0x94, 0x26) }
        var n = 1

        // Cover: totals and a line per item.
        doc.startPage(PdfDocument.PageInfo.Builder(w, h, n++).create()).also { p ->
            val c = p.canvas
            c.drawRect(0f, 0f, w.toFloat(), 8f, honey)
            var y = m + 20
            c.drawText("Ambr — everything I own", m, y, title); y += 22
            c.drawText("${items.size} items · ${Format.taka(items.sumOf { it.item.priceTaka ?: 0 })} · exported ${Format.date(LocalDate.now())}", m, y, label); y += 30
            items.forEach { ui ->
                if (y > h - m) return@forEach
                c.drawText(ui.item.name, m, y, value)
                c.drawText(Warranty.headline(ui.status), m + 260, y, label)
                y += 20
            }
            doc.finishPage(p)
        }
        items.forEach { ui ->
            val p = doc.startPage(PdfDocument.PageInfo.Builder(w, h, n++).create())
            val c = p.canvas
            val i = ui.item
            var y = m + 20
            c.drawText(i.name, m, y, title); y += 20
            c.drawText(Warranty.headline(ui.status), m, y, label); y += 26
            listOf("Brand · model" to listOfNotNull(i.brand, i.model).joinToString(" "), "Serial" to i.serial, "Price" to i.priceTaka?.let(Format::taka),
                "Bought on" to i.purchaseDate?.let(Format::date), "Store" to i.store, "Room" to i.room.label,
                "Service center" to listOfNotNull(i.serviceName, i.servicePhone).joinToString(" · ")).forEach { (k, v) ->
                if (v.isNullOrBlank()) return@forEach
                c.drawText(k.uppercase(), m, y, label); c.drawText(v, m + 130, y, value); y += 20
            }
            if (withPhotos) {
                ui.details.proofs.sortedBy { it.page }.firstOrNull()?.let { proof ->
                    BitmapFactory.decodeFile(proof.filePath, BitmapFactory.Options().apply { inSampleSize = 3 })?.let { bmp ->
                        val box = RectF(m, y + 12, w - m, h - m)
                        val s = minOf(box.width() / bmp.width, box.height() / bmp.height)
                        c.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), RectF(box.left, box.top, box.left + bmp.width * s, box.top + bmp.height * s), Paint(Paint.FILTER_BITMAP_FLAG))
                        bmp.recycle()
                    }
                }
            }
            doc.finishPage(p)
        }
        val file = File(dir(context), "Ambr-export-${LocalDate.now()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
