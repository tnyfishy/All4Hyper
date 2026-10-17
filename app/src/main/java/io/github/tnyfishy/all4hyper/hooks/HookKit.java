package io.github.tnyfishy.all4hyper.hooks;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.tnyfishy.all4hyper.HookSettings;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
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

    /** The one declared method called {@code name} with {@code paramCount} parameters (any count when negative). */
    Method methodNamed(String className, String name, int paramCount) {
        Class<?> owner = type(className);
        Method found = null;
        if (owner != null) {
            for (Method candidate : owner.getDeclaredMethods()) {
                if (!candidate.getName().equals(name)) continue;
                if (paramCount >= 0 && candidate.getParameterCount() != paramCount) continue;
                if (found != null) {
                    module.log(Log.WARN, TAG, "Target ambiguous, skipped: " + className + "#" + name);
                    return null;
                }
                found = candidate;
            }
        }
        if (found == null) {
            module.log(Log.WARN, TAG, "Target missing, skipped: " + className + "#" + name);
            return null;
        }
        found.setAccessible(true);
        return found;
    }

    /** Installs {@code body} on {@code target}; a null target (already logged) is ignored. */
    void hook(String id, Executable target, Body body) {
        raw(id, target, chain -> body.run(chain, settings()));
    }

    /** Installs a hook that reads no settings, for hot paths installed only while their switch is on. */
    void raw(String id, Executable target, XposedInterface.Hooker hooker) {
        if (target == null) return;
        try {
            module.hook(target).setId("a4h_" + id).intercept(hooker);
            module.log(Log.INFO, TAG, "Hooked " + id);
        } catch (Throwable failure) {
            module.log(Log.ERROR, TAG, "Hook failed: " + id, failure);
        }
    }

    /** Drops compiled copies of {@code target}, so callers that inlined it see a hook on what it calls. */
    void deoptimize(Executable target) {
        if (target == null) return;
        try {
            module.deoptimize(target);
        } catch (Throwable failure) {
            module.log(Log.WARN, TAG, "Deoptimize failed: " + target, failure);
        }
    }

    /** A field of {@code owner} or one of its superclasses, made accessible; null when there is none. */
    static Field field(Object owner, String name) {
        for (Class<?> type = owner == null ? null : owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException next) {
                // Look in the superclass.
            }
        }
        return null;
    }

    static Object get(Object owner, String name) {
        try {
            Field field = field(owner, name);
            return field == null ? null : field.get(owner);
        } catch (Throwable unreadable) {
            return null;
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
