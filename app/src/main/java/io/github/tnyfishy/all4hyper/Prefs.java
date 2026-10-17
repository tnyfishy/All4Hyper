package io.github.tnyfishy.all4hyper;

/** Remote preference names shared by the settings app and the hooked process. */
public final class Prefs {
    public static final String GROUP = "all4hyper";

    /** Security Center may not put any app's private AppOps into a restricted mode. */
    public static final String APPOPS_UNRESTRICT = "appops_unrestrict";

    private Prefs() { }
}
