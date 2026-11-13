package com.zensa.app.core

import com.zensa.app.core.locale.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

/** Language selection mapping (multi-language support). */
class AppLanguageTest {

    @Test
    fun `supports english zulu sesotho and xhosa`() {
        assertEquals(setOf("en", "zu", "st", "xh"), AppLanguage.entries.map { it.tag }.toSet())
    }

    @Test
    fun `tags map to languages`() {
        assertEquals(AppLanguage.ISIZULU, AppLanguage.fromTag("zu"))
        assertEquals(AppLanguage.SESOTHO, AppLanguage.fromTag("st"))
        assertEquals(AppLanguage.ISIXHOSA, AppLanguage.fromTag("xh"))
    }

    @Test
    fun `regional tags and lists use the primary language`() {
        assertEquals(AppLanguage.ISIZULU, AppLanguage.fromTag("zu-ZA"))
        assertEquals(AppLanguage.SESOTHO, AppLanguage.fromTag("ST-LS,en"))
    }

    @Test
    fun `unknown or empty tags fall back to english`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("fr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(null))
    }

    @Test
    fun `languages are shown by their own names`() {
        assertEquals("isiZulu", AppLanguage.ISIZULU.nativeName)
        assertEquals("Sesotho", AppLanguage.SESOTHO.nativeName)
    }
}
