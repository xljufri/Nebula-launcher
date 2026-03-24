package com.nebula.launcher.core.di

import android.content.Context
import androidx.room.Room
import com.nebula.launcher.core.database.AppDatabase
import com.nebula.launcher.core.database.AppNodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nebula_database"
        ).build()
    }

    @Provides
    fun provideAppNodeDao(database: AppDatabase): AppNodeDao {
        return database.appNodeDao()
    }
}
