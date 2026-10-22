package app.ambr.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.ambr.AmbrApp
import app.ambr.AppContainer

@Composable
inline fun <reified VM : ViewModel> ambrViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val app = LocalContext.current.applicationContext as AmbrApp
    return viewModel(key = key, factory = viewModelFactory { initializer { create(app.container) } })
}
