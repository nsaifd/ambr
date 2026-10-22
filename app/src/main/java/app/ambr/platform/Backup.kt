package app.ambr.platform

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import app.ambr.data.ArchiveReason
import app.ambr.data.Category
import app.ambr.data.CoverageEntity
import app.ambr.data.CoverageType
import app.ambr.data.ItemEntity
import app.ambr.data.ItemRepository
import app.ambr.data.ItemWithDetails
import app.ambr.data.ProofEntity
import app.ambr.data.ProofKind
import app.ambr.data.RepairEntity
import app.ambr.data.RepairOutcome
import app.ambr.data.Room
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupInfo(val id: String, val modified: String, val items: Int, val photos: Int, val sizeBytes: Long)

/** Result of asking Google for Drive access: a token, or a screen the user must see first. */
sealed interface DriveAccess {
    data class Token(val value: String) : DriveAccess
    data class NeedsConsent(val intent: PendingIntent) : DriveAccess
    data class Failed(val message: String) : DriveAccess
}

/**
 * Backup to the hidden app-data folder of the user's own Google Drive. The file is a zip with
 * backup.json (every item, coverage, proof and repair) and the proof photos.
 */
class Backup(private val context: Context, private val repo: ItemRepository, private val settings: Settings, private val files: File) {
    companion object {
        private const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val NAME = "ambr-backup.zip"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }

    /** From an Activity this can show Google's account picker; from a worker it only succeeds silently. */
    suspend fun access(activity: Activity? = null): DriveAccess = runCatching {
        val client = if (activity != null) Identity.getAuthorizationClient(activity) else Identity.getAuthorizationClient(context)
        val result = client.authorize(AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build()).await()
        when {
            result.hasResolution() -> DriveAccess.NeedsConsent(result.pendingIntent!!)
            result.accessToken != null -> DriveAccess.Token(result.accessToken!!)
            else -> DriveAccess.Failed("Google didn't give access")
        }
    }.getOrElse { DriveAccess.Failed(it.message ?: "Couldn't reach Google") }

