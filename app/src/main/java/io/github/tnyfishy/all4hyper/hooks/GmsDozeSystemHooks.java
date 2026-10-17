package io.github.tnyfishy.all4hyper.hooks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import io.github.libxposed.api.XposedModule;
import io.github.tnyfishy.all4hyper.HookSettings;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The system server's side of letting Google Play services stay reachable in sleep, ported from
 * HyperCeiler's GMSDozeFixFramework (AGPL-3.0). HyperOS's freezer (Greeze) and broadcast queue
 * hold back GMS's push and reconnect broadcasts; these hooks let them through and give the app a
 * push message is for a short exemption, as stock Android does. Installed only when the switch is
 * on at boot, so they cost nothing otherwise; the switch takes effect after a reboot.
 */
public final class GmsDozeSystemHooks {
    private static final String GMS = "com.google.android.gms";
    private static final String GMS_PERSISTENT = "com.google.android.gms.persistent";
    private static final String C2DM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";
    /** GMS connection broadcasts that China builds defer. */
    private static final Set<String> DEFERRED = Set.of(
            "com.google.android.intent.action.GCM_RECONNECT",
            "com.google.android.gcm.DISCONNECTED",
            "com.google.android.gcm.CONNECTED",
            "com.google.android.gms.gcm.HEARTBEAT_ALARM");
    /** PowerExemptionManager.REASON_PUSH_MESSAGING_OVER_QUOTA. */
    private static final int REASON_PUSH = 102;
    private static final long PUSH_EXEMPTION_MS = 2000;

    private static final String GREEZE = "com.miui.server.greeze.GreezeManagerService";
    private static final String DOMESTIC_POLICY = "com.miui.server.greeze.DomesticPolicyManager";
    private static final String LIST_APPS = "com.miui.server.greeze.power.ListAppsManager";
    private static final String AWARE_RESOURCE = "com.miui.server.greeze.power.AwareResourceControl";
    private static final String QUEUE_STUB = "com.android.server.am.BroadcastQueueModernStubImpl";
    private static final String QUEUE = "com.android.server.am.BroadcastQueue";
    private static final String RECORD = "com.android.server.am.BroadcastRecord";
    private static final String PROCESS_POLICY = "com.android.server.am.ProcessPolicy";
    private static final String AMS = "com.android.server.am.ActivityManagerService";
    private static final String PROCESS_RECORD = "com.android.server.am.ProcessRecord";

    public static void install(XposedModule module, ClassLoader loader, SharedPreferences prefs) {
        HookKit k = new HookKit(module, loader, prefs);
        HookSettings start = k.settings();
        if (!start.enabled || !start.pkGmsDoze) return;
        greeze(k);
        domesticPolicy(k);
        listApps(k);
        broadcastQueue(k);
        processPolicy(k);
        awareResource(k);
        activityManager(k);
    }

    private static void greeze(HookKit k) {
        Method packageOfUid = k.method(GREEZE, "getPackageNameFromUid", int.class);
        Method allow = k.method(GREEZE, "isAllowBroadcast", int.class, String.class, int.class, String.class, String.class);
        k.raw("gms_greeze_allow", allow, chain -> {
            String action = (String) chain.getArg(4);
            if (action == null) return chain.proceed();
            String callee = (String) chain.getArg(3);
            if (packageOfUid != null && chain.getArg(2) instanceof Integer) {
                try {
                    Object resolved = packageOfUid.invoke(chain.getThisObject(), chain.getArg(2));
                    if (resolved instanceof String) callee = (String) resolved;
                } catch (Throwable unresolved) {
                    // Keep the package the caller passed.
                }
            }
            boolean push = GMS.equals(chain.getArg(1)) && C2DM_RECEIVE.equals(action);
            boolean reconnect = (GMS.equals(callee) || GMS_PERSISTENT.equals(callee)) && DEFERRED.contains(action);
            return push || reconnect ? Boolean.TRUE : chain.proceed();
        });
        k.deoptimize(allow);

        Method defer = k.method(GREEZE, "deferBroadcastForMiui", String.class);
        k.raw("gms_greeze_defer", defer, chain ->
                DEFERRED.contains(chain.getArg(0)) ? Boolean.FALSE : chain.proceed());
        k.deoptimize(defer);

        Method limit = k.methodNamed(GREEZE, "triggerGMSLimitAction", -1);
        k.raw("gms_greeze_limit", limit, chain -> {
            Object[] args = chain.getArgs().toArray();
            if (args.length > 0 && args[0] instanceof Boolean) {
                args[0] = Boolean.FALSE;
                return chain.proceed(args);
            }
            try {
                java.lang.reflect.Field enabled = HookKit.field(chain.getThisObject(), "mGmsLimitEnabled");
                if (enabled != null) enabled.setBoolean(chain.getThisObject(), false);
            } catch (Throwable ignored) {
                // Nothing to turn off.
            }
            return chain.proceed();
        });
        k.deoptimize(limit);
    }

