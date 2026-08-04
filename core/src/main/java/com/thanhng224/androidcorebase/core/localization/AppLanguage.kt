package com.thanhng224.androidcorebase.core.localization

import androidx.annotation.StringRes
import com.thanhng224.androidcorebase.core.R
import java.util.Locale

/**
 * One selectable app language. A data class rather than an enum so a consuming app can support
 * languages :core ships no strings for: build your own [AppLanguage] list and hand it to
 * [LocaleManager], or bind [SupportedLanguages] in Hilt.
 */
public data class AppLanguage(
    val languageTag: String,
    @param:StringRes val displayNameResId: Int,
) {
    public companion object {
        public val ENGLISH: AppLanguage = AppLanguage("en", R.string.core_language_english)
        public val VIETNAMESE: AppLanguage = AppLanguage("vi-VN", R.string.core_language_vietnamese)

        /** The languages :core itself ships display-name strings for. */
        public val BUILT_IN: List<AppLanguage> = listOf(ENGLISH, VIETNAMESE)

        public fun findByLanguageTag(
            languageTag: String,
            candidates: List<AppLanguage> = BUILT_IN,
        ): AppLanguage? {
            val language = Locale.forLanguageTag(languageTag).language
            if (language.isBlank()) return null
            return candidates.firstOrNull { candidate ->
                Locale.forLanguageTag(candidate.languageTag).language == language
            }
        }
    }
}

/**
 * The language list [LocaleManager] resolves against. Bind this from your app's Hilt module to
 * replace [AppLanguage.BUILT_IN]; without a binding, the built-in list is used.
 */
public data class SupportedLanguages(
    val values: List<AppLanguage>,
)
