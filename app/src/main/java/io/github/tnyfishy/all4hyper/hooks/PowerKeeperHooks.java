package io.github.tnyfishy.all4hyper.hooks;

import android.content.SharedPreferences;
import android.os.Bundle;
import io.github.libxposed.api.XposedModule;
import io.github.tnyfishy.all4hyper.HookSettings;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

/**
 * Power Keeper (com.miui.powerkeeper) hooks, ported from HyperCeiler (AGPL-3.0,
 * ReChronoRain/HyperCeiler, library/libhook/.../rules/powerkeeper) onto this module's hook kit.
 * Plain targets are always hooked and check their switch on every call; the two found by a dex
 * scan are looked for only when their switch is on as Power Keeper starts.
 */
public final class PowerKeeperHooks {
    private static final String GMS = "com.google.android.gms";
    private static final String DISPLAY_FRAME_SETTING = "com.miui.powerkeeper.statemachine.DisplayFrameSetting";
    private static final String THERMAL_MANAGER = "com.miui.powerkeeper.feedbackcontrol.ThermalManager";
    private static final String PROCESS_MANAGER = "miui.process.ProcessManager";
    private static final String NETD_EXECUTOR = "com.miui.powerkeeper.utils.NetdExecutor";
    private static final String GMS_OBSERVER = "com.miui.powerkeeper.utils.GmsObserver";
    private static final String FEATURE_HELPER = "com.miui.powerkeeper.provider.GlobalFeatureConfigureHelper";
    private static final String REFRESH_RATE_ACTIVITY = "com.xiaomi.misettings.display.RefreshRate.RefreshRateActivity";

    /** GmsObserver methods whose boolean argument turns a restriction on. */
    private static final String[] GMS_OBSERVER_METHODS = {"updateGmsNetWork", "updateGmsAlarm", "updateGoogleReletivesWakelock"};

    public static void install(XposedModule module, ClassLoader loader, SharedPreferences prefs, String apkPath) {
        HookKit k = new HookKit(module, loader, prefs);
        HookSettings start = k.settings();
        lockMaxFps(k);
        displayCtrl(k);
        dontKill(k);
        gmsDoze(k);
        if (start.enabled && start.pkKeepWhitelist) keepWhitelist(k, loader, apkPath);
        if (start.enabled && start.pkCustomRefresh) customRefresh(k, loader, apkPath);
    }

