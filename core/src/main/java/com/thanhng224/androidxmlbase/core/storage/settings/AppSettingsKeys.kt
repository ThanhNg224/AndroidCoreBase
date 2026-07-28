package com.thanhng224.androidxmlbase.core.storage.settings

object AppSettingsKeys {
    val THEME_MODE = SettingsKey.StringKey(name = "theme_mode", defaultValue = "system")
    val FIRST_OPEN_AT = SettingsKey.LongKey(name = "first_open_at", defaultValue = 0L)
    val OPEN_COUNT = SettingsKey.IntKey(name = "open_count", defaultValue = 0)
    val DEBUG_LOGGING_ENABLED = SettingsKey.BooleanKey(name = "debug_logging_enabled", defaultValue = false)
}
