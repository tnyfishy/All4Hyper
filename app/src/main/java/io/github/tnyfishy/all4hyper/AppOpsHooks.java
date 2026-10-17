package io.github.tnyfishy.all4hyper;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Security Center applies Xiaomi's private AppOps (no installs, no chain launches, device
 * identifiers, screen recording, accessibility, AI screen reading/control...) through its own
 * wrapper class. While the switch is on, any call that would put an op into a mode other than
 * MODE_ALLOWED is dropped; allowing calls still go through.
 *
 * <p>The mode is the last parameter of every wrapper method, so the methods are found by name
 * and shape rather than by an exact signature that changes between Security Center builds.
 * The switch is read on every call: turning it on or off needs no restart, but modes already
 * set before it was turned on stay as they are.</p>
 */
final class AppOpsHooks {
    private static final String WRAPPER = "com.miui.permcenter.compact.AppOpsUtilsCompat";
    private static final List<String> METHODS = List.of("setMode", "setModeWithXSpace", "setUidMode");
    private static final int MODE_ALLOWED = 0;

    static void install(XposedModule module, ClassLoader classLoader, SharedPreferences prefs) {
        Class<?> wrapper;
        try {
            wrapper = Class.forName(WRAPPER, false, classLoader);
        } catch (ClassNotFoundException missing) {
            module.log(Log.WARN, All4HyperModule.TAG, WRAPPER + " not found; this Security Center build is not supported");
            return;
        }
        int hooked = 0;
        for (Method method : wrapper.getDeclaredMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (!METHODS.contains(method.getName()) || types.length == 0 || types[types.length - 1] != int.class) continue;
            try {
                module.hook(method)
                        .setId("appops:" + method.getName() + ":" + types.length)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> intercept(module, prefs, method, chain));
                hooked++;
                module.log(Log.INFO, All4HyperModule.TAG, "Hooked " + method);
            } catch (Throwable failure) {
                module.log(Log.ERROR, All4HyperModule.TAG, "Hook failed: " + method, failure);
            }
        }
        if (hooked == 0) module.log(Log.WARN, All4HyperModule.TAG, "No AppOps wrapper method matched in " + WRAPPER);
    }

    private static Object intercept(XposedModule module, SharedPreferences prefs, Method method,
            XposedInterface.Chain chain) throws Throwable {
        List<Object> args = chain.getArgs();
        Object mode = args.get(args.size() - 1);
        if (!prefs.getBoolean(Prefs.APPOPS_UNRESTRICT, false) || Integer.valueOf(MODE_ALLOWED).equals(mode)) {
            return chain.proceed();
        }
        // The arguments name the op and the app; logged so a per-op filter can be built from real values.
        Object[] named = args.subList(0, args.size() - 1).stream()
                .filter(arg -> arg instanceof String || arg instanceof Integer).toArray();
        module.log(Log.INFO, All4HyperModule.TAG, "Dropped " + method.getName() + " mode=" + mode
                + " args=" + Arrays.toString(named));
        return skippedResult(method.getReturnType());
    }

    /** What the caller sees for a dropped call: success, or the type's zero value. */
    private static Object skippedResult(Class<?> type) {
        if (type == boolean.class) return Boolean.TRUE;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private AppOpsHooks() { }
}
