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
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.ambr.ui.capture.BarcodeScanScreen
import app.ambr.ui.capture.CaptureFlow
import app.ambr.ui.capture.CaptureResult
import app.ambr.ui.capture.CaptureViewModel
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
import app.ambr.ui.backup.BackupScreen
import app.ambr.ui.backup.RestoreScreen
import app.ambr.ui.backup.WelcomeScreen
import app.ambr.ui.pro.LimitReachedScreen
import app.ambr.ui.pro.ProScreen
import app.ambr.ui.lock.AppLockSetupScreen
import app.ambr.ui.lock.LockScreen
import app.ambr.ui.you.AboutScreen
import app.ambr.ui.you.ExportScreen
import app.ambr.ui.you.FeedbackScreen
import app.ambr.ui.you.HelpScreen
import app.ambr.ui.you.PrivacyScreen
import app.ambr.ui.you.openPlayStore
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import app.ambr.ui.you.YouScreen

object Routes {
    const val Add = "add"
    const val Broke = "broke"
    const val Search = "search"
    const val Archive = "archive"
    const val Pro = "pro"
    const val Limit = "limit"
    const val Confirm = "confirm"
    const val Scan = "scan"
    fun addPages(id: Long) = "add?item=$id"
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
    return ambrViewModel("item-$id") { ItemViewModel(it.items, id, it.scope) }
}

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AmbrRoot(openItem: Long? = null, onOpened: () -> Unit = {}) {
    val nav = rememberNavController()
    LaunchedEffect(openItem) {
        openItem?.let { nav.navigate(Routes.item(it)) { launchSingleTop = true }; onOpened() }
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val currentTab = Tab.entries.firstOrNull { it.route == route }
    val context = LocalContext.current
    val container = (context.applicationContext as AmbrApp).container
    val locked by container.lock.locked.collectAsStateWithLifecycle()
    var showWelcome by remember { mutableStateOf(!container.settings.welcomed) }
    // Keep receipts out of screenshots and the recent-apps preview while app lock is on.
    val lockOn by container.settings.appLock.collectAsStateWithLifecycle()
    LaunchedEffect(lockOn) {
        val window = (context as? android.app.Activity)?.window ?: return@LaunchedEffect
        if (lockOn) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }
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
                    onNotifications = { nav.navigate("notifications") },
                )
            }
            composable("notifications") {
                app.ambr.ui.home.NotificationsScreen(
                    ambrViewModel { ItemsViewModel(it.items) }, container.settings,
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate(Routes.item(it)) },
                    onBackup = { nav.navigate("backup") },
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
                    onRate = { openPlayStore(context) },
                    onAbout = { nav.navigate("about") },
                )
            }
            composable("add?item={item}", listOf(navArgument("item") { type = NavType.LongType; defaultValue = 0L })) { e ->
                val itemId = e.arguments?.getLong("item") ?: 0L
                val vm = ambrViewModel("capture-$itemId") { CaptureViewModel(it, itemId) }
                var duplicate by remember { mutableStateOf<CaptureResult.NewItem?>(null) }
                val toConfirm = { draft: app.ambr.ui.state.Draft? ->
                    container.pendingDraft = draft
                    nav.navigate(Routes.Confirm) { popUpTo("add?item={item}") { inclusive = true } }
                }
                CaptureFlow(
                    vm,
                    onClose = { nav.popBackStack() },
                    onTypeIt = { toConfirm(null) },
                    onResult = { r ->
                        when (r) {
                            CaptureResult.Added -> nav.popBackStack()
                            is CaptureResult.NewItem -> if (r.duplicateOf != null) duplicate = r else toConfirm(r.draft)
                        }
                    },
                )
                duplicate?.let { d ->
                    val dup = d.duplicateOf ?: return@let
                    DuplicateDialog(
                        dup,
                        onOpen = { d.draft.newProofs.forEach { java.io.File(it.filePath).delete() }; nav.navigate(Routes.item(dup.item.id)) { popUpTo("add?item={item}") { inclusive = true } } },
                        onCancel = { d.draft.newProofs.forEach { java.io.File(it.filePath).delete() }; nav.popBackStack() },
                        onSaveAnyway = { toConfirm(d.draft) },
                    )
                }
            }
            composable(Routes.Confirm) { EditorRoute(nav, 0L, container.pendingDraft.also { container.pendingDraft = null }, it) }
            composable("reminders") { app.ambr.ui.you.RemindersScreen(container.settings, onBack = { nav.popBackStack() }) }
            composable(Routes.Scan) {
                BarcodeScanScreen { code ->
                    code?.let { nav.previousBackStackEntry?.savedStateHandle?.set("serial", it) }
                    nav.popBackStack()
                }
            }
            composable("edit/{id}", idArg) { EditorRoute(nav, it.id(), null, it) }
            composable("item/{id}", idArg) { e ->
                val id = e.id()
                ItemDetailScreen(
                    vm = itemVm(e),
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.edit(id)) },
                    onClaim = { nav.navigate(Routes.claim(id)) },
                    onLogRepair = { nav.navigate(Routes.repair(id)) },
                    onAddProof = { nav.navigate(Routes.addPages(id)) },
                    onOpenProof = { nav.navigate(Routes.proof(id, it)) },
                )
            }
            composable("claim/{id}", idArg) { e ->
                val id = e.id()
                ClaimPackScreen(itemVm(e), onBack = { nav.popBackStack() }, onCounter = { nav.navigate(Routes.counter(id)) }, onOpenProof = { nav.navigate(Routes.proof(id, it)) }, onAddProof = { nav.navigate(Routes.addPages(id)) })
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
            composable("applock") {
                val pro by container.settings.isPro.collectAsStateWithLifecycle()
                AppLockSetupScreen(container.lock, container.settings, pro, onGetPro = { nav.navigate(Routes.Pro) }, onBack = { nav.popBackStack() })
            }
            composable("export") {
                val pro by container.settings.isPro.collectAsStateWithLifecycle()
                ExportScreen(ambrViewModel { ItemsViewModel(it.items) }, pro, onGetPro = { nav.navigate(Routes.Pro) }, onBack = { nav.popBackStack() })
            }
            composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }) }
            composable("help") { HelpScreen(onBack = { nav.popBackStack() }, onFeedback = { nav.navigate("feedback") }) }
            composable("feedback") { FeedbackScreen(onBack = { nav.popBackStack() }) }
            composable("about") {
                val scope = rememberCoroutineScope()
                AboutScreen(onBack = { nav.popBackStack() }, onDeleteAll = { scope.launch { container.wipe() } })
            }
            composable(Routes.Pro) { ProScreen(container.billing, container.settings, onClose = { nav.popBackStack() }) }
            composable(Routes.Limit) {
                LimitReachedScreen(
                    ambrViewModel { ItemsViewModel(it.items) },
                    onPro = { nav.navigate(Routes.Pro) { popUpTo(Routes.Limit) { inclusive = true } } },
                    onArchive = { nav.goTab(Tab.Vault.route) },
                    onClose = { nav.popBackStack() },
                )
            }
            composable("backup") { BackupScreen(container, onBack = { nav.popBackStack() }, onRestore = { nav.navigate("restore") }) }
            composable("restore") {
                RestoreScreen(container, onClose = { nav.popBackStack() }, onDone = { nav.popBackStack(Tab.Home.route, inclusive = false) })
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
        // First launch: Welcome sits over Home, so Home stays the root of every back stack.
        if (showWelcome) {
            val done = { container.settings.welcomed = true; showWelcome = false }
            WelcomeScreen(
                onAdd = { done(); nav.navigate(Routes.Add) },
                onExplore = { done() },
                onRestore = { done(); nav.navigate("restore") },
            )
        }
        if (locked) {
            androidx.activity.compose.BackHandler { (context as? android.app.Activity)?.moveTaskToBack(true) }
            LockScreen(container.lock, container.settings)
        }
    }
}

