package app.ambr

import android.content.Context
import app.ambr.data.AmbrDatabase
import app.ambr.data.ItemRepository
import app.ambr.platform.AppLock
import app.ambr.platform.Backup
import app.ambr.platform.Billing
import app.ambr.platform.Settings
import app.ambr.ui.state.Draft

/** Hand-wired dependencies; small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val database: AmbrDatabase = AmbrDatabase.create(context)
    val items = ItemRepository(database.items())
    val settings = Settings(context)

    /** For saves that must finish even if the screen that started them closes. */
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    val lock = AppLock(settings)
    val billing = Billing(context.applicationContext, settings)
    val files = java.io.File(context.filesDir, "proofs").apply { mkdirs() }

    val backup = Backup(context.applicationContext, items, settings, files)

    /** What the camera captured, waiting for the Confirm form. Cleared once saved or discarded. */
    var pendingDraft: Draft? = null

    /** "Delete all my data": wipes this phone's copy, then restarts the app fresh. */
    suspend fun wipe() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            database.clearAllTables()
            files.deleteRecursively()
            java.io.File(app.cacheDir, "shared").deleteRecursively()
            app.getSharedPreferences("ambr", Context.MODE_PRIVATE).edit().clear().commit()
        }
        val restart = app.packageManager.getLaunchIntentForPackage(app.packageName)!!
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        app.startActivity(restart)
        Runtime.getRuntime().exit(0)
    }
}
