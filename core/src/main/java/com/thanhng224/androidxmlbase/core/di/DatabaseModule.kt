package com.thanhng224.androidxmlbase.core.di

import android.content.Context
import androidx.room.Room
import com.thanhng224.androidxmlbase.core.storage.database.AppDatabase
import com.thanhng224.androidxmlbase.core.storage.database.DbPassphraseProvider
import com.thanhng224.androidxmlbase.core.storage.database.LocalSettingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphraseProvider: DbPassphraseProvider,
    ): AppDatabase {
        System.loadLibrary("sqlcipher")
        // DbPassphraseProvider is warmed from process startup (see DbPassphraseWarmupInitializer)
        // on Dispatchers.IO, so this almost always resolves an already-cached value instead of
        // performing disk I/O on whichever thread first triggers this Hilt provider.
        val passphrase = runBlocking(Dispatchers.IO) { passphraseProvider.getOrCreate() }
        val factory = SupportOpenHelperFactory(passphrase.toByteArray())

        return Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "app_database.db",
            ).openHelperFactory(factory)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideLocalSettingDao(database: AppDatabase): LocalSettingDao = database.localSettingDao()
}
