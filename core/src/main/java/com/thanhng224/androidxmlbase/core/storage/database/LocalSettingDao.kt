package com.thanhng224.androidxmlbase.core.storage.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Reusable DAO contract for local settings database transactions.
 */
@Dao
internal interface LocalSettingDao {
    @Query("SELECT * FROM local_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): LocalSettingEntity?

    @Query("SELECT * FROM local_settings WHERE `key` = :key LIMIT 1")
    fun observeSetting(key: String): Flow<LocalSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: LocalSettingEntity)

    @Query("DELETE FROM local_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}
