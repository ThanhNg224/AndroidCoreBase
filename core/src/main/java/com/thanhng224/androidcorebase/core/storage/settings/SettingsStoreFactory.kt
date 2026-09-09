package com.thanhng224.androidcorebase.core.storage.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.thanhng224.androidcorebase.core.foundation.SettingsStore

public object SettingsStoreFactory {
    public fun create(dataStore: DataStore<Preferences>): SettingsStore = DataStoreSettingsStore(dataStore)
}
