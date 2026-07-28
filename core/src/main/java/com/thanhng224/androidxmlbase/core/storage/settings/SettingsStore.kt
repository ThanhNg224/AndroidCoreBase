package com.thanhng224.androidxmlbase.core.storage.settings

import kotlinx.coroutines.flow.Flow

interface SettingsStore {
    fun <T> observe(key: SettingsKey<T>): Flow<T>

    suspend fun <T> get(key: SettingsKey<T>): T

    suspend fun <T> set(
        key: SettingsKey<T>,
        value: T,
    )

    suspend fun <T> remove(key: SettingsKey<T>)
}
