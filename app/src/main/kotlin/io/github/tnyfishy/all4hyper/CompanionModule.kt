package io.github.tnyfishy.all4hyper

import android.content.Context
import java.io.IOException
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class CompanionState {
    /** Not read yet, or root refused. */
    Unknown,
    Checking,
    Installing,
    Missing,
    Disabled,
    /** Installed, waiting for a reboot. */
    Pending,
    Ready,
    /** Installed, but older than the one bundled in this app. */
    Outdated;

    val installed: Boolean get() = this == Pending || this == Ready || this == Outdated
}

/**
 * The companion Magisk/KernelSU/APatch module (the one XGoogle ships, with a switch for the power
 * key added). It makes the Google app a system app, declares Circle to Search and its live
 * translate to the system, and sets the power key and the navigation handle to Google on every
 * boot. It is read and installed through root, only when the user asks.
 */
object CompanionModule {
    private const val DIR = "/data/adb/modules/coogle_companion"
    private const val UPDATE_DIR = "/data/adb/modules_update/coogle_companion"
    private const val ASSET = "coogle-companion.zip"
    private const val BACKUP = "/data/adb/coogle_keys_backup"
    private const val GOOGLE = "launch_google_search"

    /** A key the companion sets at boot, with the marker file that tells it to leave the key alone. */
    enum class Key(val setting: String, val marker: String, val backupField: String) {
        Power("long_press_power_key", "/data/adb/coogle_power_off", "power"),
        Nav("long_press_home_key", "/data/adb/coogle_nav_off", "home"),
    }

    suspend fun check(context: Context): CompanionState {
        val result = Root.run(
            "d=$DIR; u=$UPDATE_DIR; " +
                "if [ -f \"\$u/module.prop\" ]; then " +
                "if [ -f \"\$u/disable\" ] || [ -f \"\$u/remove\" ]; then echo DISABLED; else echo PENDING; fi; " +
                "elif [ ! -f \"\$d/module.prop\" ]; then echo MISSING; " +
                "elif [ -f \"\$d/disable\" ] || [ -f \"\$d/remove\" ]; then echo DISABLED; " +
                "else echo \"READY \$(sed -n 's/^versionCode=//p' \"\$d/module.prop\")\"; fi",
            timeoutSeconds = 20,
        )
        if (!result.ok) return CompanionState.Unknown
        val last = result.output.trim().lineSequence().lastOrNull().orEmpty().trim().split(" ")
        return when (last.firstOrNull()) {
            "MISSING" -> CompanionState.Missing
            "DISABLED" -> CompanionState.Disabled
            "PENDING" -> CompanionState.Pending
            "READY" -> {
                val installed = last.getOrNull(1)?.trim()?.toIntOrNull()
                val bundled = runCatching { bundledVersion(context) }.getOrNull()
                // An unreadable version is not reported as outdated.
                if (installed != null && bundled != null && installed < bundled) CompanionState.Outdated
                else CompanionState.Ready
            }
            else -> CompanionState.Unknown
        }
    }

    /** versionCode of the module packed into the APK. */
    private suspend fun bundledVersion(context: Context): Int = withContext(Dispatchers.IO) {
        ZipInputStream(context.assets.open(ASSET)).use { zip ->
            generateSequence { zip.nextEntry }.firstOrNull { it.name == "module.prop" }
                ?: throw IOException("Bundled companion has no module.prop")
            zip.readBytes().toString(Charsets.UTF_8).lineSequence()
                .firstOrNull { it.startsWith("versionCode=") }
                ?.substringAfter('=')?.trim()?.toInt()
                ?: throw IOException("Bundled companion version missing")
        }
    }

    suspend fun install(context: Context): Boolean {
        val zip = context.cacheDir.resolve(ASSET)
        withContext(Dispatchers.IO) {
            context.assets.open(ASSET).use { input -> zip.outputStream().use { input.copyTo(it) } }
        }
        val path = "'" + zip.path.replace("'", "'\\''") + "'"
        // Some KernelSU-Next / SukiSU builds keep ksud at /data/adb/ksud.
        val result = Root.run(
            "if [ -x /data/adb/ksu/bin/ksud ]; then /data/adb/ksu/bin/ksud module install $path; " +
                "elif [ -x /data/adb/ksud ]; then /data/adb/ksud module install $path; " +
                "elif [ -x /data/adb/apd ]; then /data/adb/apd module install $path; " +
                "else magisk --install-module $path; fi",
            timeoutSeconds = 120,
        )
        withContext(Dispatchers.IO) { zip.delete() }
        return result.ok
    }

    /**
     * Gives the key to Google or hands it back. The marker is written whether or not the companion
     * is installed yet, so its first boot respects a switch that is already off. The user's own
     * choice, saved by the companion at boot, is put back when the switch is turned off.
     */
    suspend fun applyKey(key: Key, enabled: Boolean): Boolean {
        val script = if (enabled) {
            "rm -f ${key.marker} && if [ -f $BACKUP ]; then settings put system ${key.setting} $GOOGLE; fi"
        } else {
            "touch ${key.marker} && if [ -f $BACKUP ] && " +
                "[ \"\$(settings get system ${key.setting})\" = $GOOGLE ]; then " +
                "v=\$(sed -n 's/^${key.backupField}=//p' $BACKUP); " +
                "if [ -z \"\$v\" ] || [ \"\$v\" = null ]; then settings delete system ${key.setting}; " +
                "else settings put system ${key.setting} \"\$v\"; fi; fi"
        }
        return Root.run(script, timeoutSeconds = 20).ok
    }
}
