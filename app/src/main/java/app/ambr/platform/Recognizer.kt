package app.ambr.platform

import android.content.Context
import android.net.Uri
import app.ambr.domain.OcrLayout
import app.ambr.domain.OcrLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/** On-device text reading. Nothing leaves the phone. */
object Recognizer {
    private val client by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /** Returns the page as text, one printed row per line. */
    suspend fun read(context: Context, file: File): String = runCatching {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val result = client.process(image).await()
        val lines = result.textBlocks.flatMap { it.lines }.mapNotNull { l ->
            l.boundingBox?.let { b -> OcrLine(l.text, b.left, b.top, b.right, b.bottom) }
        }
        OcrLayout.rows(lines).joinToString("\n").ifBlank { result.text }
    }.getOrDefault("")
}
