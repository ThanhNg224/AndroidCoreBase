package com.thanhng224.androidxmlbase.core.storage.settings

sealed class SettingsKey<T>(
    val name: String,
    val defaultValue: T,
) {
    class StringKey(
        name: String,
        defaultValue: String = "",
    ) : SettingsKey<String>(name, defaultValue)

    class IntKey(
        name: String,
        defaultValue: Int = 0,
    ) : SettingsKey<Int>(name, defaultValue)

    class LongKey(
        name: String,
        defaultValue: Long = 0L,
    ) : SettingsKey<Long>(name, defaultValue)

    class BooleanKey(
        name: String,
        defaultValue: Boolean = false,
    ) : SettingsKey<Boolean>(name, defaultValue)

    class FloatKey(
        name: String,
        defaultValue: Float = 0f,
    ) : SettingsKey<Float>(name, defaultValue)
}
