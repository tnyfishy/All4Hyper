package io.github.tnyfishy.all4hyper.hooks;

import android.util.Log;
import java.lang.reflect.Method;

/**
 * Network Assistant hooks: per-app firewall rules, background restriction, traffic limit
 * cut-off and the hotspot traffic limit.
 */
final class NetworkHooks {
    private static final String FIREWALL_RULE = "com.miui.networkassistant.model.FirewallRule";
    /** Last writer of per-app network rules, ending in SecurityManager.setMiuiFirewallRule. */
    private static final String NETD_FIREWALL = "com.miui.networkassistant.firewall.impl.MiuiNetdFirewall";
    private static final String BG_POLICY = "com.miui.networkassistant.firewall.BackgroundPolicyService";
    private static final String TRAFFIC_SIM = "com.miui.networkassistant.service.tm.TrafficSimManager";
    private static final String TETHER_STATS = "com.miui.networkassistant.service.tm.TetherStatsManager";

    static void install(HookKit k) {
        firewallRules(k);
        backgroundRestriction(k);
        k.drop("traffic_cutoff", k.method(TRAFFIC_SIM, "onNormalTrafficOverLimit"),
                s -> s.noTrafficCutoff, "Traffic limit cut-off blocked");
        k.drop("tether_limit", k.method(TETHER_STATS, "onTetherStatsOverLimit"),
                s -> s.noTetherLimit, "Hotspot traffic limit handling blocked");
    }

    /**
     * setWifiRule / setMobileRule: a restricting rule is replaced by FirewallRule.Allow rather
     * than skipped, so the firewall's own rule map stays in step with what the system holds.
     */
    private static void firewallRules(HookKit k) {
        Class<?> rule = k.type(FIREWALL_RULE);
        if (rule == null || !rule.isEnum()) {
            k.log(Log.WARN, "FirewallRule not found; network rule hooks skipped");
            return;
        }
        Object allow = null;
        for (Object constant : rule.getEnumConstants()) {
            if ("Allow".equals(String.valueOf(constant))) allow = constant;
        }
        if (allow == null) return;
        final Object allowRule = allow;

        Method wifi = k.method(NETD_FIREWALL, "setWifiRule", String.class, rule);
        k.hook("net_wifi", wifi, (chain, s) -> allowArg(k, chain, s.enabled && s.noNetworkRestrict, allowRule,
                "Wi-Fi network restriction lifted: "));
        Method mobile = k.method(NETD_FIREWALL, "setMobileRule", String.class, rule, int.class, boolean.class);
        k.hook("net_mobile", mobile, (chain, s) -> allowArg(k, chain, s.enabled && s.noNetworkRestrict, allowRule,
                "Mobile data network restriction lifted: "));
    }

    private static Object allowArg(HookKit k, io.github.libxposed.api.XposedInterface.Chain chain, boolean active,
            Object allow, String reason) throws Throwable {
        if (active && chain.getArg(1) != allow) {
            Object[] args = chain.getArgs().toArray();
            args[1] = allow;
            k.debug(reason + chain.getArg(0));
            return chain.proceed(args);
        }
        return chain.proceed();
    }

    /** setAppRestrictBackground(uid, restrict) and the global setRestrictBackground(restrict): true becomes false. */
    private static void backgroundRestriction(HookKit k) {
        k.hook("net_bg_app", k.method(BG_POLICY, "setAppRestrictBackground", int.class, boolean.class),
                (chain, s) -> {
                    if (s.enabled && s.noBgNetworkRestrict && Boolean.TRUE.equals(chain.getArg(1))) {
                        k.debug("Background network restriction lifted (uid " + chain.getArg(0) + ")");
                        return chain.proceed(new Object[]{chain.getArg(0), Boolean.FALSE});
                    }
                    return chain.proceed();
                });
        k.hook("net_bg_global", k.method(BG_POLICY, "setRestrictBackground", boolean.class), (chain, s) -> {
            if (s.enabled && s.noBgNetworkRestrict && Boolean.TRUE.equals(chain.getArg(0))) {
                k.debug("Global background network restriction lifted");
                return chain.proceed(new Object[]{Boolean.FALSE});
            }
            return chain.proceed();
        });
    }

    private NetworkHooks() { }
}
