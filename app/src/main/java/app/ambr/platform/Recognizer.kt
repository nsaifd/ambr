package app.ambr.platform

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/** On-device text reading. Nothing leaves the phone. */
object Recognizer {
    private val client by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun read(context: Context, file: File): String = runCatching {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        // Keep ML Kit's line order within each block; blocks come roughly top to bottom.
        client.process(image).await().textBlocks.sortedBy { it.boundingBox?.top ?: 0 }.joinToString("\n") { it.text }
    }.getOrDefault("")
}
