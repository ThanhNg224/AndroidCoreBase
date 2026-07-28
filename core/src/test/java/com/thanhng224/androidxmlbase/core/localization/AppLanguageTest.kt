package com.thanhng224.androidxmlbase.core.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `supported languages have the exact app locale tags`() {
        assertEquals(listOf("en", "vi-VN"), AppLanguage.entries.map(AppLanguage::languageTag))
    }

    @Test
    fun `findByLanguageTag recognizes a language only when it is supported`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.findByLanguageTag("en-US"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.findByLanguageTag("vi-VN"))
        assertNull(AppLanguage.findByLanguageTag("ko-KR"))
    }
}
