package io.github.tnyfishy.all4hyper.hooks;

import android.content.Context;
import io.github.libxposed.api.XposedInterface;
import io.github.tnyfishy.all4hyper.Config;
import io.github.tnyfishy.all4hyper.HookSettings;

/**
 * Charging hooks: night charge protection, the generic protection strategies, pogo dock,
 * wireless silent charging, fast charge, bypass charging and the battery health level.
 * Targets are Security Center's obfuscated classes, so each is looked up by name and shape.
 */
final class ChargeHooks {
    /** Charge protection helpers (all static): night state, common limit writers. */
    private static final String PROTECT_UTILS = "com.miui.powercenter.charge.protect.e";
    private static final String NIGHT_MANAGER = "com.miui.powercenter.charge.protect.NightChargeProtectManager";
    /** Base class of all seven protection strategies; its l(boolean) starts or stops one. */
    private static final String BASE_PROTECT = "hg.a";
    private static final String POGO_MANAGER = "gh.e";
    private static final String WIRELESS_SILENCE = "jh.e0";
    /** IMiCharge wrapper that writes the smart_chg bits. */
    private static final String MI_CHARGE = "jh.p";
    private static final String FAST_CHARGE = "ng.c";
    private static final String SIDE_ROAD = "jg.a";
    private static final String BATTERY_HEALTH = "jh.c";

    static void install(HookKit k) {
        nightCharge(k);
        commonProtect(k);
        baseProtect(k);
        pogo(k);
        wirelessSilence(k);
        fastCharge(k);
        bypass(k);
        batteryHealth(k);
    }

    /**
     * C(Context) is the only caller of the strategy start for night charging, M(Context) applies
     * it and arms the restore alarm, and e.r(1) is the low-level setNightChargingState(1) write
     * that actually holds the battery at 80%. Returning false for r(1) keeps the caller from
     * setting its protected state; r(0), which ends protection, always goes through.
     */
    private static void nightCharge(HookKit k) {
        k.drop("night_entry", k.method(NIGHT_MANAGER, "C", Context.class),
                s -> s.killNightEntry, "Night charge protection entry blocked");
        k.drop("night_apply", k.method(NIGHT_MANAGER, "M", Context.class),
                s -> s.killNightEntry, "Night charge protection apply blocked");
        k.hook("night_state", k.method(PROTECT_UTILS, "r", int.class), (chain, s) -> {
            if (s.enabled && s.killNightState && HookKit.asInt(chain.getArg(0), 0) == 1) {
                k.debug("setNightChargingState(1) refused");
                return Boolean.FALSE;
            }
            return chain.proceed();
        });
    }

    /**
     * e.n(percent) and e.p(String, percent) write smart_chg for the high temperature, navigation
     * and long-charge protections. Either drop them, or rewrite the percentage to the user's
     * limit; a limit of 100 means the write is skipped altogether.
     */
    private static void commonProtect(HookKit k) {
        k.hook("common_protect_n", k.method(PROTECT_UTILS, "n", int.class),
                (chain, s) -> limitGate(k, chain, s, 0));
        k.hook("common_protect_p", k.method(PROTECT_UTILS, "p", String.class, int.class),
                (chain, s) -> limitGate(k, chain, s, 1));
    }

    private static Object limitGate(HookKit k, XposedInterface.Chain chain, HookSettings s, int limitArg)
            throws Throwable {
        if (!s.enabled) return chain.proceed();
        if (s.killCommonProtect) {
            k.debug("Common charge protection blocked");
            return null;
        }
        if (s.overrideLimit) {
            if (s.limitPercent >= Config.LIMIT_UNLIMITED) {
                k.debug("Common charge protection skipped: limit " + s.limitPercent + " means unlimited");
                return null;
            }
            int origin = HookKit.asInt(chain.getArg(limitArg), 80);
            if (origin != s.limitPercent) {
                Object[] args = chain.getArgs().toArray();
                args[limitArg] = s.limitPercent;
                k.debug("Charge limit " + origin + " -> " + s.limitPercent);
                return chain.proceed(args);
            }
        }
        return chain.proceed();
    }

