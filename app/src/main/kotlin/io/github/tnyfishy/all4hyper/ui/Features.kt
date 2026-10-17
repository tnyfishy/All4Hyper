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

    val googlePower = Toggle(Config.GOOGLE_POWER, R.string.g_power, R.string.g_power_s)
    val googleNav = Toggle(Config.GOOGLE_NAV, R.string.g_nav, R.string.g_nav_s)
    val googleVibrate = Toggle(Config.GOOGLE_CIRCLE_VIBRATE, R.string.g_vibrate, R.string.g_vibrate_s)
    val googleGms = Toggle(Config.GOOGLE_GMS, R.string.g_gms, R.string.g_gms_s)
    val googleLive = Toggle(Config.GOOGLE_LIVE_TRANSLATE, R.string.g_live, R.string.g_live_s)
}
