package app.ambr

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.ambr.platform.Notifications
import app.ambr.ui.nav.AmbrRoot
import app.ambr.ui.theme.AmbrTheme

class MainActivity : ComponentActivity() {
    /** Item to open, set when the app is launched from a reminder. */
    private val openItem = mutableStateOf<Long?>(null)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        intent?.getLongExtra(Notifications.EXTRA_ITEM, 0L)?.takeIf { it != 0L }?.let { openItem.value = it }
    }
}
