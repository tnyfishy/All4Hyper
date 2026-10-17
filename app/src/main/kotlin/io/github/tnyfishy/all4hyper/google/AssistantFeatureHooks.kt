// Derived from MiToverG422/ONextBox, commit 1ce6ad3559c9c6ba33f8d8179b1ede55198961e2. GPL-3.0. See LICENSE and NOTICE.txt.
package io.github.tnyfishy.all4hyper.google

import io.github.tnyfishy.all4hyper.google.compat.ModernHookBridge as XposedBridge
import io.github.tnyfishy.all4hyper.google.compat.ModernMethodHook as XC_MethodHook
import io.github.tnyfishy.all4hyper.google.compat.ModernReflect as XposedHelpers
import io.github.tnyfishy.all4hyper.google.AssistantRuntime.SYSTEM_PACKAGE
import io.github.tnyfishy.all4hyper.google.AssistantRuntime.ANDROID_PACKAGE
import io.github.tnyfishy.all4hyper.google.AssistantRuntime.CONTEXTUAL_SEARCH_SYSTEM_FEATURE
import io.github.tnyfishy.all4hyper.google.AssistantRuntime.log
import io.github.tnyfishy.all4hyper.google.AssistantRuntime.installedHooks

/** Only the contextual-search feature report is kept; the ColorOS (oplus) routes are not used on HyperOS. */
internal object AssistantFeatureHooks {
    fun hookContextualSearchSystemFeature(
        packageName: String,
        classLoader: ClassLoader,
    ) {
        val classNames = buildList {
            add("android.app.ApplicationPackageManager")
            if (packageName == SYSTEM_PACKAGE || packageName == ANDROID_PACKAGE) {
                // The service-side hook makes the synthetic XML feature visible to unscoped
                // clients such as the Google app too, matching a real permissions XML entry.
                add("com.android.server.pm.PackageManagerService")
                add("com.android.server.pm.PackageManagerService\$IPackageManagerImpl")
                add("com.android.server.pm.ComputerEngine")
            }
        }
        classNames.forEach { className ->
            val cls = XposedHelpers.findClassIfExists(className, classLoader) ?: return@forEach
            cls.declaredMethods
                .filter { method ->
                    method.name == "hasSystemFeature" &&
                        method.returnType == java.lang.Boolean.TYPE &&
                        method.parameterTypes.firstOrNull() == String::class.java
                }
                .forEach { method ->
                    val key =
                        "$packageName:contextual-search-system-feature:${method.toGenericString()}"
                    if (!installedHooks.add(key)) return@forEach
                    runCatching {
                        XposedBridge.hookMethod(
                            method,
                            object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    if (param.args.firstOrNull() == CONTEXTUAL_SEARCH_SYSTEM_FEATURE &&
                                        LspConfig.isAssistantNativeCircleEnabledXposed()) {
                                        param.result = true
                                    }
                                }
                            },
                        )
                    }.onFailure {
                        installedHooks.remove(key)
                        log("Failed to expose contextual-search system feature in $className", it)
                    }
                }
        }
    }
}
