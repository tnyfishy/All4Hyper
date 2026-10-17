package io.github.tnyfishy.all4hyper.hooks;

import java.lang.reflect.Method;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

/** Finds an obfuscated method in an APK's dex by what it uses, with DexKit. */
final class DexScan {
    private static volatile boolean nativeLoaded;

    /** The one method {@code matcher} finds in {@code apkPath}, loaded through {@code loader}; null when none or several. */
    static Method single(HookKit k, String id, String apkPath, ClassLoader loader, MethodMatcher matcher) {
        if (apkPath == null || apkPath.isEmpty()) {
            k.log(android.util.Log.WARN, "Dex scan skipped, no APK path: " + id);
            return null;
        }
        try {
            loadNative();
            try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
                MethodDataList found = bridge.findMethod(FindMethod.create().matcher(matcher));
                if (found.size() != 1) {
                    k.log(android.util.Log.WARN, "Dex scan found " + found.size() + " targets, skipped: " + id);
                    return null;
                }
                MethodData data = found.get(0);
                Method method = data.getMethodInstance(loader);
                method.setAccessible(true);
                return method;
            }
        } catch (Throwable failure) {
            k.log(android.util.Log.ERROR, "Dex scan failed: " + id + ": " + failure);
            return null;
        }
    }

    private static void loadNative() {
        if (nativeLoaded) return;
        synchronized (DexScan.class) {
            if (!nativeLoaded) {
                System.loadLibrary("dexkit");
                nativeLoaded = true;
            }
        }
    }

    private DexScan() { }
}
