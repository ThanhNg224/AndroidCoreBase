package com.thanhng224.androidxmlbase.core.localization

import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanhng224.androidxmlbase.core.R
import com.thanhng224.androidxmlbase.core.ui.base.TransitionActivity
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

        assertEquals(AppLanguage.entries.map(AppLanguage::languageTag), localeTags)
    }

    @Test
    fun transitionActivity_keepsItsOpaqueWindowAcrossLocaleRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activityInfo =
            context.packageManager.getActivityInfo(
                ComponentName(context, TransitionActivity::class.java),
                0,
            )
        val requiredConfigChanges = ActivityInfo.CONFIG_LOCALE or ActivityInfo.CONFIG_LAYOUT_DIRECTION

        assertEquals(requiredConfigChanges, activityInfo.configChanges and requiredConfigChanges)
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val LOCALE_TAG_NAME = "locale"
        const val LOCALE_NAME_ATTRIBUTE = "name"
    }
}
