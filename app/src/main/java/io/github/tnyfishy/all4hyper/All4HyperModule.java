package io.github.tnyfishy.all4hyper;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.tnyfishy.all4hyper.hooks.SecurityCenterHooks;

/**
 * Entry point. The scope is Security Center only; it runs in several processes (the power saver
 * service lives in :remote), and each gets its own copy of the hooks.
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
        log(Log.INFO, TAG, "Loaded in " + processName + ", framework " + getFrameworkName() + " "
                + getFrameworkVersion() + ", API " + getApiVersion());
    }

    @Override public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!Config.SECURITY_CENTER.equals(param.getPackageName())) return;
        log(Log.INFO, TAG, "Security Center ready in " + processName);
        SecurityCenterHooks.install(this, param.getClassLoader(), prefs);
    }
}
