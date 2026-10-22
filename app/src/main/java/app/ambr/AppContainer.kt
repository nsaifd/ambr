package app.ambr

import android.content.Context
import app.ambr.data.AmbrDatabase
import app.ambr.data.ItemRepository
import app.ambr.platform.Settings
import app.ambr.ui.state.Draft

/** Hand-wired dependencies; small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val database: AmbrDatabase = AmbrDatabase.create(context)
    val items = ItemRepository(database.items())
    val settings = Settings(context)
    val files = java.io.File(context.filesDir, "proofs").apply { mkdirs() }

    /** What the camera captured, waiting for the Confirm form. Cleared once saved or discarded. */
    var pendingDraft: Draft? = null
}
