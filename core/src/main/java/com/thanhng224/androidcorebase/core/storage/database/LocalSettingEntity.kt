package com.thanhng224.androidcorebase.core.storage.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Reusable DB setting entity for demonstration.
 */
@Entity(tableName = "local_settings")
internal data class LocalSettingEntity(
    @PrimaryKey
    val key: String,
    @ColumnInfo(name = "value")
    val value: String,
)
