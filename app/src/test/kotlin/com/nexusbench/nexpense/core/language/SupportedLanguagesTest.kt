package com.nexusbench.nexpense.core.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedLanguagesTest {

    @Test
    fun testAllLanguagesHas26Items() {
        val languages = LanguageCatalog.getAllLanguages()
        assertEquals(26, languages.size)
        assertEquals("", languages.first().tag) // System default
    }

    @Test
    fun testFindLanguage() {
        val en = LanguageCatalog.findLanguage("en")
        assertNotNull(en)
        assertEquals("English", en?.nativeName)

        val ja = LanguageCatalog.findLanguage("ja")
        assertNotNull(ja)
        assertEquals("日本語", ja?.nativeName)

        val zhTw = LanguageCatalog.findLanguage("zh-TW")
        assertNotNull(zhTw)
        assertEquals("繁體中文", zhTw?.nativeName)

        val system = LanguageCatalog.findLanguage("")
        assertNotNull(system)
    }

    @Test
    fun testSearchLanguages() {
        val spanish = LanguageCatalog.searchLanguages("spanish")
        assertTrue(spanish.any { it.tag == "es" })

        val espanol = LanguageCatalog.searchLanguages("español")
        assertTrue(espanol.any { it.tag == "es" })

        val nihon = LanguageCatalog.searchLanguages("日本語")
        assertTrue(nihon.any { it.tag == "ja" })
    }
}
