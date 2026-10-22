package app.ambr

import android.content.Context
import app.ambr.data.AmbrDatabase
import app.ambr.data.ItemRepository

/** Hand-wired dependencies; small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val database: AmbrDatabase = AmbrDatabase.create(context)
    val items = ItemRepository(database.items())
}
