package com.master.myapplication.di

import android.content.Context
import com.master.myapplication.data.repository.DynamicRepository
import com.master.myapplication.data.repository.Web3RepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDynamicRepository(
        @ApplicationContext context: Context
    ): DynamicRepository {
        return Web3RepositoryImpl(context)
    }
}
