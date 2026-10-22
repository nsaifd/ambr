package app.ambr.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Proof photos are stored as upright JPEGs, no larger than needed to read small print. */
object Images {
    private const val MAX_SIDE = 2200
    private const val QUALITY = 85

    fun newFile(dir: File): File = File(dir, "${UUID.randomUUID()}.jpg")

    /** Rewrites a camera JPEG: applies its EXIF rotation and scales it down. */
    fun normalize(file: File) {
        val rotation = runCatching { ExifInterface(file).rotationDegrees }.getOrDefault(0)
        val bmp = decodeScaled(file) ?: return
        write(rotate(bmp, rotation), file)
    }

    fun rotate(file: File, degrees: Int) {
        val bmp = BitmapFactory.decodeFile(file.path) ?: return
        write(rotate(bmp, degrees), file)
    }

    /** Copies a picked image into the proofs folder. */
    fun import(context: Context, uri: Uri, dir: File): File? {
        val out = newFile(dir)
        context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } } ?: return null
        normalize(out)
        return out.takeIf { it.length() > 0 }
    }

    /** Renders the first pages of a PDF (an emailed invoice, say) into images. */
    fun importPdf(context: Context, uri: Uri, dir: File, maxPages: Int = 5): List<File> {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
        return pfd.use {
            PdfRenderer(it).use { renderer ->
                (0 until minOf(renderer.pageCount, maxPages)).map { index ->
                    renderer.openPage(index).use { page ->
                        val scale = (MAX_SIDE.toFloat() / maxOf(page.width, page.height)).coerceAtMost(3f)
                        val bmp = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        newFile(dir).also { f -> write(bmp, f) }
                    }
                }
            }
        }
    }

    private fun decodeScaled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val side = maxOf(bmp.width, bmp.height)
        if (side <= MAX_SIDE) return bmp
        val f = MAX_SIDE.toFloat() / side
        return Bitmap.createScaledBitmap(bmp, (bmp.width * f).toInt(), (bmp.height * f).toInt(), true).also { if (it !== bmp) bmp.recycle() }
    }

    private fun rotate(bmp: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bmp
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true).also { if (it !== bmp) bmp.recycle() }
    }

    private fun write(bmp: Bitmap, file: File) {
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        bmp.recycle()
    }
}
