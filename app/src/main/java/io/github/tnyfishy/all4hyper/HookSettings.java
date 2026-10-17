package io.github.tnyfishy.all4hyper;

import android.content.SharedPreferences;

/**
 * One read of every setting. A hook takes a snapshot per call; remote preferences cache in
 * memory and invalidate on change, so this is cheap and changes apply without a restart.
 */
public final class HookSettings {
    private static final HookSettings DEFAULTS = new HookSettings(null);

    public final boolean enabled, debugLog;
    public final boolean killNightEntry, killNightState, killAllProtect, killCommonProtect;
    public final boolean overrideLimit, killPogo, killWirelessSilence, fakeHealth;
    public final int limitPercent, fastChargeMode, bypassMode, healthLevel;
    public final String uiHealthText;
    public final boolean killPowerMode, killSuperSave, noFpsThrottle, noThermalLimit;
    public final boolean noAppOpsRestrict, noKillBackground, allowAutostart;
    public final boolean noNetworkRestrict, noBgNetworkRestrict, noTrafficCutoff, noTetherLimit, noInstallVerify;

    private HookSettings(SharedPreferences p) {
        enabled = bool(p, Config.ENABLED);
        debugLog = bool(p, Config.DEBUG_LOG);
        killNightEntry = bool(p, Config.KILL_NIGHT_ENTRY);
        killNightState = bool(p, Config.KILL_NIGHT_STATE);
        killAllProtect = bool(p, Config.KILL_ALL_PROTECT);
        killCommonProtect = bool(p, Config.KILL_COMMON_PROTECT);
        overrideLimit = bool(p, Config.OVERRIDE_LIMIT);
        killPogo = bool(p, Config.KILL_POGO);
        killWirelessSilence = bool(p, Config.KILL_WIRELESS_SILENCE);
        fakeHealth = bool(p, Config.FAKE_HEALTH);
        limitPercent = clamp(p == null ? 100 : p.getInt(Config.LIMIT_PERCENT, 100), 50, 100);
        fastChargeMode = clamp(p == null ? 0 : p.getInt(Config.FAST_CHARGE_MODE, 0), 0, 2);
        bypassMode = clamp(p == null ? 0 : p.getInt(Config.BYPASS_MODE, 0), 0, 2);
        healthLevel = clamp(p == null ? 4 : p.getInt(Config.HEALTH_LEVEL, 4), 1, 4);
        String text = p == null ? "" : p.getString(Config.UI_HEALTH_TEXT, "");
        uiHealthText = text == null ? "" : text;
        killPowerMode = bool(p, Config.KILL_POWER_MODE);
        killSuperSave = bool(p, Config.KILL_SUPER_SAVE);
        noFpsThrottle = bool(p, Config.NO_FPS_THROTTLE);
        noThermalLimit = bool(p, Config.NO_THERMAL_LIMIT);
        noAppOpsRestrict = bool(p, Config.NO_APPOPS_RESTRICT);
        noKillBackground = bool(p, Config.NO_KILL_BACKGROUND);
        allowAutostart = bool(p, Config.ALLOW_AUTOSTART);
        noNetworkRestrict = bool(p, Config.NO_NETWORK_RESTRICT);
        noBgNetworkRestrict = bool(p, Config.NO_BG_NETWORK_RESTRICT);
        noTrafficCutoff = bool(p, Config.NO_TRAFFIC_CUTOFF);
        noTetherLimit = bool(p, Config.NO_TETHER_LIMIT);
        noInstallVerify = bool(p, Config.NO_INSTALL_VERIFY);
    }

    public static HookSettings read(SharedPreferences prefs) {
        if (prefs == null) return DEFAULTS;
        try {
            return new HookSettings(prefs);
        } catch (Throwable unreadable) {
            return DEFAULTS;
        }
    }

    private static boolean bool(SharedPreferences p, String key) {
        return p == null ? Config.defaultBool(key) : p.getBoolean(key, Config.defaultBool(key));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
