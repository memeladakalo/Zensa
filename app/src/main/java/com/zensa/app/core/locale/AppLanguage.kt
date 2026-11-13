package com.zensa.app.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * AppLanguage - the languages Zensa supports.
 *
 * English, isiZulu and Sesotho are the three languages required by the Zensa
 * Planning document; isiXhosa is an extra South African language.
 *
 * Switching uses AppCompatDelegate.setApplicationLocales(), the official AndroidX
 * per-app-language API. It works on every supported Android version, recreates the
 * Activity automatically, and AppCompat remembers the choice (see the
 * AppLocalesMetadataHolderService entry in AndroidManifest.xml).
 *
 * [nativeName] is deliberately NOT translated: each language is always shown in
 * its own name so a user can find their language even if the UI is in a language
 * they cannot read.
 */
enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    ISIZULU("zu", "isiZulu"),
    SESOTHO("st", "Sesotho"),
    ISIXHOSA("xh", "isiXhosa");

    companion object {

        /** Maps a BCP-47 language tag (e.g. "zu" or "zu-ZA") to a supported language; English otherwise. */
        fun fromTag(tag: String?): AppLanguage {
            val primary = tag?.substringBefore(',')?.substringBefore('-')?.lowercase().orEmpty()
            return entries.firstOrNull { it.tag == primary } ?: ENGLISH
        }

        /** The language currently applied to the app (falls back to English). */
        fun current(): AppLanguage {
            val applied = AppCompatDelegate.getApplicationLocales()
            return if (applied.isEmpty) ENGLISH else fromTag(applied.toLanguageTags())
        }

        /** Applies [language] app-wide. Must be called on the main thread. */
        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag)
            )
        }
    }
}
