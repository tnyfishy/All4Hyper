package io.github.tnyfishy.all4hyper.hooks;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.tnyfishy.all4hyper.HookSettings;
import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Shared plumbing for the Security Center hooks. Every target is an obfuscated class, so each
 * lookup is checked: a missing target is logged and skipped, never fatal to the other hooks.
 */
final class HookKit {
    static final String TAG = "All4Hyper";

    interface Body {
        Object run(XposedInterface.Chain chain, HookSettings settings) throws Throwable;
    }

    private final XposedModule module;
    private final ClassLoader loader;
    private final SharedPreferences prefs;
    private volatile boolean debug;

    HookKit(XposedModule module, ClassLoader loader, SharedPreferences prefs) {
        this.module = module;
        this.loader = loader;
        this.prefs = prefs;
    }

    HookSettings settings() {
        HookSettings snapshot = HookSettings.read(prefs);
        debug = snapshot.debugLog;
        return snapshot;
    }

    Class<?> type(String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable missing) {
            return null;
        }
    }

    Method method(String className, String name, Class<?>... params) {
        try {
            Method method = Class.forName(className, false, loader).getDeclaredMethod(name, params);
            method.setAccessible(true);
            return method;
        } catch (Throwable missing) {
            module.log(Log.WARN, TAG, "Target missing, skipped: " + className + "#" + name);
            return null;
        }
    }

    /** Installs {@code body} on {@code target}; a null target (already logged) is ignored. */
    void hook(String id, Method target, Body body) {
        if (target == null) return;
        try {
            module.hook(target).setId("a4h_" + id).intercept(chain -> body.run(chain, settings()));
            module.log(Log.INFO, TAG, "Hooked " + id);
        } catch (Throwable failure) {
            module.log(Log.ERROR, TAG, "Hook failed: " + id, failure);
        }
    }

    /** Drops the call (returns null) while the master switch and {@code when} are on. */
    void drop(String id, Method target, Predicate<HookSettings> when, String reason) {
        hook(id, target, (chain, s) -> {
            if (s.enabled && when.test(s)) {
                debug(reason);
                return null;
            }
            return chain.proceed();
        });
    }

    void debug(String message) {
        if (debug) module.log(Log.INFO, TAG, message);
    }

    void log(int priority, String message) {
        module.log(priority, TAG, message);
    }

    static int asInt(Object value, int fallback) {
        return value instanceof Integer ? (Integer) value : fallback;
    }
}
