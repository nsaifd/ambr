package app.ambr.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object Intents {
    fun uriFor(context: Context, file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun dial(context: Context, phone: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })))
    }

    fun shareFiles(context: Context, files: List<File>, text: String, mime: String = "image/*") {
        val uris = ArrayList(files.filter { it.exists() }.map { uriFor(context, it) })
        val intent = when {
            uris.isEmpty() -> Intent(Intent.ACTION_SEND).setType("text/plain")
            uris.size == 1 -> Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uris[0])
            else -> Intent(Intent.ACTION_SEND_MULTIPLE).setType(mime).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }.putExtra(Intent.EXTRA_TEXT, text).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun copy(context: Context, label: String, value: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        // Android 13+ shows its own confirmation.
        if (android.os.Build.VERSION.SDK_INT < 33) Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }
}
