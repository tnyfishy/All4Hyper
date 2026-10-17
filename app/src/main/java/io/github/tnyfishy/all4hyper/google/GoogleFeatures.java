package io.github.tnyfishy.all4hyper.google;

import android.content.SharedPreferences;

/**
 * The Google feature switches as the hooked processes see them. They read the module's remote
 * preferences on every call, so a switch takes effect without a restart wherever the hook is
 * already installed; a hook that is installed only when its switch is on needs one restart.
 */
public final class GoogleFeatures {
    private static volatile SharedPreferences preferences;

    public static void attach(SharedPreferences value) {
        preferences = value;
    }

    /** Every Google feature starts off. */
    public static boolean enabled(String key) {
        SharedPreferences prefs = preferences;
        try {
            return prefs != null && prefs.getBoolean(key, false);
        } catch (RuntimeException unreadable) {
            return false;
        }
    }

    private GoogleFeatures() { }
}