    /** Called with the data returned from the consent screen. */
    fun tokenFrom(data: android.content.Intent?): String? =
        runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken }.getOrNull()

    suspend fun accountEmail(token: String): String? = withContext(Dispatchers.IO) {
        runCatching { JSONObject(request("GET", "$API/about?fields=user(emailAddress)", token)).getJSONObject("user").getString("emailAddress") }.getOrNull()
    }

    suspend fun find(token: String): BackupInfo? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("name='$NAME'", "UTF-8")
        val json = JSONObject(request("GET", "$API/files?spaces=appDataFolder&q=$q&fields=files(id,modifiedTime,size,appProperties)&orderBy=modifiedTime%20desc", token))
        val f = json.getJSONArray("files").optJSONObject(0) ?: return@withContext null
        val props = f.optJSONObject("appProperties")
        BackupInfo(f.getString("id"), f.optString("modifiedTime"), props?.optString("items")?.toIntOrNull() ?: 0, props?.optString("photos")?.toIntOrNull() ?: 0, f.optString("size").toLongOrNull() ?: 0)
    }

    /** Builds the zip and uploads it, replacing the previous backup. */
    suspend fun backUp(token: String, withPhotos: Boolean = settings.backupPhotos) = withContext(Dispatchers.IO) {
        val all = repo.activeOnce() + repo.archivedOnce()
        val zip = File(context.cacheDir, NAME)
        var photos = 0
        ZipOutputStream(zip.outputStream().buffered()).use { out ->
            out.putNextEntry(ZipEntry("backup.json"))
            out.write(toJson(all).toString().toByteArray())
            out.closeEntry()
            if (withPhotos) all.flatMap { it.proofs.map { p -> File(p.filePath) } + listOfNotNull(it.item.photoPath?.let(::File)) }.filter { it.exists() }.forEach { f ->
                out.putNextEntry(ZipEntry("photos/${f.name}"))
                f.inputStream().use { it.copyTo(out) }
                out.closeEntry()
                photos++
            }
        }
        val meta = JSONObject().put("name", NAME).put("appProperties", JSONObject().put("items", all.size.toString()).put("photos", photos.toString()))
        val existing = find(token)
        if (existing == null) meta.put("parents", JSONArray().put("appDataFolder"))
        val url = if (existing == null) "$UPLOAD/files?uploadType=multipart" else "$UPLOAD/files/${existing.id}?uploadType=multipart"
        multipart(if (existing == null) "POST" else "PATCH", url, token, meta, zip)
        zip.delete()
        settings.setLastBackup(System.currentTimeMillis())
    }

    /** Downloads the backup and adds every item to this phone. Items already here are kept. */
    suspend fun restore(token: String, info: BackupInfo, onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
        val zip = File(context.cacheDir, NAME)
        val conn = open("GET", "$API/files/${info.id}?alt=media", token)
        check(conn.responseCode in 200..299) { "Download failed (${conn.responseCode})" }
        conn.inputStream.use { input -> zip.outputStream().use { input.copyTo(it) } }
        var json: JSONObject? = null
        ZipInputStream(zip.inputStream().buffered()).use { zin ->
            generateSequence { zin.nextEntry }.forEach { e ->
                when {
                    e.name == "backup.json" -> json = JSONObject(zin.readBytes().decodeToString())
                    e.name.startsWith("photos/") -> File(files, File(e.name).name).outputStream().use { zin.copyTo(it) }
                }
            }
        }
        zip.delete()
        val items = json?.getJSONArray("items") ?: return@withContext
        for (i in 0 until items.length()) {
            fromJson(items.getJSONObject(i))
            onProgress(i + 1, items.length())
        }
    }

    // --- JSON ---

    private fun JSONObject.str(k: String) = if (isNull(k) || !has(k)) null else getString(k)
    private fun JSONObject.long(k: String) = if (isNull(k) || !has(k)) null else getLong(k)

    private fun toJson(all: List<ItemWithDetails>) = JSONObject().put("version", 1).put("createdAt", System.currentTimeMillis()).put("items", JSONArray().apply {
        all.forEach { d ->
            val i = d.item
            put(JSONObject()
                .put("name", i.name).put("category", i.category.name).put("room", i.room.name).put("brand", i.brand).put("model", i.model)
                .put("serial", i.serial).put("priceTaka", i.priceTaka).put("purchaseDate", i.purchaseDate?.toString()).put("store", i.store)
                .put("photo", i.photoPath?.let { File(it).name }).put("notes", i.notes).put("serviceName", i.serviceName).put("servicePhone", i.servicePhone)
                .put("serviceAddress", i.serviceAddress).put("archivedReason", i.archivedReason?.name).put("archivedOn", i.archivedOn?.toString()).put("createdAt", i.createdAt)
                .put("coverages", JSONArray().apply { d.coverages.forEach { c ->
                    put(JSONObject().put("type", c.type.name).put("label", c.label).put("startDate", c.startDate.toString()).put("months", c.months)
                        .put("servicesTotal", c.servicesTotal).put("servicesUsed", c.servicesUsed).put("remindersSent", c.remindersSent).put("reminderDismissed", c.reminderDismissed))
                } })
                .put("proofs", JSONArray().apply { d.proofs.forEach { p ->
                    put(JSONObject().put("kind", p.kind.name).put("file", File(p.filePath).name).put("page", p.page).put("ocrText", p.ocrText).put("createdAt", p.createdAt))
                } })
                .put("repairs", JSONArray().apply { d.repairs.forEach { r ->
                    put(JSONObject().put("date", r.date.toString()).put("problem", r.problem).put("outcome", r.outcome.name).put("costTaka", r.costTaka).put("notes", r.notes))
                } }))
        }
    })

    private suspend fun fromJson(o: JSONObject) {
        val item = ItemEntity(
            name = o.getString("name"), category = runCatching { Category.valueOf(o.getString("category")) }.getOrDefault(Category.Other),
            room = runCatching { Room.valueOf(o.getString("room")) }.getOrDefault(Room.Other), brand = o.str("brand"), model = o.str("model"),
            serial = o.str("serial"), priceTaka = o.long("priceTaka"), purchaseDate = o.str("purchaseDate")?.let(LocalDate::parse), store = o.str("store"),
            photoPath = o.str("photo")?.let { File(files, it).path }, notes = o.str("notes"), serviceName = o.str("serviceName"),
            servicePhone = o.str("servicePhone"), serviceAddress = o.str("serviceAddress"),
            archivedReason = o.str("archivedReason")?.let { runCatching { ArchiveReason.valueOf(it) }.getOrNull() }, archivedOn = o.str("archivedOn")?.let(LocalDate::parse),
            createdAt = o.long("createdAt") ?: System.currentTimeMillis(),
        )
        val cov = o.optJSONArray("coverages") ?: JSONArray()
        val coverages = (0 until cov.length()).map { cov.getJSONObject(it) }.map { c ->
            CoverageEntity(itemId = 0, type = runCatching { CoverageType.valueOf(c.getString("type")) }.getOrDefault(CoverageType.Manufacturer),
                label = c.getString("label"), startDate = LocalDate.parse(c.getString("startDate")), months = c.getInt("months"),
                servicesTotal = if (c.isNull("servicesTotal") || !c.has("servicesTotal")) null else c.getInt("servicesTotal"), servicesUsed = c.optInt("servicesUsed"),
                remindersSent = c.optInt("remindersSent"), reminderDismissed = c.optBoolean("reminderDismissed"))
        }
        val pr = o.optJSONArray("proofs") ?: JSONArray()
        val proofs = (0 until pr.length()).map { pr.getJSONObject(it) }.mapNotNull { p ->
            val f = File(files, p.getString("file"))
            if (!f.exists()) return@mapNotNull null
            ProofEntity(itemId = 0, kind = runCatching { ProofKind.valueOf(p.getString("kind")) }.getOrDefault(ProofKind.Receipt), filePath = f.path, page = p.optInt("page"), ocrText = p.str("ocrText"))
        }
        val id = repo.save(item, coverages, proofs)
        val rp = o.optJSONArray("repairs") ?: JSONArray()
        (0 until rp.length()).map { rp.getJSONObject(it) }.forEach { r ->
            repo.logRepair(RepairEntity(itemId = id, date = LocalDate.parse(r.getString("date")), problem = r.getString("problem"),
                outcome = runCatching { RepairOutcome.valueOf(r.getString("outcome")) }.getOrDefault(RepairOutcome.Fixed), costTaka = r.optLong("costTaka"), notes = r.str("notes")))
        }
    }

    // --- HTTP ---

    private fun open(method: String, url: String, token: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            if (method == "PATCH") { requestMethod = "POST"; setRequestProperty("X-HTTP-Method-Override", "PATCH") } else requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 20_000
            readTimeout = 60_000
        }

    private fun request(method: String, url: String, token: String): String {
        val c = open(method, url, token)
        if (c.responseCode !in 200..299) error("Drive error ${c.responseCode}: ${c.errorStream?.bufferedReader()?.readText()?.take(200)}")
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun multipart(method: String, url: String, token: String, meta: JSONObject, file: File) {
        val boundary = "ambr${System.nanoTime()}"
        val c = open(method, url, token).apply {
            doOutput = true
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            setChunkedStreamingMode(64 * 1024)
        }
        c.outputStream.buffered().use { out ->
            out.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${meta}\r\n--$boundary\r\nContent-Type: application/zip\r\n\r\n".toByteArray())
            file.inputStream().use { it.copyTo(out) }
            out.write("\r\n--$boundary--\r\n".toByteArray())
        }
        if (c.responseCode !in 200..299) {
            val body = c.errorStream?.bufferedReader()?.readText().orEmpty()
            if ("storageQuotaExceeded" in body) error("quota")
            error("Upload failed (${c.responseCode})")
        }
        c.inputStream.close()
    }
}