    /** Mi Settings shows the per-app refresh rate page only on devices it lists; this lists every device. */
    public static void installMiSettings(XposedModule module, ClassLoader loader, SharedPreferences prefs, String apkPath) {
        HookKit k = new HookKit(module, loader, prefs);
        HookSettings start = k.settings();
        if (!start.enabled || !start.pkCustomRefresh) return;
        Method category = DexScan.single(k, "misettings_refresh", apkPath, loader,
                MethodMatcher.create().addEqString("btn_preferce_category"));
        k.hook("misettings_refresh", category, (chain, s) -> {
            if (s.enabled && s.pkCustomRefresh && chain.getArgs().size() > 0) {
                Object[] args = chain.getArgs().toArray();
                args[0] = Boolean.TRUE;
                return chain.proceed(args);
            }
            return chain.proceed();
        });
        // The activity keeps the "supported" answer in its one static final boolean.
        Class<?> activity = k.type(REFRESH_RATE_ACTIVITY);
        if (activity == null) return;
        for (Field field : activity.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || field.getType() != boolean.class) continue;
            try {
                field.setAccessible(true);
                field.set(null, true);
                k.log(android.util.Log.INFO, "Per-app refresh rate page unlocked");
            } catch (Throwable failure) {
                k.log(android.util.Log.WARN, "Per-app refresh rate flag not set: " + failure);
            }
            break;
        }
    }

    /** setScreenEffect(3 args) switches the refresh rate per scene; dropping it keeps the top rate. */
    private static void lockMaxFps(HookKit k) {
        k.drop("pk_lock_max_fps", k.methodNamed(DISPLAY_FRAME_SETTING, "setScreenEffect", 3),
                s -> s.pkLockMaxFps, "Scene refresh rate switch blocked");
    }

    /** Code 0 means the thermal manager asks the display for nothing (no dimming, no rate drop). */
    private static void displayCtrl(HookKit k) {
        k.hook("pk_display_ctrl", k.methodNamed(THERMAL_MANAGER, "getDisplayCtrlCode", -1), (chain, s) -> {
            if (s.enabled && s.pkNoDisplayCtrl) {
                k.debug("Thermal display control code replaced with 0");
                return 0;
            }
            return chain.proceed();
        });
    }

    private static void dontKill(HookKit k) {
        Class<?> manager = k.type(PROCESS_MANAGER);
        if (manager == null) {
            k.log(android.util.Log.WARN, "Target missing, skipped: " + PROCESS_MANAGER);
            return;
        }
        for (Method kill : manager.getDeclaredMethods()) {
            if (!kill.getName().equals("kill") || kill.getReturnType() != boolean.class) continue;
            k.hook("pk_dont_kill_" + kill.getParameterCount(), kill, (chain, s) -> {
                if (s.enabled && s.pkDontKill) {
                    k.debug("Power Keeper kill blocked");
                    return Boolean.FALSE;
                }
                return chain.proceed();
            });
        }
    }

    /**
     * Power Keeper's side of letting Google Play services stay reachable in sleep: its netd chain
     * accepts instead of dropping, its observers never turn restrictions on, and GMS is on its doze
     * whitelist. The system server side is in {@link GmsDozeSystemHooks}.
     */
    private static void gmsDoze(HookKit k) {
        Method chainSetup = k.method(NETD_EXECUTOR, "initGmsChain", String.class, int.class, String.class);
        k.hook("pk_gms_netd", chainSetup, (chain, s) -> {
            if (s.enabled && s.pkGmsDoze) {
                Object[] args = chain.getArgs().toArray();
                args[2] = "ACCEPT";
                return chain.proceed(args);
            }
            return chain.proceed();
        });
        k.deoptimize(chainSetup);
        for (String name : GMS_OBSERVER_METHODS) {
            Method observer = k.method(GMS_OBSERVER, name, boolean.class);
            k.hook("pk_gms_" + name, observer, (chain, s) -> {
                if (s.enabled && s.pkGmsDoze) return chain.proceed(new Object[]{Boolean.FALSE});
                return chain.proceed();
            });
            k.deoptimize(observer);
        }
        k.hook("pk_gms_whitelist", k.method(FEATURE_HELPER, "getDozeWhiteListApps", Bundle.class), (chain, s) -> {
            Object result = chain.proceed();
            if (s.enabled && s.pkGmsDoze && result instanceof List) {
                @SuppressWarnings("unchecked") List<Object> list = (List<Object>) result;
                try {
                    if (!list.contains(GMS)) list.add(GMS);
                } catch (UnsupportedOperationException fixed) {
                    k.debug("Doze whitelist is read-only");
                }
            }
            return result;
        });
    }

    /** Power Keeper restores its default whitelist in one batch call; batches of more than one app are dropped. */
    private static void keepWhitelist(HookKit k, ClassLoader loader, String apkPath) {
        String marker = "addPowerSaveWhitelistApps: ";
        Method add = DexScan.single(k, "pk_keep_whitelist", apkPath, loader, MethodMatcher.create()
                .declaredClass(ClassMatcher.create().usingStrings(marker))
                .usingStrings(marker));
        k.hook("pk_keep_whitelist", add, (chain, s) -> {
            if (s.enabled && s.pkKeepWhitelist && chain.getArgs().size() > 0
                    && chain.getArg(0) instanceof String[] && ((String[]) chain.getArg(0)).length > 1) {
                k.debug("Battery optimization whitelist restore blocked");
                return null;
            }
            return chain.proceed();
        });
    }

    /** The custom refresh rate switch method; it reads mIsCustomFpsSwitch, which is turned on before it runs. */
    private static void customRefresh(HookKit k, ClassLoader loader, String apkPath) {
        Method fucSwitch = DexScan.single(k, "pk_custom_refresh", apkPath, loader, MethodMatcher.create()
                .usingStrings("custom_mode_switch", "fucSwitch")
                .returnType(boolean.class));
        k.hook("pk_custom_refresh", fucSwitch, (chain, s) -> {
            if (s.enabled && s.pkCustomRefresh) {
                Field flag = HookKit.field(chain.getThisObject(), "mIsCustomFpsSwitch");
                if (flag != null) {
                    try {
                        if (flag.getType() == boolean.class || flag.getType() == Boolean.class) flag.set(chain.getThisObject(), true);
                        else if (flag.getType() == String.class) flag.set(chain.getThisObject(), "true");
                    } catch (Throwable failure) {
                        k.debug("Custom refresh rate flag not set: " + failure);
                    }
                }
            }
            return chain.proceed();
        });
    }

    private PowerKeeperHooks() { }
}
