package io.github.tnyfishy.all4hyper.ui

import androidx.annotation.StringRes
import io.github.tnyfishy.all4hyper.Config
import io.github.tnyfishy.all4hyper.R

/** A plain on/off hook with its label. */
data class Toggle(val key: String, @StringRes val title: Int, @StringRes val summary: Int)

object Features {
    val power = listOf(
        Toggle(Config.KILL_POWER_MODE, R.string.t_power_mode, R.string.t_power_mode_s),
        Toggle(Config.KILL_SUPER_SAVE, R.string.t_super_save, R.string.t_super_save_s),
        Toggle(Config.NO_FPS_THROTTLE, R.string.t_fps, R.string.t_fps_s),
        Toggle(Config.NO_THERMAL_LIMIT, R.string.t_thermal, R.string.t_thermal_s),
    )
    val permissions = listOf(
        Toggle(Config.NO_APPOPS_RESTRICT, R.string.t_appops, R.string.t_appops_s),
        Toggle(Config.NO_KILL_BACKGROUND, R.string.t_kill_bg, R.string.t_kill_bg_s),
        Toggle(Config.ALLOW_AUTOSTART, R.string.t_autostart, R.string.t_autostart_s),
    )
    val network = listOf(
        Toggle(Config.NO_NETWORK_RESTRICT, R.string.t_net, R.string.t_net_s),
        Toggle(Config.NO_BG_NETWORK_RESTRICT, R.string.t_bg_net, R.string.t_bg_net_s),
        Toggle(Config.NO_TRAFFIC_CUTOFF, R.string.t_traffic, R.string.t_traffic_s),
        Toggle(Config.NO_TETHER_LIMIT, R.string.t_tether, R.string.t_tether_s),
        Toggle(Config.NO_INSTALL_VERIFY, R.string.t_install, R.string.t_install_s),
    )

    val powerKeeper = listOf(
        Toggle(Config.PK_LOCK_MAX_FPS, R.string.pk_lock_fps, R.string.pk_lock_fps_s),
        Toggle(Config.PK_NO_DISPLAY_CTRL, R.string.pk_display_ctrl, R.string.pk_display_ctrl_s),
        Toggle(Config.PK_CUSTOM_REFRESH, R.string.pk_custom_refresh, R.string.pk_custom_refresh_s),
        Toggle(Config.PK_DONT_KILL, R.string.pk_dont_kill, R.string.pk_dont_kill_s),
        Toggle(Config.PK_KEEP_WHITELIST, R.string.pk_keep_whitelist, R.string.pk_keep_whitelist_s),
        Toggle(Config.PK_GMS_DOZE, R.string.pk_gms_doze, R.string.pk_gms_doze_s),
    )

    val googlePower = Toggle(Config.GOOGLE_POWER, R.string.g_power, R.string.g_power_s)
    val googleNav = Toggle(Config.GOOGLE_NAV, R.string.g_nav, R.string.g_nav_s)
    val googleVibrate = Toggle(Config.GOOGLE_CIRCLE_VIBRATE, R.string.g_vibrate, R.string.g_vibrate_s)
    val googleGms = Toggle(Config.GOOGLE_GMS, R.string.g_gms, R.string.g_gms_s)
    val googleLive = Toggle(Config.GOOGLE_LIVE_TRANSLATE, R.string.g_live, R.string.g_live_s)

    /** Every hook switch with the card it sits on, in page order, for the home page's list of what is on. */
    val groups: List<Pair<Int, List<Toggle>>> = listOf(
        R.string.card_power to power,
        R.string.card_perm to permissions,
        R.string.card_net to network,
        R.string.nav_google to listOf(googlePower, googleNav, googleVibrate, googleGms, googleLive),
        R.string.card_power_keeper to powerKeeper,
    )

    /** A sub-switch only does something while the switch it follows is on. */
    val parents: Map<String, String> = mapOf(googleVibrate.key to googleNav.key)
}
