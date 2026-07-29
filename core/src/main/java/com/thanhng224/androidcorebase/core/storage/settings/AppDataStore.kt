package com.thanhng224.androidcorebase.core.storage.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private const val APP_SETTINGS_DATASTORE_NAME = "core_app_settings"

internal val Context.appSettingsDataStore by preferencesDataStore(name = APP_SETTINGS_DATASTORE_NAME)
