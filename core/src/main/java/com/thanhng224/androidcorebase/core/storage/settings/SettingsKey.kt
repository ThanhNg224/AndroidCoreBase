package com.thanhng224.androidcorebase.core.storage.settings

public sealed class SettingsKey<T>(
    public val name: String,
    public val defaultValue: T,
) {
    public class StringKey(
        name: String,
        defaultValue: String = "",
    ) : SettingsKey<String>(name, defaultValue)

    public class IntKey(
        name: String,
        defaultValue: Int = 0,
    ) : SettingsKey<Int>(name, defaultValue)

    public class LongKey(
        name: String,
        defaultValue: Long = 0L,
    ) : SettingsKey<Long>(name, defaultValue)

    public class BooleanKey(
        name: String,
        defaultValue: Boolean = false,
    ) : SettingsKey<Boolean>(name, defaultValue)

    public class FloatKey(
        name: String,
        defaultValue: Float = 0f,
    ) : SettingsKey<Float>(name, defaultValue)
}
