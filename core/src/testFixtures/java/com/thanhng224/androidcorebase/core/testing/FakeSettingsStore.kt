package com.thanhng224.androidcorebase.core.testing

import com.thanhng224.androidcorebase.core.storage.settings.SettingsKey
import com.thanhng224.androidcorebase.core.storage.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [SettingsStore] that emits on change, so `observe` behaves like DataStore's. */
public class FakeSettingsStore : SettingsStore {
    private val data = mutableMapOf<String, Any>()
    private val changes = MutableStateFlow<Map<String, Any>>(emptyMap())

    @Suppress("UNCHECKED_CAST")
    override fun <T> observe(key: SettingsKey<T>): Flow<T> = changes.map { it[key.name] as? T ?: key.defaultValue }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> get(key: SettingsKey<T>): T = data[key.name] as? T ?: key.defaultValue

    override suspend fun <T> set(
        key: SettingsKey<T>,
        value: T,
    ) {
        data[key.name] = value as Any
        changes.value = data.toMap()
    }

    override suspend fun <T> remove(key: SettingsKey<T>) {
        data.remove(key.name)
        changes.value = data.toMap()
    }
}
