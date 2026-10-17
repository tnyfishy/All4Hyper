package io.github.tnyfishy.all4hyper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.tnyfishy.all4hyper.ui.All4HyperTheme
import io.github.tnyfishy.all4hyper.ui.FloatingBarHeight
import io.github.tnyfishy.all4hyper.ui.FloatingNavBar
import io.github.tnyfishy.all4hyper.ui.GoogleActions
import io.github.tnyfishy.all4hyper.ui.HomeActions
import io.github.tnyfishy.all4hyper.ui.NavItem
import io.github.tnyfishy.all4hyper.ui.Page
import io.github.tnyfishy.all4hyper.ui.RootState
import io.github.tnyfishy.all4hyper.ui.chargingPage
import io.github.tnyfishy.all4hyper.ui.googlePage
import io.github.tnyfishy.all4hyper.ui.homePage
import io.github.tnyfishy.all4hyper.ui.settingsPage
import io.github.tnyfishy.all4hyper.ui.systemPage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appearance = Appearance(this)
        setContent { All4HyperTheme(appearance) { AppRoot(appearance) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(appearance: Appearance) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val link by Connection.link.collectAsState()
    // A new store whenever the connection changes; the values themselves live in the remote preferences.
    val store = remember(link) { Store((link as? Link.Ready)?.let { Connection.preferences(it) }) }
    val snackbar = remember { SnackbarHostState() }

    var root by remember { mutableStateOf(RootState.Checking) }
    var rootCheck by remember { mutableIntStateOf(0) }
    var restarting by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var companion by remember { mutableStateOf(CompanionState.Unknown) }

    // The first su call is what makes the root manager ask for permission.
    LaunchedEffect(rootCheck) {
        root = RootState.Checking
        root = if (Root.available()) RootState.Granted else RootState.Denied
    }

    // The companion is read through root, so it waits for the root answer (and again on every re-check).
    LaunchedEffect(root) {
        when (root) {
            RootState.Checking -> Unit
            RootState.Denied -> companion = CompanionState.Unknown
            RootState.Granted -> {
                companion = CompanionState.Checking
                companion = CompanionModule.check(context)
            }
        }
    }

    fun say(@StringRes message: Int) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(context.getString(message))
        }
    }

    val actions = HomeActions(
        restart = {
            scope.launch {
                restarting = true
                val done = Root.restart(Config.SECURITY_CENTER)
                restarting = false
                say(if (done) R.string.msg_restarted else R.string.msg_restart_failed)
            }
        },
        recheck = {
            rootCheck++
            Connection.refresh()
        },
        addScope = {
            (link as? Link.Ready)?.let { ready ->
                Connection.requestScope(ready.service, listOf(Config.SECURITY_CENTER)) { approved -> if (!approved) say(R.string.msg_scope_failed) }
                say(R.string.msg_scope_requested)
            }
        },
        recommended = {
            store.applyRecommended()
            say(R.string.msg_recommended)
        },
        reset = { confirmReset = true },
    )

    val googleActions = GoogleActions(
        install = {
            scope.launch {
                companion = CompanionState.Installing
                val done = CompanionModule.install(context)
                if (done) {
                    // A switch that is already off must stop the companion's first boot from taking its key.
                    if (!store.bool(Config.GOOGLE_POWER)) CompanionModule.applyKey(CompanionModule.Key.Power, false)
                    if (!store.bool(Config.GOOGLE_NAV)) CompanionModule.applyKey(CompanionModule.Key.Nav, false)
                }
                companion = CompanionModule.check(context)
                say(if (done) R.string.msg_companion_installed else R.string.msg_companion_failed)
            }
        },
        recheck = {
            rootCheck++
            Connection.refresh()
        },
        toggle = { key, enabled ->
            scope.launch {
                // The companion sets these two keys at boot: change the system first, save only if that worked.
                val systemKey = when (key) {
                    Config.GOOGLE_POWER -> CompanionModule.Key.Power
                    Config.GOOGLE_NAV -> CompanionModule.Key.Nav
                    else -> null
                }
                if (systemKey != null && root == RootState.Granted && !CompanionModule.applyKey(systemKey, enabled)) {
                    say(R.string.msg_key_failed)
                    return@launch
                }
                store.put(key, enabled)
                say(
                    when {
                        key == Config.GOOGLE_CIRCLE_VIBRATE -> R.string.msg_saved
                        systemKey != null && root != RootState.Granted -> R.string.msg_key_no_root
                        else -> R.string.msg_reboot_needed
                    },
                )
            }
        },
        requestScopes = { packages ->
            (link as? Link.Ready)?.let { ready ->
                Connection.requestScope(ready.service, packages) { approved -> if (!approved) say(R.string.msg_scope_failed) }
                say(R.string.msg_scope_requested)
            }
        },
    )

    val items = listOf(
        NavItem(stringResource(R.string.nav_home), Icons.Rounded.Home),
        NavItem(stringResource(R.string.nav_charging), Icons.Rounded.BatteryChargingFull),
        NavItem(stringResource(R.string.nav_system), Icons.Rounded.Memory),
        NavItem(stringResource(R.string.nav_google), Icons.Rounded.Language),
        NavItem(stringResource(R.string.nav_settings), Icons.Rounded.Settings),
    )
    val titles = listOf(stringResource(R.string.app_name)) + items.drop(1).map { it.label }
    val pager = rememberPagerState { items.size }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(titles[pager.currentPage]) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding())) {
            HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                Page {
                    when (page) {
                        0 -> homePage(link, store, root, restarting, actions)
                        1 -> chargingPage(store) { say(R.string.msg_saved) }
                        2 -> systemPage(store)
                        3 -> googlePage(link, store, companion, root, googleActions)
                        else -> settingsPage(store, appearance)
                    }
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).padding(bottom = FloatingBarHeight + navInset),
            )
            FloatingNavBar(
                items = items,
                pager = pager,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_title)) },
            text = { Text(stringResource(R.string.reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    store.reset()
                    say(R.string.msg_reset_done)
                }) { Text(stringResource(R.string.action_reset)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
