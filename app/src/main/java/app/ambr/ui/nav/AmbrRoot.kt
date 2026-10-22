package app.ambr.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.ambr.AmbrApp
import app.ambr.ui.claim.ClaimPackScreen
import app.ambr.ui.claim.ProofViewerScreen
import app.ambr.ui.claim.ShowCounterScreen
import app.ambr.ui.components.TopBar
import app.ambr.ui.edit.EditItemScreen
import app.ambr.ui.edit.SavedScreen
import app.ambr.ui.home.HomeScreen
import app.ambr.ui.item.ItemDetailScreen
import app.ambr.ui.repair.LogRepairScreen
import app.ambr.ui.search.ArchiveScreen
import app.ambr.ui.search.SearchScreen
import app.ambr.ui.search.WhatBrokeScreen
import app.ambr.ui.state.EditorViewModel
import app.ambr.ui.state.ItemViewModel
import app.ambr.ui.state.ItemsViewModel
import app.ambr.ui.state.ambrViewModel
import app.ambr.ui.theme.AmbrColors
import app.ambr.ui.vault.VaultScreen
import app.ambr.ui.you.YouScreen

object Routes {
    const val Add = "add"
    const val Broke = "broke"
    const val Search = "search"
    const val Archive = "archive"
    const val Pro = "pro"
    const val Limit = "limit"
    fun item(id: Long) = "item/$id"
    fun edit(id: Long) = "edit/$id"
    fun claim(id: Long) = "claim/$id"
    fun counter(id: Long) = "counter/$id"
    fun proof(id: Long, page: Int) = "proof/$id/$page"
    fun repair(id: Long) = "repair/$id"
    fun saved(id: Long) = "saved/$id"
}

private val idArg = listOf(navArgument("id") { type = NavType.LongType })
private fun NavBackStackEntry.id() = arguments?.getLong("id") ?: 0L

