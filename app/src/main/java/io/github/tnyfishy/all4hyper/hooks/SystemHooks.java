package io.github.tnyfishy.all4hyper.hooks;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Power, performance, permission and install hooks that live in Security Center's own code:
 * power mode switching, super power saving, throttling, thermal config, private AppOps,
 * background clean-up, auto-start and the install verification receiver.
 */
final class SystemHooks {
    /** The one dispatcher behind performance, power saving and super power saving modes. */
    private static final String POWER_SAVER_PROVIDER = "com.miui.powercenter.powersaver.PowerSaverProvider";
    private static final String SUPER_SAVE = "uj.p";
    /** Power task manager: G/H change brightness, I lowers the refresh rate, O shortens screen timeout. */
    private static final String POWER_TASK = "jh.x";
    private static final String THERMAL = "jh.g0";
    private static final String APPOPS_COMPAT = "com.miui.permcenter.compact.AppOpsUtilsCompat";
    private static final String MEMORY_CHECK = "com.miui.securitycenter.memory.MemoryCheck";
    private static final String MEMORY_CALLBACK = "com.miui.securitycenter.memory.IMemoryCleanupCallback";
    private static final String APP_MANAGE_UTILS = "com.miui.appmanager.AppManageUtils";
    private static final String PACKAGE_VERIFY = "com.miui.permcenter.install.PackageVerificationReceiver";

    /** Only calls that switch modes are dropped; queries such as checkOverclock must still answer. */
    private static final Set<String> MODE_SWITCH_CALLS = Set.of("changePowerMode", "changePerformanceMode",
            "changeSuperPowerMode", "changeBalancedMode", "changeExtremeEnduranceMode");

    static void install(HookKit k) {
        powerMode(k);
        superSave(k);
        throttling(k);
        thermal(k);
        appOps(k);
        killBackground(k);
        autoStart(k);
        installVerify(k);
    }

    private static void powerMode(HookKit k) {
        k.hook("power_mode", k.method(POWER_SAVER_PROVIDER, "call", String.class, String.class, Bundle.class),
                (chain, s) -> {
                    Object name = chain.getArg(1);
                    if (s.enabled && s.killPowerMode && name instanceof String && MODE_SWITCH_CALLS.contains(name)) {
                        k.debug("Power mode switch blocked: " + name);
                        return null;
                    }
                    return chain.proceed();
                });
    }

    /** X() chains into disabling apps, killing background, freezing auto-start and limiting notifications. */
    private static void superSave(HookKit k) {
        k.drop("super_save", k.method(SUPER_SAVE, "X", boolean.class, boolean.class, boolean.class),
                s -> s.killSuperSave, "Super power saving blocked");
    }

    private static void throttling(HookKit k) {
        for (String name : new String[]{"G", "H", "O"}) {
            k.drop("throttle_" + name, k.method(POWER_TASK, name, int.class),
                    s -> s.noFpsThrottle, "Power saving task " + name + " blocked");
        }
        k.drop("throttle_I", k.method(POWER_TASK, "I"), s -> s.noFpsThrottle, "Power saving refresh rate drop blocked");
    }

    /** e(int, boolean) writes persist.sys.thermal.config and the thermal_message sconfig node. */
    private static void thermal(HookKit k) {
        k.drop("thermal", k.method(THERMAL, "e", int.class, boolean.class),
                s -> s.noThermalLimit, "Thermal config write blocked");
    }

    /**
     * Private AppOps (no installs 10048, no chain launch 10049, device identifiers 10050, screen
     * recording protection 10041, accessibility restriction 119, AI screen reading/control
     * 10054/10055) are all set through three wrapper methods. Any mode other than MODE_ALLOWED
     * is dropped.
     *
     * <p>The wrapper's setMode takes (appOps, pkg, uid, op, mode): op and pkg sit the other way
     * round from AppOpsManager.setMode(op, uid, pkg, mode).</p>
     */
    private static void appOps(HookKit k) {
        k.hook("appops_setmode", k.method(APPOPS_COMPAT, "setMode",
                AppOpsManager.class, String.class, int.class, String.class, int.class), (chain, s) -> {
            if (s.enabled && s.noAppOpsRestrict && restricts(chain.getArg(4))) {
                k.debug("AppOps restriction lifted, op=" + chain.getArg(3));
                return null;
            }
            return chain.proceed();
        });
        k.hook("appops_xspace", k.method(APPOPS_COMPAT, "setModeWithXSpace",
                Context.class, AppOpsManager.class, String.class, int.class, int.class, int.class), (chain, s) -> {
            if (s.enabled && s.noAppOpsRestrict && restricts(chain.getArg(5))) {
                k.debug("AppOps restriction lifted (XSpace)");
                return null;
            }
            return chain.proceed();
        });
        k.hook("appops_uidmode", k.method(APPOPS_COMPAT, "setUidMode",
                AppOpsManager.class, String.class, int.class, int.class), (chain, s) -> {
            if (s.enabled && s.noAppOpsRestrict && restricts(chain.getArg(3))) {
                k.debug("AppOps restriction lifted (uid mode)");
                return null;
            }
            return chain.proceed();
        });
    }

    /** MODE_ALLOWED is 0; every other mode is a restriction. */
    private static boolean restricts(Object mode) {
        return !(mode instanceof Integer) || (Integer) mode != 0;
    }

    /**
     * One-tap clean-up reaches killBackgroundProcesses by two routes: MemoryCheck.d4 (the AIDL
     * entry) and MemoryCheck.O9(String) (the per-package kill). Both are dropped.
     */
    private static void killBackground(HookKit k) {
        Class<?> callback = k.type(MEMORY_CALLBACK);
        if (callback != null) {
            k.drop("kill_background", k.method(MEMORY_CHECK, "d4", List.class, callback),
                    s -> s.noKillBackground, "Memory clean-up blocked");
        }
        k.hook("kill_process", k.method(MEMORY_CHECK, "O9", String.class), (chain, s) -> {
            if (s.enabled && s.noKillBackground) {
                k.debug("killBackgroundProcesses blocked: " + chain.getArg(0));
                return null;
            }
            return chain.proceed();
        });
    }

    /**
     * w0(Context, int, String, boolean) stores the auto-start permission; allow=false is flipped
     * to true. Auto-start is kept in the platform permission library, not in Settings or AppOps,
     * so this write is the only place to catch it.
     */
    private static void autoStart(HookKit k) {
        Method write = k.method(APP_MANAGE_UTILS, "w0", Context.class, int.class, String.class, boolean.class);
        k.hook("autostart", write, (chain, s) -> {
            if (s.enabled && s.allowAutostart && !Boolean.TRUE.equals(chain.getArg(3))) {
                Object[] args = chain.getArgs().toArray();
                args[3] = Boolean.TRUE;
                k.debug("Auto-start forced on: " + chain.getArg(2));
                return chain.proceed(args);
            }
            return chain.proceed();
        });
    }

    /** c(...) returning false aborts the install; true lets it continue. */
    private static void installVerify(HookKit k) {
        k.hook("install_verify", k.method(PACKAGE_VERIFY, "c",
                Context.class, PackageInfo.class, String.class, String.class), (chain, s) -> {
            if (s.enabled && s.noInstallVerify) {
                k.debug("Install verification skipped: " + chain.getArg(2));
                return Boolean.TRUE;
            }
            return chain.proceed();
        });
    }

    private SystemHooks() { }
}