private val tabRoutes = Tab.entries.map { it.route }.toSet()

@Composable
private fun EditorRoute(nav: NavHostController, id: Long, pending: app.ambr.ui.state.Draft?, entry: NavBackStackEntry? = null) {
    val vm = ambrViewModel("editor-$id") { EditorViewModel(it.items, it.settings, id, pending) }
    val back = { vm.discard(); nav.popBackStack() }
    var confirmDelete by remember { mutableStateOf(false) }
    val here = entry ?: nav.currentBackStackEntry
    val scanned by (here?.savedStateHandle?.getStateFlow<String?>("serial", null) ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsStateWithLifecycle()
    LaunchedEffect(scanned) {
        scanned?.let { code -> vm.edit(app.ambr.ui.state.Field.Serial) { it.copy(serial = code) }; here?.savedStateHandle?.remove<String>("serial") }
    }
    androidx.activity.compose.BackHandler { back() }
    EditItemScreen(
        vm = vm,
        onBack = { back() },
        onSaved = { saved -> if (id == 0L) nav.navigate(Routes.saved(saved)) { popUpTo(Routes.Confirm) { inclusive = true } } else nav.popBackStack() },
        onLimit = { nav.navigate(Routes.Limit) },
        onDelete = if (id != 0L) ({ confirmDelete = true }) else null,
        onScanSerial = { nav.navigate(Routes.Scan) },
    )
    if (confirmDelete) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmDelete = false },
        containerColor = AmbrColors.Card,
        title = { Text("Delete this item?") },
        text = { Text("Its receipt, warranty card and service history are removed from this phone. This can't be undone.") },
        confirmButton = {
            androidx.compose.material3.TextButton({ confirmDelete = false; vm.delete { nav.popBackStack(Tab.Home.route, inclusive = false) } }) {
                Text("Delete", color = AmbrColors.Danger, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        dismissButton = { androidx.compose.material3.TextButton({ confirmDelete = false }) { Text("Keep it", color = AmbrColors.TextPrimary) } },
    )
}

@Composable
private fun DuplicateDialog(existing: app.ambr.data.ItemWithDetails, onOpen: () -> Unit, onCancel: () -> Unit, onSaveAnyway: () -> Unit) {
    val item = existing.item
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        containerColor = AmbrColors.Card,
        title = { Text("Saved this before?") },
        text = {
            Text(
                "Looks like this receipt is already saved as ${item.name}" +
                    (java.time.Instant.ofEpochMilli(item.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().let { ", on ${app.ambr.domain.Format.date(it)}" }) + ".",
            )
        },
        confirmButton = { androidx.compose.material3.TextButton(onSaveAnyway) { Text("Save anyway", color = AmbrColors.HoneyInk, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) } },
        dismissButton = {
            Row {
                androidx.compose.material3.TextButton(onCancel) { Text("Cancel", color = AmbrColors.TextSecondary) }
                androidx.compose.material3.TextButton(onOpen) { Text("Open it", color = AmbrColors.TextPrimary) }
            }
        },
    )
}
