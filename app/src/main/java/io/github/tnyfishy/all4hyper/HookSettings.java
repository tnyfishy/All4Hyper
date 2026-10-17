package io.github.tnyfishy.all4hyper;

import android.content.SharedPreferences;

/**
 * One read of every setting. A hook takes a snapshot per call; remote preferences cache in
 * memory and invalidate on change, so this is cheap and changes apply without a restart.
 */
public final class HookSettings {
    private static final HookSettings DEFAULTS = new HookSettings(null);

    public final boolean enabled, debugLog;
    public final boolean killPowerMode, killSuperSave, noFpsThrottle, noThermalLimit;
    public final boolean noAppOpsRestrict, noKillBackground, allowAutostart;
    public final boolean noNetworkRestrict, noBgNetworkRestrict, noTrafficCutoff, noTetherLimit, noInstallVerify;
    public final boolean pkLockMaxFps, pkNoDisplayCtrl, pkDontKill, pkGmsDoze, pkKeepWhitelist, pkCustomRefresh;

    private HookSettings(SharedPreferences p) {
        enabled = bool(p, Config.ENABLED);
        debugLog = bool(p, Config.DEBUG_LOG);
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
        pkLockMaxFps = bool(p, Config.PK_LOCK_MAX_FPS);
        pkNoDisplayCtrl = bool(p, Config.PK_NO_DISPLAY_CTRL);
        pkDontKill = bool(p, Config.PK_DONT_KILL);
        pkGmsDoze = bool(p, Config.PK_GMS_DOZE);
        pkKeepWhitelist = bool(p, Config.PK_KEEP_WHITELIST);
        pkCustomRefresh = bool(p, Config.PK_CUSTOM_REFRESH);
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