    /**
     * hg.a.l(boolean) is implemented once and none of the seven strategies override it, so one
     * hook stops all of them from starting. l(false), the exit path, must go through or charging
     * could never resume.
     */
    private static void baseProtect(HookKit k) {
        k.hook("base_protect", k.method(BASE_PROTECT, "l", boolean.class), (chain, s) -> {
            if (s.enabled && s.killAllProtect && Boolean.TRUE.equals(chain.getArg(0))) {
                k.debug("Charge protection strategy start blocked");
                return null;
            }
            return chain.proceed();
        });
    }

    /** n() turns the dock protection on, m(int, int) is the battery-level trigger; l() (support check) is left alone. */
    private static void pogo(HookKit k) {
        k.drop("pogo_open", k.method(POGO_MANAGER, "n"), s -> s.killPogo, "Pogo protection start blocked");
        k.drop("pogo_battery", k.method(POGO_MANAGER, "m", int.class, int.class),
                s -> s.killPogo, "Pogo protection battery trigger blocked");
    }

    /** b(boolean) writes smart_chg 0x81/0x80; forcing false restores full wireless power. */
    private static void wirelessSilence(HookKit k) {
        k.hook("wireless_silence", k.method(WIRELESS_SILENCE, "b", boolean.class), (chain, s) -> {
            if (s.enabled && s.killWirelessSilence && Boolean.TRUE.equals(chain.getArg(0))) {
                k.debug("Silent wireless charging forced off");
                return chain.proceed(new Object[]{Boolean.FALSE});
            }
            return chain.proceed();
        });
    }

    /**
     * Fast charge goes ng.c.i(boolean), then the support checks e/f/g, then jh.p.d(boolean), which
     * writes bit 0 of smart_chg. Both writers are forced and, when forcing on, the support checks
     * answer true so the device allow-list and a missing vendor property cannot veto it.
     */
    private static void fastCharge(HookKit k) {
        k.hook("fast_charge_entry", k.method(FAST_CHARGE, "i", boolean.class),
                (chain, s) -> forceFastCharge(k, chain, s, "entry"));
        k.hook("fast_charge_writer", k.method(MI_CHARGE, "d", boolean.class),
                (chain, s) -> forceFastCharge(k, chain, s, "smart_chg bit0"));
        for (String name : new String[]{"e", "f", "g"}) {
            k.hook("fast_charge_support_" + name, k.method(FAST_CHARGE, name), (chain, s) ->
                    s.enabled && s.fastChargeMode == 1 ? Boolean.TRUE : chain.proceed());
        }
    }

    private static Object forceFastCharge(HookKit k, XposedInterface.Chain chain, HookSettings s, String where)
            throws Throwable {
        if (s.enabled && s.fastChargeMode != 0) {
            boolean want = s.fastChargeMode == 1;
            if (!Boolean.valueOf(want).equals(chain.getArg(0))) {
                k.debug("Fast charge (" + where + ") -> " + want);
                return chain.proceed(new Object[]{want});
            }
        }
        return chain.proceed();
    }

    /** D() starts bypass charging, E() stops it, F() stops it from the UI. Mode 1 blocks D, mode 2 blocks E and F. */
    private static void bypass(HookKit k) {
        k.drop("bypass_start", k.method(SIDE_ROAD, "D"), s -> s.bypassMode == 1, "Bypass charging auto start blocked");
        for (String name : new String[]{"E", "F"}) {
            k.drop("bypass_stop_" + name, k.method(SIDE_ROAD, name), s -> s.bypassMode == 2,
                    "Bypass charging auto stop blocked");
        }
    }

    /** p() is the only way the health level (1 worst .. 4 best) leaves the class. */
    private static void batteryHealth(HookKit k) {
        k.hook("health_level", k.method(BATTERY_HEALTH, "p"), (chain, s) ->
                s.enabled && s.fakeHealth ? (Object) s.healthLevel : chain.proceed());
    }

    private ChargeHooks() { }
}
