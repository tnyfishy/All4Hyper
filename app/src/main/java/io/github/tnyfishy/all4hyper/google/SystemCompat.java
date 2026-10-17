package io.github.tnyfishy.all4hyper.google;

/** The Google features are written for HyperOS; elsewhere they stay out of the way. */
public final class SystemCompat {
    private SystemCompat() { }

    /** HyperOS reports its version name in ro.mi.os.version.name. */
    public static boolean isHyperOs() {
        try {
            Class<?> properties = Class.forName("android.os.SystemProperties");
            String version = (String) properties.getMethod("get", String.class)
                    .invoke(null, "ro.mi.os.version.name");
            return version != null && !version.trim().isEmpty();
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return false;
        }
    }
}
