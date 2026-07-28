package com.thanhng224.androidxmlbase.core.ui.theme

enum class AppTheme(
    val key: String,
) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system"),
    ;

    companion object {
        fun fromKey(key: String): AppTheme = values().firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
    }
}
