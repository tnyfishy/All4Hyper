package io.github.tnyfishy.all4hyper.hooks;

import android.content.SharedPreferences;
import io.github.libxposed.api.XposedModule;

/** Installs every Security Center hook; one missing target never stops the others. */
public final class SecurityCenterHooks {
    public static void install(XposedModule module, ClassLoader loader, SharedPreferences prefs) {
        HookKit kit = new HookKit(module, loader, prefs);
        SystemHooks.install(kit);
        NetworkHooks.install(kit);
    }

    private SecurityCenterHooks() { }
}
