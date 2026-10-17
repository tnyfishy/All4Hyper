package io.github.tnyfishy.all4hyper;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class All4HyperModule extends XposedModule {
    static final String TAG = "All4Hyper";
    static final String SECURITY_CENTER = "com.miui.securitycenter";

    @Override public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!SECURITY_CENTER.equals(param.getPackageName())) return;
        SharedPreferences prefs;
        try {
            prefs = getRemotePreferences(Prefs.GROUP);
        } catch (Throwable failure) {
            log(Log.ERROR, TAG, "Settings unavailable; AppOps hooks not installed", failure);
            return;
        }
        AppOpsHooks.install(this, param.getClassLoader(), prefs);
    }
}
