package io.github.tnyfishy.all4hyper

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/** The app's own language: Vietnamese unless the user picks another one in Settings. */
object Language {
    val supported = listOf("vi", "en")

    /** First launch has no choice yet, so it starts in Vietnamese whatever the system language is. */
    fun ensureDefault(context: Context) {
        val manager = context.getSystemService(LocaleManager::class.java) ?: return
        if (manager.applicationLocales.isEmpty) manager.applicationLocales = LocaleList.forLanguageTags(supported.first())
    }

    fun current(context: Context): String {
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
        val tag = locales?.takeIf { !it.isEmpty }?.get(0)?.language
        return if (tag in supported) tag!! else supported.first()
    }

    /** The activity is recreated by the system, so the new language shows at once. */
    fun set(context: Context, tag: String) {
        context.getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(tag)
    }
}
