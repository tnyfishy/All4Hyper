package io.github.tnyfishy.all4hyper.google

import io.github.tnyfishy.all4hyper.Config

/** Feature switches shared by the settings app and the migrated hooks. */
internal object LspConfig {
    fun isAssistantNativeCircleEnabledXposed() = GoogleFeatures.enabled(Config.GOOGLE_NAV)
    fun isGmsRegionRestrictionBypassEnabledXposed() = GoogleFeatures.enabled(Config.GOOGLE_GMS)
}
