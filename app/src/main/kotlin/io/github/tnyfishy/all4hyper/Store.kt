package io.github.tnyfishy.all4hyper

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * The module's settings as the UI sees them. Values live in LSPosed remote preferences (so the
 * hooked process reads the same ones); every write bumps [revision] so composables that read a
 * value through this class recompose.
 */
class Store(private val prefs: SharedPreferences?) {
    private var revision by mutableIntStateOf(0)

    /** False while LSPosed is not connected: nothing can be saved then. */
    val writable: Boolean get() = prefs != null

    fun bool(key: String): Boolean {
        revision
        return prefs?.getBoolean(key, Config.defaultBool(key)) ?: Config.defaultBool(key)
    }

    fun int(key: String, default: Int): Int {
        revision
        return prefs?.getInt(key, default) ?: default
    }

    fun text(key: String): String {
        revision
        return prefs?.getString(key, "").orEmpty()
    }

    fun put(key: String, value: Boolean) = write { putBoolean(key, value) }
    fun put(key: String, value: Int) = write { putInt(key, value) }
    fun put(key: String, value: String) = write { putString(key, value) }

    private fun write(edit: SharedPreferences.Editor.() -> Unit) {
        prefs?.edit()?.apply(edit)?.apply()
        revision++
    }
}

/** Look and feel, kept in the app's own preferences: it has no meaning for the hooked process. */
class Appearance(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private var revision by mutableIntStateOf(0)

    enum class Mode { System, Light, Dark }

    var mode: Mode
        get() {
            revision
            return Mode.entries.getOrElse(prefs.getInt("mode", 0)) { Mode.System }
        }
        set(value) {
            prefs.edit().putInt("mode", value.ordinal).apply()
            revision++
        }

    var dynamicColor: Boolean
        get() {
            revision
            return prefs.getBoolean("dynamic", true)
        }
        set(value) {
            prefs.edit().putBoolean("dynamic", value).apply()
            revision++
        }
}
