package io.github.tnyfishy.all4hyper.ui

import androidx.annotation.StringRes
import io.github.tnyfishy.all4hyper.Config
import io.github.tnyfishy.all4hyper.R

/** A plain on/off hook with its label. */
data class Toggle(val key: String, @StringRes val title: Int, @StringRes val summary: Int)

object Features {
    val nightCharge = listOf(
        Toggle(Config.KILL_NIGHT_ENTRY, R.string.t_night_entry, R.string.t_night_entry_s),
        Toggle(Config.KILL_NIGHT_STATE, R.string.t_night_state, R.string.t_night_state_s),
    )
    val chargeLimit = listOf(
        Toggle(Config.KILL_ALL_PROTECT, R.string.t_kill_all, R.string.t_kill_all_s),
        Toggle(Config.KILL_COMMON_PROTECT, R.string.t_kill_common, R.string.t_kill_common_s),
    )
    val chargeExtras = listOf(
        Toggle(Config.KILL_POGO, R.string.t_pogo, R.string.t_pogo_s),
        Toggle(Config.KILL_WIRELESS_SILENCE, R.string.t_wireless, R.string.t_wireless_s),
    )
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

    /** Every plain switch that changes behavior; the count on the home screen is taken from these. */
    val all: List<Toggle> = nightCharge + chargeLimit + chargeExtras + power + permissions + network
}
