package com.thanhng224.androidxmlbase.core.storage.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class DataStoreSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : SettingsStore {
    override fun <T> observe(key: SettingsKey<T>): Flow<T> {
        val prefsKey = key.toPreferencesKey()
        return dataStore.data.map { it[prefsKey] ?: key.defaultValue }
    }

    override suspend fun <T> get(key: SettingsKey<T>): T = observe(key).first()

    override suspend fun <T> set(
        key: SettingsKey<T>,
        value: T,
    ) {
        val prefsKey = key.toPreferencesKey()
        dataStore.edit { it[prefsKey] = value }
    }

    override suspend fun <T> remove(key: SettingsKey<T>) {
        val prefsKey = key.toPreferencesKey()
        dataStore.edit { it.remove(prefsKey) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> SettingsKey<T>.toPreferencesKey(): Preferences.Key<T> =
        when (this) {
            is SettingsKey.StringKey -> stringPreferencesKey(name)
            is SettingsKey.IntKey -> intPreferencesKey(name)
            is SettingsKey.LongKey -> longPreferencesKey(name)
            is SettingsKey.BooleanKey -> booleanPreferencesKey(name)
            is SettingsKey.FloatKey -> floatPreferencesKey(name)
        } as Preferences.Key<T>
}
