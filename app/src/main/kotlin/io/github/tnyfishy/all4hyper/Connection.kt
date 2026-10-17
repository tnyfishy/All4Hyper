package io.github.tnyfishy.all4hyper

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Where the connection to LSPosed stands. */
sealed interface Link {
    /** Waiting for the framework to hand over its binder; this takes a moment after launch. */
    data object Connecting : Link

    /** The module is not enabled in LSPosed (or no framework is running), so nothing can be saved. */
    data object Missing : Link

    data class Ready(val service: XposedService, val scope: List<String>) : Link {
        /** Security Center is the scope the Security Center hooks need. */
        val scoped: Boolean get() = Config.SECURITY_CENTER in scope
        val framework: String get() = "${service.frameworkName} ${service.frameworkVersion}"
        val api: Int get() = service.apiVersion
    }
}

/** One process-wide LSPosed service listener; the UI observes [link]. */
object Connection {
    /** Longer than the binder normally takes, short enough not to look stuck. */
    private const val BIND_TIMEOUT_MS = 4000L

    private val main = Handler(Looper.getMainLooper())
    private val state = MutableStateFlow<Link>(Link.Connecting)
    private var started = false
    private var service: XposedService? = null

    val link: StateFlow<Link> get() = state

    @Synchronized
    fun start() {
        if (started) return
        started = true
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(value: XposedService) = update(value)
            override fun onServiceDied(value: XposedService) = update(null)
        })
        main.postDelayed({ if (service == null) state.value = Link.Missing }, BIND_TIMEOUT_MS)
    }

    /** Re-reads the scope, e.g. after the user approved a scope request. */
    fun refresh() = update(service)

    private fun update(value: XposedService?) {
        service = value
        state.value = if (value == null) Link.Missing else Link.Ready(value, scopeOf(value))
    }

    private fun scopeOf(value: XposedService): List<String> =
        runCatching { value.scope.toList() }.getOrDefault(emptyList())

    /**
     * Asks LSPosed to add apps to the module's scope; the user approves in a notification.
     * LSPosed cannot add the system framework this way, so "system" is never requested.
     */
    fun requestScope(value: XposedService, packages: List<String>, done: (Boolean) -> Unit) {
        val requested = packages.filter { it != Config.SYSTEM_SCOPE }
        if (requested.isEmpty()) return done(true)
        runCatching {
            value.requestScope(requested, object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) {
                    main.post { refresh(); done(true) }
                }

                override fun onScopeRequestFailed(message: String) {
                    main.post { done(false) }
                }
            })
        }.onFailure { done(false) }
    }

    fun preferences(link: Link.Ready): SharedPreferences? =
        runCatching { link.service.getRemotePreferences(Config.GROUP) }.getOrNull()
}
