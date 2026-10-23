package app.ambr

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import app.ambr.platform.Notifications
import app.ambr.ui.nav.AmbrRoot
import app.ambr.ui.theme.AmbrTheme

// FragmentActivity so BiometricPrompt can attach to it.
class MainActivity : FragmentActivity() {
    /** Item to open, set when the app is launched from a reminder. */
    private val openItem = mutableStateOf<Long?>(null)
    private val container get() = (application as AmbrApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readIntent(intent)
        setContent {
            AmbrTheme {
                AmbrRoot(openItem = openItem.value, onOpened = { openItem.value = null })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.lock.onForeground()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) container.lock.onBackground()
    }

    // Our own trips out (camera roll, share sheet, dialer) shouldn't lock the app on return.
    @Deprecated("Deprecated in Java")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        container.lock.leavingForResult()
        super.startActivityForResult(intent, requestCode, options)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        container.lock.leavingForResult()
        super.startActivity(intent, options)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        intent?.getLongExtra(Notifications.EXTRA_ITEM, 0L)?.takeIf { it != 0L }?.let { openItem.value = it }
    }
}
