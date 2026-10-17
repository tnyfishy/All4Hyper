// Adapted from MiToverG422/ONextBox, commit 1ce6ad3559c9c6ba33f8d8179b1ede55198961e2. GPL-3.0. See LICENSE and NOTICE.txt.
package io.github.tnyfishy.all4hyper.google

import android.content.Context
import io.github.tnyfishy.all4hyper.Config
import io.github.tnyfishy.all4hyper.google.AssistantRuntime.SYSTEM_PACKAGE
import io.github.tnyfishy.all4hyper.google.AssistantRuntime.installedHooks
import io.github.tnyfishy.all4hyper.google.AssistantRuntime.log
import io.github.tnyfishy.all4hyper.google.AssistantRuntime.systemContext
import io.github.tnyfishy.all4hyper.google.compat.ModernHookRegistry
import io.github.tnyfishy.all4hyper.google.compat.ModernMethodHook as XC_MethodHook
import io.github.tnyfishy.all4hyper.google.compat.ModernReflect as XposedHelpers

/**
 * The system_server hooks that wait for the system context. Some HyperOS targets only load after
 * boot, so the first valid context is captured from SystemService.getContext and the late targets
 * are tried again with the class loaders seen there.
 */
internal object GoogleSystemHooks {
    fun hook(classLoader: ClassLoader) {
        hookSystemContext(classLoader)
        retry(classLoader)
    }

    private fun hookSystemContext(classLoader: ClassLoader) {
        val key = "$SYSTEM_PACKAGE:system-context"
        val cls = XposedHelpers.findClassIfExists("com.android.server.SystemService", classLoader) ?: return
        if (!installedHooks.add(key)) return
        runCatching {
            cls.declaredMethods
                .filter { method -> method.name == "getContext" }
                .forEach { method ->
                    ModernHookRegistry.installCompat(
                        "assistant:system-context:${method.toGenericString()}",
                        method,
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                (param.result as? Context)?.let {
                                    systemContext = it
                                    sequenceOf(
                                        param.thisObject?.javaClass?.classLoader,
                                        it.classLoader,
                                        Thread.currentThread().contextClassLoader,
                                    ).filterNotNull().distinct().forEach(::retry)
                                    // SystemService#getContext is common during boot. It is needed only to
                                    // capture the first valid context and retry late targets once.
                                    ModernHookRegistry.unhookPrefix("assistant:system-context:")
                                }
                            }
                        },
                    )
                }
        }.onFailure {
            installedHooks.remove(key)
            log("Failed to hook SystemService.getContext", it)
        }
    }

    private fun retry(classLoader: ClassLoader) {
        if (LspConfig.isAssistantNativeCircleEnabledXposed()) {
            ContextualSearchHooks.hookContextualSearch(SYSTEM_PACKAGE, classLoader)
        }
        if (GoogleFeatures.enabled(Config.GOOGLE_POWER)) {
            AssistantVoiceHooks.hookVoiceInteractionServiceRepair(SYSTEM_PACKAGE, classLoader)
        }
    }
}