    /** Only GMS's own connection broadcasts stop being deferred; everything else keeps the policy. */
    private static void domesticPolicy(HookKit k) {
        Method defer = k.method(DOMESTIC_POLICY, "deferBroadcast", String.class);
        k.raw("gms_domestic_defer", defer, chain ->
                DEFERRED.contains(chain.getArg(0)) || C2DM_RECEIVE.equals(chain.getArg(0)) ? Boolean.FALSE : chain.proceed());
        k.deoptimize(defer);
    }

    private static void listApps(HookKit k) {
        Class<?> type = k.type(LIST_APPS);
        if (type == null) return;
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            k.raw("gms_list_apps_init", constructor, chain -> {
                Object result = chain.proceed();
                Object blackList = HookKit.get(chain.getThisObject(), "mSystemBlackList");
                if (blackList instanceof Collection) {
                    try { ((Collection<?>) blackList).remove(GMS); } catch (Throwable ignored) { }
                }
                return result;
            });
            k.deoptimize(constructor);
        }
        Method inWhiteList = k.methodNamed(LIST_APPS, "isInWhiteList", -1);
        k.raw("gms_list_apps_white", inWhiteList, chain -> {
            Object whiteList = HookKit.get(chain.getThisObject(), "mUseDataWhiteList");
            if (whiteList instanceof Collection) {
                try {
                    @SuppressWarnings("unchecked") Collection<Object> set = (Collection<Object>) whiteList;
                    if (!set.contains(GMS)) set.add(GMS);
                } catch (Throwable ignored) { }
            }
            return chain.proceed();
        });
        k.deoptimize(inWhiteList);
    }

    /** A push from GMS may start the app it is for even when HyperOS's auto-start control would refuse. */
    private static void broadcastQueue(HookKit k) {
        Class<?> queue = k.type(QUEUE);
        Class<?> record = k.type(RECORD);
        if (queue == null || record == null) return;
        Method autoStart = k.method(QUEUE_STUB, "checkApplicationAutoStart", queue, record, ResolveInfo.class);
        k.raw("gms_queue_autostart", autoStart, chain -> {
            Object broadcast = chain.getArg(1);
            Object caller = HookKit.get(broadcast, "callerPackage");
            Object intent = HookKit.get(broadcast, "intent");
            if (GMS.equals(caller) && intent instanceof Intent && C2DM_RECEIVE.equals(((Intent) intent).getAction())) {
                return Boolean.TRUE;
            }
            return chain.proceed();
        });
        k.deoptimize(autoStart);
    }

    /** With flag 1 the list is the processes that are never cleaned up; GMS joins it. */
    private static void processPolicy(HookKit k) {
        k.raw("gms_process_policy", k.method(PROCESS_POLICY, "getWhiteList", int.class), chain -> {
            Object result = chain.proceed();
            if (!(chain.getArg(0) instanceof Integer) || ((Integer) chain.getArg(0) & 1) == 0) return result;
            if (!(result instanceof List)) return result;
            @SuppressWarnings("unchecked") List<Object> list = new java.util.ArrayList<>((List<Object>) result);
            if (!list.contains(GMS)) list.add(GMS);
            if (!list.contains(GMS_PERSISTENT)) list.add(GMS_PERSISTENT);
            return list;
        });
    }

    private static void awareResource(HookKit k) {
        Class<?> type = k.type(AWARE_RESOURCE);
        if (type == null) return;
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            k.raw("gms_aware_init", constructor, chain -> {
                Object result = chain.proceed();
                Object blocked = HookKit.get(chain.getThisObject(), "mNoNetworkBlackUids");
                if (blocked instanceof Collection) {
                    try { ((Collection<?>) blocked).remove(GMS); } catch (Throwable ignored) { }
                }
                return result;
            });
            k.deoptimize(constructor);
        }
    }

    /**
     * When GMS sends a push to an app, that app gets a two-second exemption and the broadcast also
     * reaches it while it is stopped, as on stock Android.
     */
    private static void activityManager(HookKit k) {
        Class<?> ams = k.type(AMS);
        Class<?> thread = k.type("android.app.IApplicationThread");
        Class<?> processRecord = k.type(PROCESS_RECORD);
        if (ams == null || thread == null || processRecord == null) return;
        Method recordOf;
        java.lang.reflect.Field info;
        java.lang.reflect.Field contextField;
        try {
            recordOf = ams.getDeclaredMethod("getRecordForAppLOSP", thread);
            recordOf.setAccessible(true);
            info = processRecord.getDeclaredField("info");
            info.setAccessible(true);
            contextField = ams.getDeclaredField("mContext");
            contextField.setAccessible(true);
        } catch (Throwable missing) {
            k.log(android.util.Log.WARN, "Target missing, skipped: AMS push exemption: " + missing);
            return;
        }
        Method broadcast = null;
        for (Method candidate : ams.getDeclaredMethods()) {
            Class<?>[] params = candidate.getParameterTypes();
            if (candidate.getName().equals("broadcastIntentWithFeature") && params.length > 2
                    && params[0] == thread && params[2] == Intent.class) {
                broadcast = candidate;
                break;
            }
        }
        if (broadcast == null) {
            k.log(android.util.Log.WARN, "Target missing, skipped: AMS#broadcastIntentWithFeature");
            return;
        }
        // addToTemporaryAllowList is a system API, so it is reached by reflection; the manager is made once.
        Object[] exemption = new Object[2];
        k.raw("gms_ams_push", broadcast, chain -> {
            Object arg = chain.getArg(2);
            if (!(arg instanceof Intent) || !C2DM_RECEIVE.equals(((Intent) arg).getAction())) return chain.proceed();
            Intent intent = (Intent) arg;
            try {
                Object app = recordOf.invoke(chain.getThisObject(), chain.getArg(0));
                Object appInfo = app == null ? null : info.get(app);
                if (appInfo instanceof ApplicationInfo && GMS.equals(((ApplicationInfo) appInfo).packageName)) {
                    String target = intent.getPackage();
                    Context context = (Context) contextField.get(chain.getThisObject());
                    if (target != null && context != null) {
                        if (exemption[0] == null) {
                            Object manager = context.getSystemService(Class.forName("android.os.PowerExemptionManager"));
                            Method add = manager.getClass().getMethod("addToTemporaryAllowList",
                                    String.class, int.class, String.class, long.class);
                            exemption[1] = add;
                            exemption[0] = manager;
                        }
                        ((Method) exemption[1]).invoke(exemption[0], target, REASON_PUSH, "GOOGLE_C2DM", PUSH_EXEMPTION_MS);
                    }
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                }
            } catch (Throwable failure) {
                k.debug("GMS push exemption skipped: " + failure);
            }
            return chain.proceed();
        });
        k.deoptimize(broadcast);
    }

    private GmsDozeSystemHooks() { }
}
