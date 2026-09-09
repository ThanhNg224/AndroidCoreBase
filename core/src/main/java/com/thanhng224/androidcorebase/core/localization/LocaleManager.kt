package com.thanhng224.androidcorebase.core.localization

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat

public interface AppLocaleApplier {
    public fun applyLocales(tag: String)

    public fun currentLocaleTags(): String
}

public class AppCompatLocaleApplier(
    private val context: Context,
) : AppLocaleApplier {
    override fun applyLocales(tag: String) {
        val locales =
            if (tag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    override fun currentLocaleTags(): String =
        if (Build.VERSION.SDK_INT < 33) {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        } else {
            LocaleManagerCompat.getApplicationLocales(context).toLanguageTags()
        }
}

public class LocaleManager(
    private val localeApplier: AppLocaleApplier,
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
