package app.ambr.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.ambr.ui.theme.AmbrColors

object Routes {
    const val Add = "add"
}

@Composable
fun AmbrRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == route }

    Box(Modifier.fillMaxSize().background(AmbrColors.Ground)) {
        NavHost(navController = nav, startDestination = Tab.Home.route) {
            composable(Tab.Home.route) { Placeholder("Home") }
            composable(Tab.Vault.route) { Placeholder("Vault") }
            composable(Tab.You.route) { Placeholder("You") }
            composable(Routes.Add) { Placeholder("Camera") }
        }
        if (currentTab != null) {
            FloatingNavBar(
                current = currentTab,
                onSelect = { tab ->
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAdd = { nav.navigate(Routes.Add) },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            )
        }
    }
}

// Stand-in until each screen is built.
@Composable
private fun Placeholder(title: String) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary)
    }
}
