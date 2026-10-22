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

/** Builds a claim pack PDF: a summary page, then one page per proof image. A4 at 72 dpi. */
object ClaimPdf {
    private const val W = 595
    private const val H = 842
    private const val M = 48f

    fun build(context: Context, ui: ItemUi): File {
        val doc = PdfDocument()
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 26f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(0x1D, 0x24, 0x33) }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.rgb(0x64, 0x6B, 0x7E) }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(0x1D, 0x24, 0x33) }
        val honey = Paint().apply { color = Color.rgb(0xEF, 0x94, 0x26) }

        val page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, 1).create())
        page.canvas.apply {
            drawRect(0f, 0f, W.toFloat(), 8f, honey)
            var y = M + 24
            drawText(ui.item.name, M, y, title); y += 26
            drawText(Warranty.headline(ui.status), M, y, value); y += 34
            val i = ui.item
            listOfNotNull(
                "Serial number" to i.serial,
                "Brand" to i.brand, "Model" to i.model,
                "Bought on" to i.purchaseDate?.let(Format::date),
                "Price" to i.priceTaka?.let(Format::taka),
                "Store" to i.store,
                "Service center" to listOfNotNull(i.serviceName, i.servicePhone).joinToString(" · ").ifBlank { null },
            ).filter { it.second != null }.forEach { (k, v) ->
                drawText(k.uppercase(), M, y, label); y += 18
                drawText(v!!, M, y, value); y += 28
            }
            ui.status.coverages.forEach { c ->
                val end = c.end?.let { " until ${Format.date(it)}" } ?: ""
                val svc = c.coverage.servicesTotal?.let { " · ${c.servicesLeft} of $it free services left" } ?: ""
                drawText("• ${c.coverage.label}$end$svc", M, y, label.apply { textSize = 12f }); y += 18
            }
            drawText("Made with Ambr", M, H - M, label)
        }
        doc.finishPage(page)

        ui.details.proofs.sortedBy { it.page }.forEachIndexed { idx, p ->
            val bmp = BitmapFactory.decodeFile(p.filePath, BitmapFactory.Options().apply { inSampleSize = 2 }) ?: return@forEachIndexed
            val pg = doc.startPage(PdfDocument.PageInfo.Builder(W, H, idx + 2).create())
            val box = RectF(M, M + 20, W - M, H - M)
            val scale = minOf(box.width() / bmp.width, box.height() / bmp.height)
            val w = bmp.width * scale
            val h = bmp.height * scale
            pg.canvas.drawText(p.kind.label, M, M, value)
            pg.canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), RectF(box.left, box.top, box.left + w, box.top + h), Paint(Paint.FILTER_BITMAP_FLAG))
            doc.finishPage(pg)
            bmp.recycle()
        }

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val safe = ui.item.name.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifBlank { "item" }
        val file = File(dir, "Ambr-claim-$safe.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
