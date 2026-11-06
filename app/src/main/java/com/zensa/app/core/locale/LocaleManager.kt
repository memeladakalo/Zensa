package com.zensa.app.core.locale

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * Applies app language for English, IsiZulu, Sesotho, IsiXhosa.
 * Persist choice via UserPreferences / DataStore.
 */
object LocaleManager {

    const val LANG_EN = "en"
    const val LANG_ZU = "zu"
    const val LANG_NSO = "nso"
    const val LANG_XH = "xh"

    val supported = listOf(LANG_EN, LANG_ZU, LANG_NSO, LANG_XH)

    fun apply(context: Context, languageTag: String): Context {
        val tag = if (languageTag in supported) languageTag else LANG_EN
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun displayName(tag: String): String = when (tag) {
        LANG_ZU -> "IsiZulu"
        LANG_NSO -> "Sesotho"
        LANG_XH -> "IsiXhosa"
        else -> "English"
    }
}
