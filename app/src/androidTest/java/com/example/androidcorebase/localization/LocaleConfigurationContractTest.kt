package com.example.androidcorebase.localization

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidcorebase.R
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class LocaleConfigurationContractTest {
    @Test
    fun appLanguageRegistry_matchesLocaleConfig() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val parser = context.resources.getXml(R.xml.locales_config)
        val localeTags = mutableListOf<String>()

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == LOCALE_TAG_NAME) {
                localeTags += parser.getAttributeValue(ANDROID_NAMESPACE, LOCALE_NAME_ATTRIBUTE)
            }
            parser.next()
        }

        assertEquals(AppLanguage.BUILT_IN.map(AppLanguage::languageTag), localeTags)
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val LOCALE_TAG_NAME = "locale"
        const val LOCALE_NAME_ATTRIBUTE = "name"
    }
}
