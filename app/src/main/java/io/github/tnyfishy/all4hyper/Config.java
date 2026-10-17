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

    // Power Keeper (com.miui.powerkeeper), from HyperCeiler
    /** Power Keeper stops switching the refresh rate per scene, so the display keeps its top rate. */
    public static final String PK_LOCK_MAX_FPS = "pk_lock_max_fps";
    /** Power Keeper gets no display control code from its thermal manager: no heat-driven dimming or rate drop. */
    public static final String PK_NO_DISPLAY_CTRL = "pk_no_display_ctrl";
    /** Power Keeper's ProcessManager.kill does nothing. */
    public static final String PK_DONT_KILL = "pk_dont_kill";
    /** Google Play services keeps network, alarms and push while the phone sleeps (Power Keeper and system). */
    public static final String PK_GMS_DOZE = "pk_gms_doze";
    /** Power Keeper cannot restore its batch of apps to the battery optimization whitelist. */
    public static final String PK_KEEP_WHITELIST = "pk_keep_whitelist";
    /** Per-app high refresh rate: unlocks the page in Mi Settings and the switch in Power Keeper. */
    public static final String PK_CUSTOM_REFRESH = "pk_custom_refresh";

    public static final String POWER_KEEPER = "com.miui.powerkeeper";
    public static final String MI_SETTINGS = "com.xiaomi.misettings";

    public static final String SYSTEM_SCOPE = "system";
    public static final String SYSTEM_UI = "com.android.systemui";
    public static final String XIAOAI = "com.miui.voiceassist";
    public static final String GOOGLE_APP = "com.google.android.googlequicksearchbox";

    /** Switches that are on until the user turns them off; every other boolean starts off. */
    public static boolean defaultBool(String key) {
        return ENABLED.equals(key);
    }

    private Config() { }
}