@Composable
private fun itemVm(entry: NavBackStackEntry): ItemViewModel {
    val id = entry.id()
    return ambrViewModel("item-$id") { ItemViewModel(it.items, id) }
}

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AmbrRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == route }
    val container = (LocalContext.current.applicationContext as AmbrApp).container
    val firstReminder by container.settings.firstReminderDays.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(AmbrColors.Ground)) {
        NavHost(
            navController = nav,
            startDestination = Tab.Home.route,
            enterTransition = { if (targetState.destination.route in tabRoutes) fadeIn(tween(200)) else slideInHorizontally(tween(250)) { it / 4 } + fadeIn(tween(250)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { if (initialState.destination.route in tabRoutes) fadeOut(tween(180)) else slideOutHorizontally(tween(250)) { it / 4 } + fadeOut(tween(200)) },
        ) {
            composable(Tab.Home.route) {
                HomeScreen(
                    vm = ambrViewModel { ItemsViewModel(it.items) },
                    onOpenItem = { nav.navigate(Routes.item(it)) },
                    onSomethingBroke = { nav.navigate(Routes.Broke) },
                    onSearch = { nav.navigate(Routes.Search) },
                    onSeeAll = { nav.goTab(Tab.Vault.route) },
                    onAdd = { nav.navigate(Routes.Add) },
                )
            }
            composable(Tab.Vault.route) {
                VaultScreen(
                    vm = ambrViewModel { ItemsViewModel(it.items) },
                    onOpenItem = { nav.navigate(Routes.item(it)) },
                    onSearch = { nav.navigate(Routes.Search) },
                    onArchive = { nav.navigate(Routes.Archive) },
                )
            }
            composable(Tab.You.route) {
                YouScreen(
                    vm = ambrViewModel { ItemsViewModel(it.items) },
                    settings = container.settings,
                    onBackup = { nav.navigate("backup") },
                    onPro = { nav.navigate(Routes.Pro) },
                    onReminders = { nav.navigate("reminders") },
                    onAppLock = { nav.navigate("applock") },
                    onExport = { nav.navigate("export") },
                    onPrivacy = { nav.navigate("privacy") },
                    onArchive = { nav.navigate(Routes.Archive) },
                    onHelp = { nav.navigate("help") },
                    onRate = { nav.navigate("rate") },
                    onAbout = { nav.navigate("about") },
                )
            }
            composable(Routes.Add) {
                // The camera step lands here next; until then, Add opens the form directly.
                EditorRoute(nav, 0L, container.pendingDraft.also { container.pendingDraft = null })
            }
            composable("edit/{id}", idArg) { EditorRoute(nav, it.id(), null) }
            composable("item/{id}", idArg) { e ->
                val id = e.id()
                ItemDetailScreen(
                    vm = itemVm(e),
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.edit(id)) },
                    onClaim = { nav.navigate(Routes.claim(id)) },
                    onLogRepair = { nav.navigate(Routes.repair(id)) },
                    onAddProof = { nav.navigate(Routes.Add) },
                    onOpenProof = { nav.navigate(Routes.proof(id, it)) },
                )
            }
            composable("claim/{id}", idArg) { e ->
                val id = e.id()
                ClaimPackScreen(itemVm(e), onBack = { nav.popBackStack() }, onCounter = { nav.navigate(Routes.counter(id)) }, onOpenProof = { nav.navigate(Routes.proof(id, it)) }, onAddProof = { nav.navigate(Routes.Add) })
            }
            composable("counter/{id}", idArg) { e -> ShowCounterScreen(itemVm(e), onClose = { nav.popBackStack() }) }
            composable("proof/{id}/{page}", idArg + navArgument("page") { type = NavType.IntType }) { e ->
                val vm = itemVm(e)
                ProofViewerScreen(vm, e.arguments?.getInt("page") ?: 0, onClose = { nav.popBackStack() }, onDelete = { vm.removeProof(it) })
            }
            composable("repair/{id}", idArg) { e -> LogRepairScreen(itemVm(e), onBack = { nav.popBackStack() }) }
            composable("saved/{id}", idArg) { e ->
                SavedScreen(
                    itemVm(e), firstReminder,
                    onAddAnother = { nav.navigate(Routes.Add) { popUpTo(Tab.Home.route) } },
                    onDone = { nav.navigate(Routes.item(e.id())) { popUpTo(Tab.Home.route) } },
                )
            }
            composable(Routes.Broke) {
                WhatBrokeScreen(
                    ambrViewModel { ItemsViewModel(it.items) },
                    onBack = { nav.popBackStack() },
                    onSearch = { nav.navigate(Routes.Search) },
                    onPick = { nav.navigate(Routes.claim(it)) },
                    onAll = { nav.goTab(Tab.Vault.route) },
                )
            }
            composable(Routes.Search) { SearchScreen(ambrViewModel { ItemsViewModel(it.items) }, onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.item(it)) }) }
            composable(Routes.Archive) { ArchiveScreen(ambrViewModel { ItemsViewModel(it.items) }, onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.item(it)) }) }
            listOf(Routes.Pro, Routes.Limit, "backup", "reminders", "applock", "export", "privacy", "help", "rate", "about").forEach { r ->
                composable(r) { Placeholder(r.replaceFirstChar { it.uppercase() }) { nav.popBackStack() } }
            }
        }
        if (currentTab != null) {
            FloatingNavBar(
                current = currentTab,
                onSelect = { nav.goTab(it.route) },
                onAdd = { nav.navigate(Routes.Add) },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            )
        }
    }
}

private val tabRoutes = Tab.entries.map { it.route }.toSet()

@Composable
private fun EditorRoute(nav: NavHostController, id: Long, pending: app.ambr.ui.state.Draft?) {
    val vm = ambrViewModel("editor-$id") { EditorViewModel(it.items, it.settings, id, pending) }
    val back = { vm.discard(); nav.popBackStack() }
    androidx.activity.compose.BackHandler { back() }
    EditItemScreen(
        vm = vm,
        onBack = { back() },
        onSaved = { saved -> if (id == 0L) nav.navigate(Routes.saved(saved)) { popUpTo(Routes.Add) { inclusive = true } } else nav.popBackStack() },
        onLimit = { nav.navigate(Routes.Limit) },
        onDelete = null,
        onScanSerial = null,
    )
}

// Stand-in until each screen is built.
@Composable
private fun Placeholder(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        TopBar(title, onBack)
        Text("Coming in the next build.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary, modifier = Modifier.padding(top = 16.dp))
    }
}
