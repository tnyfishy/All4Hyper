package io.github.tnyfishy.all4hyper;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.tnyfishy.all4hyper.google.GoogleEntry;
import io.github.tnyfishy.all4hyper.hooks.GmsDozeSystemHooks;
import io.github.tnyfishy.all4hyper.hooks.PowerKeeperHooks;
import io.github.tnyfishy.all4hyper.hooks.SecurityCenterHooks;

/**
 * Entry point. Security Center runs in several processes (the power saver service lives in
 * :remote) and each gets its own copy of the hooks; the Google features are spread over the system
 * server, SystemUI, XiaoAi and the Google app; the Power Keeper hooks over Power Keeper, Mi Settings
 * and the system server.
 */
public final class All4HyperModule extends XposedModule {
    private static final String TAG = "All4Hyper";
    private SharedPreferences prefs;
    private String processName = "";

    @Override public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        processName = param.getProcessName();
        try {
            prefs = getRemotePreferences(Config.GROUP);
        } catch (Throwable failure) {
            log(Log.ERROR, TAG, "Remote preferences unavailable; defaults will be used", failure);
        }
        GoogleEntry.attach(this, prefs);
        log(Log.INFO, TAG, "Loaded in " + processName + ", framework " + getFrameworkName() + " "
                + getFrameworkVersion() + ", API " + getApiVersion());
    }

    @Override public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        GoogleEntry.hookSystem(param.getClassLoader());
        GmsDozeSystemHooks.install(this, param.getClassLoader(), prefs);
    }

    @Override public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        String packageName = param.getPackageName();
        switch (packageName) {
            case Config.SECURITY_CENTER:
                log(Log.INFO, TAG, "Security Center ready in " + processName);
                SecurityCenterHooks.install(this, param.getClassLoader(), prefs);
                break;
            case Config.POWER_KEEPER:
                PowerKeeperHooks.install(this, param.getClassLoader(), prefs, param.getApplicationInfo().sourceDir);
                break;
            case Config.MI_SETTINGS:
                PowerKeeperHooks.installMiSettings(this, param.getClassLoader(), prefs, param.getApplicationInfo().sourceDir);
                break;
            case Config.SYSTEM_UI:
            case Config.XIAOAI:
                GoogleEntry.hookPackage(packageName, param.getClassLoader());
                break;
            case Config.GOOGLE_APP:
                GoogleEntry.hookGoogleApp(this, param.getClassLoader(),
                        param.getApplicationInfo().sourceDir, param.getApplicationInfo().dataDir);
                break;
            default:
                break;
        }
    }
}
