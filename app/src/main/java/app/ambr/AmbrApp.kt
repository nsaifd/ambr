package app.ambr

import android.app.Application
import app.ambr.platform.Notifications
import app.ambr.platform.ReminderWorker

class AmbrApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannel(this)
        ReminderWorker.schedule(this)
    }
}
