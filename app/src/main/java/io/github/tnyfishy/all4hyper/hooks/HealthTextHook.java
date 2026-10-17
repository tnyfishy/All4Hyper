package io.github.tnyfishy.all4hyper.hooks;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces the battery health percentage on the charge protection page with the user's text.
 * Nothing else is touched, and an empty text changes nothing.
 *
 * <p>The percentage is recognised in two steps. Every "NN%" string that comes out of
 * Resources.getText/getString is remembered with its resource name; TextView.setText then
 * rewrites a string only when its resource is the health percentage and the view sits on the
 * charge protection page. Matching on the resource name rather than on a text value or an
 * internal counter keeps it stable across Security Center updates.</p>
 */
final class HealthTextHook {
    private static final String CHARGER_PROTECT_ACTIVITY = "com.miui.powercenter.nightcharge.ChargerProtectActivity";
    private static final String HEALTH_RESOURCE = "percent_formatted_text";

    private static final ConcurrentHashMap<String, String> RESOURCE_BY_TEXT = new ConcurrentHashMap<>();
    /** Fallback when a view's context chain does not reach an Activity. */
    private static volatile String lastActivity = "?";

    static void install(HookKit k) {
        rememberResourceNames(k);
        rewriteSetText(k);
        trackPage(k);
    }

    /** The health string is a formatted one (getString(id, args)), so every overload is observed. */
    private static void rememberResourceNames(HookKit k) {
        observe(k, "getText", new Class<?>[]{int.class}, "health_res_gettext");
        observe(k, "getText", new Class<?>[]{int.class, CharSequence.class}, "health_res_gettext_default");
        observe(k, "getString", new Class<?>[]{int.class}, "health_res_getstring");
        observe(k, "getString", new Class<?>[]{int.class, Object[].class}, "health_res_getstring_format");
    }

    private static void observe(HookKit k, String name, Class<?>[] params, String id) {
        k.hook(id, k.method("android.content.res.Resources", name, params), (chain, s) -> {
            Object result = chain.proceed();
            remember(chain.getThisObject(), chain.getArg(0), result);
            return result;
        });
    }

    private static void remember(Object resources, Object id, Object result) {
        if (!(resources instanceof Resources) || !(id instanceof Integer) || !(result instanceof CharSequence)) return;
        String text = result.toString();
        if (!isPercent(text)) return;
        try {
            String name = ((Resources) resources).getResourceName((Integer) id);
            if (name != null) RESOURCE_BY_TEXT.put(text, name);
        } catch (Throwable invalidId) {
            // Not a real resource id.
        }
    }

    private static void rewriteSetText(HookKit k) {
        Class<?> bufferType = k.type("android.widget.TextView$BufferType");
        if (bufferType == null) return;
        Method setText = k.method("android.widget.TextView", "setText", CharSequence.class, bufferType);
        k.hook("health_text", setText, (chain, s) -> {
            String target = s.uiHealthText;
            Object first = chain.getArg(0);
            if (!s.enabled || target.isEmpty() || !(first instanceof CharSequence)) return chain.proceed();
            String text = first.toString();
            if (text.isEmpty() || target.equals(text) || !isPercent(text)) return chain.proceed();
            Object view = chain.getThisObject();
            if (view instanceof EditText) return chain.proceed();
            String resource = RESOURCE_BY_TEXT.get(text);
            if (resource == null || !resource.contains(HEALTH_RESOURCE)) return chain.proceed();
            String page = pageOf(view);
            if (!page.isEmpty() && !"?".equals(page) && !page.contains("ChargerProtectActivity")) return chain.proceed();
            k.debug("Health text " + text + " -> " + target + " (" + resource + ")");
            Object[] args = chain.getArgs().toArray();
            args[0] = target;
            return chain.proceed(args);
        });
    }

    private static void trackPage(HookKit k) {
        Class<?> page = k.type(CHARGER_PROTECT_ACTIVITY);
        if (page == null) return;
        try {
            Method onCreate = page.getDeclaredMethod("onCreate", Bundle.class);
            onCreate.setAccessible(true);
            k.hook("health_page", onCreate, (chain, s) -> {
                lastActivity = CHARGER_PROTECT_ACTIVITY;
                return chain.proceed();
            });
        } catch (Throwable missing) {
            // Page tracking is a fallback only.
        }
    }

    /** Cheap check for "NN%" so the maps and the view hook stay light. */
    private static boolean isPercent(String s) {
        int n = s.length();
        if (n < 2 || n > 5 || s.charAt(n - 1) != '%') return false;
        for (int i = 0; i < n - 1; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static String pageOf(Object view) {
        if (view instanceof View) {
            Context context = ((View) view).getContext();
            for (int i = 0; context != null && i < 20; i++) {
                if (context instanceof Activity) return context.getClass().getName();
                if (!(context instanceof ContextWrapper)) break;
                context = ((ContextWrapper) context).getBaseContext();
            }
        }
        return lastActivity;
    }

    private HealthTextHook() { }
}
