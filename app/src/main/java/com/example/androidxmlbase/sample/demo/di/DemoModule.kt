package com.example.androidxmlbase.sample.demo.di

import com.example.androidxmlbase.sample.demo.data.datasource.DemoApiService
import com.example.androidxmlbase.sample.demo.data.datasource.DemoRemoteDataSource
import com.example.androidxmlbase.sample.demo.data.datasource.DemoRemoteDataSourceImpl
import com.example.androidxmlbase.sample.demo.data.repository.DemoRepositoryImpl
import com.example.androidxmlbase.sample.demo.domain.repository.DemoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DemoModule {
    @Binds
    @Singleton
    abstract fun bindDemoRemoteDataSource(implementation: DemoRemoteDataSourceImpl): DemoRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindDemoRepository(implementation: DemoRepositoryImpl): DemoRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DemoNetworkModule {
    @Provides
    @Singleton
    fun provideDemoApiService(retrofit: Retrofit): DemoApiService = retrofit.create(DemoApiService::class.java)
}
