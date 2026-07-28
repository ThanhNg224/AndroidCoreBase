package com.thanhng224.androidxmlbase.core.localization

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat

/**
 * Process-wide [Context] captured by `core.startup.LocaleContextInitializer` at app startup.
 *
 * On API 33+, [AppCompatDelegate.getApplicationLocales] only works while at least one
 * [AppCompatDelegate] is alive (it looks up the framework `LocaleManager` via an active
 * delegate's `Context` — see `androidx.appcompat.app.AppCompatDelegate.getLocaleManagerForApplication`);
 * with none alive, it silently returns an empty locale list even though the per-app locale is
 * actually set. Reading via this captured application [Context] instead
 * ([androidx.core.app.LocaleManagerCompat.getApplicationLocales]) works regardless of whether any
 * Activity is currently alive — but this only applies on API 33+; see
 * [AppCompatLocaleApplier.currentLocaleTags] for why API <33 keeps using
 * [AppCompatDelegate.getApplicationLocales] unconditionally.
 */
internal object LocaleAppContext {
    @Volatile
    var applicationContext: Context? = null
}

interface AppLocaleApplier {
    fun applyLocales(tag: String)

    fun currentLocaleTags(): String
}

class AppCompatLocaleApplier : AppLocaleApplier {
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
        // Below API 33, AppCompatDelegate.getApplicationLocales() reads a synchronous in-memory
        // static with no active-delegate dependency at all — that range never had the bug above,
        // so it's left exactly as it was. Only API 33+ needs the LocaleManagerCompat path, since
        // that's the only range where AppCompatDelegate's read requires a live delegate; below
        // 33, LocaleManagerCompat instead reads an async-disk-written SharedPreferences store,
        // which would trade this reliable synchronous read for a racy one.
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

class LocaleManager(
    private val localeApplier: AppLocaleApplier = AppCompatLocaleApplier(),
) {
    fun setLanguage(language: AppLanguage) {
        localeApplier.applyLocales(language.languageTag)
    }

    fun useSystemLanguage() {
        localeApplier.applyLocales("")
    }

    fun currentLanguage(): AppLanguage? = AppLanguage.findByLanguageTag(localeApplier.currentLocaleTags())
}
