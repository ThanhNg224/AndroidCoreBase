package com.thanhng224.androidxmlbase.core.localization

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeAppLocaleApplier : AppLocaleApplier {
    val appliedTags = mutableListOf<String>()
    var currentTags: String = ""

    override fun applyLocales(tag: String) {
        appliedTags.add(tag)
        currentTags = tag
    }

    override fun currentLocaleTags(): String = currentTags
}

class LocaleManagerTest {
    @Test
    fun `setLanguage English applies its language tag`() =
        runTest {
            val applier = FakeAppLocaleApplier()
            val manager = LocaleManager(applier)

            manager.setLanguage(AppLanguage.ENGLISH)

            assertEquals(listOf("en"), applier.appliedTags)
        }

    @Test
    fun `setLanguage Vietnamese applies its regional tag`() =
        runTest {
            val applier = FakeAppLocaleApplier()
            val manager = LocaleManager(applier)

            manager.setLanguage(AppLanguage.VIETNAMESE)

            assertEquals(listOf("vi-VN"), applier.appliedTags)
        }

    @Test
    fun `useSystemLanguage clears the app locale override`() =
        runTest {
            val applier = FakeAppLocaleApplier()
            val manager = LocaleManager(applier)

            manager.useSystemLanguage()

            assertEquals(listOf(""), applier.appliedTags)
        }

    @Test
    fun `currentLanguage returns the matching supported language`() =
        runTest {
            val applier = FakeAppLocaleApplier().apply { currentTags = "vi-VN" }
            val manager = LocaleManager(applier)

            assertEquals(AppLanguage.VIETNAMESE, manager.currentLanguage())
        }

    @Test
    fun `currentLanguage returns null for the system locale`() =
        runTest {
            val manager = LocaleManager(FakeAppLocaleApplier())

            assertEquals(null, manager.currentLanguage())
        }
}
