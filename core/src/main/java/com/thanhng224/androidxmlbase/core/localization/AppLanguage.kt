package com.thanhng224.androidxmlbase.core.localization

import androidx.annotation.StringRes
import com.thanhng224.androidxmlbase.core.R
import java.util.Locale

/** The only app-specific languages currently shipped by this demo. */
enum class AppLanguage(
    val languageTag: String,
    @param:StringRes val displayNameResId: Int,
) {
    ENGLISH("en", R.string.core_language_english),
    VIETNAMESE("vi-VN", R.string.core_language_vietnamese),
    ;

    companion object {
        fun findByLanguageTag(languageTag: String): AppLanguage? {
            val language = Locale.forLanguageTag(languageTag).language
            if (language.isBlank()) return null
            return entries.firstOrNull { candidate -> Locale.forLanguageTag(candidate.languageTag).language == language }
        }
    }
}
