package app.ambr.platform

import android.graphics.BitmapFactory
import app.ambr.domain.OcrLayout
import app.ambr.domain.OcrLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/** On-device text reading. Nothing leaves the phone. */
object Recognizer {
    private val client by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /**
     * Returns the page as text, one printed row per line. People often photograph a receipt
     * sideways; when the text runs sideways or upside down, the photo is turned upright on
     * disk and read again, because rows can only be rebuilt from upright text.
     */
    suspend fun read(file: File): String = runCatching {
        var result = process(file) ?: return@runCatching ""
        val turn = uprightTurn(result)
        if (turn != 0) {
            withContext(Dispatchers.IO) { Images.rotate(file, turn) }
            val again = process(file)
            // Keep the turn only if the text is now upright; otherwise undo it.
            if (again != null && uprightTurn(again) == 0) result = again
            else withContext(Dispatchers.IO) { Images.rotate(file, 360 - turn) }
        }
        val lines = result.textBlocks.flatMap { it.lines }.mapNotNull { l ->
            l.boundingBox?.let { b -> OcrLine(l.text, b.left, b.top, b.right, b.bottom) }
        }
        OcrLayout.rows(lines).joinToString("\n").ifBlank { result.text }
    }.getOrDefault("")

    private suspend fun process(file: File): Text? {
        val bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path) } ?: return null
        return try { client.process(InputImage.fromBitmap(bitmap, 0)).await() } finally { bitmap.recycle() }
    }

    /** Clockwise turn (0, 90, 180, 270) that makes most of the text horizontal. */
    private fun uprightTurn(text: Text): Int {
        // Weight by length so the shop's long lines count more than stray marks.
        val lines = text.textBlocks.flatMap { it.lines }.filter { it.text.length >= 4 }
        if (lines.size < 3) return 0
        val sorted = lines.flatMap { l -> List(l.text.length.coerceAtMost(40)) { l.angle } }.sorted()
        val angle = sorted[sorted.size / 2]
        if (abs(angle) <= 35f) return 0
        // ML Kit reports the clockwise angle of the text; turning the photo the other way undoes it.
        val snapped = (Math.round(angle / 90f) * 90 + 360) % 360
        return (360 - snapped) % 360
    }
}
