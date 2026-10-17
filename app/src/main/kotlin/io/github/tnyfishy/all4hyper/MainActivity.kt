package io.github.tnyfishy.all4hyper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.tnyfishy.all4hyper.ui.All4HyperTheme
import io.github.tnyfishy.all4hyper.ui.AppNavigationBar
import io.github.tnyfishy.all4hyper.ui.GoogleActions
import io.github.tnyfishy.all4hyper.ui.HomeActions
import io.github.tnyfishy.all4hyper.ui.NavItem
import io.github.tnyfishy.all4hyper.ui.Page
import io.github.tnyfishy.all4hyper.ui.RestartAction
import io.github.tnyfishy.all4hyper.ui.RestartTarget
import io.github.tnyfishy.all4hyper.ui.RootState
import io.github.tnyfishy.all4hyper.ui.ShrinkingTopBar
import io.github.tnyfishy.all4hyper.ui.googlePage
import io.github.tnyfishy.all4hyper.ui.homePage
import io.github.tnyfishy.all4hyper.ui.settingsPage
import io.github.tnyfishy.all4hyper.ui.systemPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferHighRefreshRate()
        val appearance = Appearance(this)
        setContent { All4HyperTheme(appearance) { AppRoot(appearance) } }
    }

    override fun onResume() {
        super.onResume()
        // The display can change modes (adaptive refresh rate, folding) while the app is away.
        preferHighRefreshRate()
    }

    /**
     * Asks for the display's fastest mode at its current resolution and stops the system from
     * trading frame rate for power on this window, so scrolling and animations run at the full rate.
     * The system still lowers the rate on its own when nothing moves.
     */
    private fun preferHighRefreshRate() {
        runCatching {
            val display = display ?: return
            val current = display.mode
            val best = display.supportedModes
                .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
                .maxByOrNull { it.refreshRate }
            if (best != null && best.modeId != window.attributes.preferredDisplayModeId) {
                window.attributes = window.attributes.also { it.preferredDisplayModeId = best.modeId }
            }
            window.isFrameRatePowerSavingsBalanced = false
            window.setFrameRateBoostOnTouchEnabled(true)
        }
    }
}

/** True while the app is on screen; the background polling below stops when it is not. */
@Composable
private fun rememberStarted(): Boolean {
    val lifecycle = (LocalContext.current as ComponentActivity).lifecycle
    var started by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return started
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
    val started = rememberStarted()

    var root by remember { mutableStateOf(RootState.Checking) }
    var restarting by remember { mutableStateOf(false) }
    var companion by remember { mutableStateOf(CompanionState.Checking) }

    // Everything the status shows is kept current by this loop while the app is on screen, so there
    // is nothing to re-check by hand. The first su call is what makes the root manager ask for
    // permission; once root is granted it is not asked again, and a refusal is retried slowly.
    LaunchedEffect(started) {
        if (!started) return@LaunchedEffect
        var tick = 0
        while (true) {
            Connection.refresh()
            if (root != RootState.Granted && (root == RootState.Checking || tick % 6 == 0)) {
                root = if (Root.available()) RootState.Granted else RootState.Denied
            }
            when (root) {
                RootState.Granted -> if (tick % 2 == 0 && companion != CompanionState.Installing) {
                    val state = CompanionModule.check(context)
                    if (companion != CompanionState.Installing) companion = state
                }
                RootState.Denied -> companion = CompanionState.Unknown
                RootState.Checking -> Unit
            }
            tick++
            delay(3000)
        }
    }

    // A key switch that is on keeps its key on Google, also after a ROM reset or when the companion
    // has not booted yet. Once per launch, as soon as root and the settings are both available.
    LaunchedEffect(root, store.writable) {
        if (root != RootState.Granted || !store.writable) return@LaunchedEffect
        if (store.bool(Config.GOOGLE_POWER)) {
            CompanionModule.applyKey(CompanionModule.Key.Power, true)
        }
        if (store.bool(Config.GOOGLE_NAV)) CompanionModule.applyKey(CompanionModule.Key.Nav, true)
    }

    fun say(@StringRes message: Int) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(context.getString(message))
        }
    }

    val actions = HomeActions(
        restart = { target ->
            scope.launch {
                restarting = true
                val done = when (target) {
                    RestartTarget.SecurityCenter -> Root.restart(Config.SECURITY_CENTER)
                    RestartTarget.SystemUi -> Root.restartSystemUi()
                    RestartTarget.System -> Root.softReboot()
                }
                restarting = false
                say(if (done) R.string.msg_restarted else R.string.msg_restart_failed)
            }
        },
        addScope = {
            (link as? Link.Ready)?.let { ready ->
                Connection.requestScope(ready.service, listOf(Config.SECURITY_CENTER)) { approved -> if (!approved) say(R.string.msg_scope_failed) }
                say(R.string.msg_scope_requested)
            }
        },
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
                // The power key only opens Google while Google is the assistant.
                if (key == Config.GOOGLE_POWER && enabled && root == RootState.Granted) CompanionModule.ensureAssistantRole()
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
    )

    val google = ImageVector.vectorResource(R.drawable.ic_google)
    val items = listOf(
        NavItem(stringResource(R.string.nav_home), Icons.Outlined.Home, Icons.Rounded.Home),
        NavItem(stringResource(R.string.nav_system), Icons.Outlined.Tune, Icons.Rounded.Tune),
        NavItem(stringResource(R.string.nav_google), google),
        NavItem(stringResource(R.string.nav_settings), Icons.Outlined.Settings, Icons.Rounded.Settings),
    )
    val titles = listOf(stringResource(R.string.app_name)) + items.drop(1).map { it.label }
    val pager = rememberPagerState { items.size }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ShrinkingTopBar(titles[pager.currentPage], scrollBehavior) {
                RestartAction(enabled = root == RootState.Granted, restarting = restarting, onRestart = actions.restart)
            }
        },
        bottomBar = {
            AppNavigationBar(items, pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        HorizontalPager(pager, Modifier.fillMaxSize().padding(inner), beyondViewportPageCount = 1) { page ->
            Page {
                when (page) {
                    0 -> homePage(link, store, actions)
                    1 -> systemPage(store)
                    2 -> googlePage(store, companion, root, googleActions)
                    else -> settingsPage(store, appearance)
                }
            }
        }
    }
}
