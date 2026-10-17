package io.github.tnyfishy.all4hyper.google

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule
import io.github.tnyfishy.all4hyper.Config
import io.github.tnyfishy.all4hyper.google.compat.ModernHookRuntime

/**
 * Where the module hands each process to the Google features. Everything here is for HyperOS and
 * does nothing elsewhere. A hook whose switch is off at start is not installed; one that is
 * installed also checks its switch on every call.
 */
object GoogleEntry {
    @JvmStatic fun attach(module: XposedModule, prefs: SharedPreferences?) {
        ModernHookRuntime.attach(module)
        GoogleFeatures.attach(prefs)
    }

    /** system_server: assistant keep-alive and voice repair (power), contextual search and the XiaoAi route (nav), GMS. */
    @JvmStatic fun hookSystem(loader: ClassLoader) {
        if (!SystemCompat.isHyperOs()) return
        val power = GoogleFeatures.enabled(Config.GOOGLE_POWER)
        val nav = GoogleFeatures.enabled(Config.GOOGLE_NAV)
        if (power) AssistantKeepAliveHooks.install(loader)
        GmsRegionRestrictionHooker.hook(loader)
        // These wait for the system context, which only power and nav need.
        if (power || nav) GoogleSystemHooks.hook(loader)
        if (nav) XiaoAiNavRedirectHooks.hookSystem(loader)
    }

    /** SystemUI (Quick Share tile) and XiaoAi (nav-handle redirect). */
    @JvmStatic fun hookPackage(packageName: String, loader: ClassLoader) {
        if (!SystemCompat.isHyperOs()) return
        when (packageName) {
            AssistantRuntime.SYSTEM_UI_PACKAGE ->
                if (GoogleFeatures.enabled(Config.GOOGLE_GMS)) QuickShareTileHooks.install(loader)
            XiaoAiNavRedirectHooks.XIAOAI_PACKAGE ->
                if (GoogleFeatures.enabled(Config.GOOGLE_NAV)) XiaoAiNavRedirectHooks.install(loader)
        }
    }

    /** The Google app: Circle to Search live translation, found in its dex and cached. */
    @JvmStatic fun hookGoogleApp(module: XposedModule, loader: ClassLoader, apkPath: String?, dataDir: String?) {
        if (!SystemCompat.isHyperOs() || !GoogleFeatures.enabled(Config.GOOGLE_LIVE_TRANSLATE)) return
        LiveTranslateHooks.install(HookInstaller(module), loader, apkPath, dataDir)
    }
}
