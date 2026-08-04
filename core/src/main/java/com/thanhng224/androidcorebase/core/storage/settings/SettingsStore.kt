package com.thanhng224.androidcorebase.core.storage.settings

import kotlinx.coroutines.flow.Flow

public interface SettingsStore {
    public fun <T> observe(key: SettingsKey<T>): Flow<T>

    public suspend fun <T> get(key: SettingsKey<T>): T

    public suspend fun <T> set(
        key: SettingsKey<T>,
        value: T,
    )

    public suspend fun <T> remove(key: SettingsKey<T>)
}
