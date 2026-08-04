package com.thanhng224.androidcorebase.core.storage.settings

public object AppSettingsKeys {
    public val THEME_MODE: SettingsKey.StringKey = SettingsKey.StringKey(name = "theme_mode", defaultValue = "system")
    public val FIRST_OPEN_AT: SettingsKey.LongKey = SettingsKey.LongKey(name = "first_open_at", defaultValue = 0L)
    public val OPEN_COUNT: SettingsKey.IntKey = SettingsKey.IntKey(name = "open_count", defaultValue = 0)
    public val DEBUG_LOGGING_ENABLED: SettingsKey.BooleanKey = SettingsKey.BooleanKey(name = "debug_logging_enabled", defaultValue = false)
}
