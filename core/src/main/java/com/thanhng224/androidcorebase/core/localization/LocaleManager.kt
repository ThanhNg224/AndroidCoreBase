package com.thanhng224.androidcorebase.core.localization

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat

/**
 * Process-wide application [Context] captured at startup for API 33+ locale resolution.
 */
internal object LocaleAppContext {
    @Volatile
    var applicationContext: Context? = null
}

public interface AppLocaleApplier {
    public fun applyLocales(tag: String)

    public fun currentLocaleTags(): String
}

public class AppCompatLocaleApplier : AppLocaleApplier {
    override fun applyLocales(tag: String) {
        val locales =
            if (tag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    override fun currentLocaleTags(): String {
        if (Build.VERSION.SDK_INT < 33) {
            return AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }
        val context = LocaleAppContext.applicationContext
        return if (context != null) {
            LocaleManagerCompat.getApplicationLocales(context).toLanguageTags()
        } else {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }
    }
}

public class LocaleManager(
    private val localeApplier: AppLocaleApplier = AppCompatLocaleApplier(),
    private val supportedLanguages: List<AppLanguage> = AppLanguage.BUILT_IN,
) {
    public fun setLanguage(language: AppLanguage) {
        localeApplier.applyLocales(language.languageTag)
    }

    public fun useSystemLanguage() {
        localeApplier.applyLocales("")
    }

    public fun supportedLanguages(): List<AppLanguage> = supportedLanguages

    public fun currentLanguage(): AppLanguage? = AppLanguage.findByLanguageTag(localeApplier.currentLocaleTags(), supportedLanguages)
}
