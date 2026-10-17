package io.github.tnyfishy.all4hyper;

/**
 * Preference keys shared by the settings app and the hooked Security Center process.
 *
 * <p>Everything lives in LSPosed remote preferences (group {@link #GROUP}): the app writes
 * through {@code XposedService}, the hooked process reads through
 * {@code XposedModule#getRemotePreferences}, so both sides see the same data.</p>
 */
public final class Config {
    public static final String GROUP = "config";
    public static final String SECURITY_CENTER = "com.miui.securitycenter";

    public static final String ENABLED = "master_enabled";
    public static final String DEBUG_LOG = "debug_log";

    // Charging
    public static final String KILL_NIGHT_ENTRY = "kill_night_entry";
    public static final String KILL_NIGHT_STATE = "kill_night_state";
    public static final String KILL_ALL_PROTECT = "kill_all_protect";
    public static final String KILL_COMMON_PROTECT = "kill_common_protect";
    public static final String OVERRIDE_LIMIT = "override_limit";
    /** 50..100; {@link #LIMIT_UNLIMITED} means no limit. */
    public static final String LIMIT_PERCENT = "limit_percent";
    /** 0 stock, 1 force on, 2 force off. */
    public static final String FAST_CHARGE_MODE = "fast_charge_mode";
    /** 0 stock, 1 block auto start, 2 block auto stop. */
    public static final String BYPASS_MODE = "bypass_mode";
    public static final String KILL_POGO = "kill_pogo_protect";
    public static final String KILL_WIRELESS_SILENCE = "kill_wireless_silence";
    public static final String FAKE_HEALTH = "fake_health";
    /** 1 (worst) .. 4 (best). */
    public static final String HEALTH_LEVEL = "health_level";
    /** Text shown instead of the health percentage on the charge protection page; empty keeps it. */
    public static final String UI_HEALTH_TEXT = "ui_health_text";

    // Power and performance
    public static final String KILL_POWER_MODE = "kill_power_mode";
    public static final String KILL_SUPER_SAVE = "kill_super_save";
    public static final String NO_FPS_THROTTLE = "no_fps_throttle";
    public static final String NO_THERMAL_LIMIT = "no_thermal_limit";

    // System and background
    public static final String NO_APPOPS_RESTRICT = "no_appops_restrict";
    public static final String NO_KILL_BACKGROUND = "no_kill_background";
    public static final String ALLOW_AUTOSTART = "allow_autostart";

    // Network and install
    public static final String NO_NETWORK_RESTRICT = "no_network_restrict";
    public static final String NO_BG_NETWORK_RESTRICT = "no_bg_network_restrict";
    public static final String NO_TRAFFIC_CUTOFF = "no_traffic_cutoff";
    public static final String NO_TETHER_LIMIT = "no_tether_limit";
    public static final String NO_INSTALL_VERIFY = "no_install_verify";

    // Google features (all off until the user turns them on)
    /** Long-press power opens Google's assistant. System framework hooks; needs the companion. */
    public static final String GOOGLE_POWER = "google_power";
    /** Long-press the navigation handle opens Circle to Search. */
    public static final String GOOGLE_NAV = "google_nav";
    /** A short tick when Circle to Search opens from the navigation handle. */
    public static final String GOOGLE_CIRCLE_VIBRATE = "google_circle_vibrate";
    /** Removes the China GMS restriction, with Quick Share and its control center tile. */
    public static final String GOOGLE_GMS = "google_gms";
    /** Circle to Search live (scroll) translation. */
    public static final String GOOGLE_LIVE_TRANSLATE = "google_live_translate";

    public static final String SYSTEM_SCOPE = "system";
    public static final String SYSTEM_UI = "com.android.systemui";
    public static final String XIAOAI = "com.miui.voiceassist";
    public static final String GOOGLE_APP = "com.google.android.googlequicksearchbox";

    public static final int LIMIT_UNLIMITED = 100;

    /** Switches that are on until the user turns them off; every other boolean starts off. */
    public static boolean defaultBool(String key) {
        return ENABLED.equals(key) || KILL_NIGHT_ENTRY.equals(key)
                || KILL_NIGHT_STATE.equals(key) || OVERRIDE_LIMIT.equals(key);
    }

    private Config() { }
}
