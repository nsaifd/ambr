package app.ambr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.ambr.ui.nav.AmbrRoot
import app.ambr.ui.theme.AmbrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AmbrTheme {
                AmbrRoot()
            }
        }
    }
}
