package com.yourco.spend.di

import android.content.Context
import com.yourco.spend.data.db.AppDatabase
import com.yourco.spend.data.db.dao.CategoryDao
import com.yourco.spend.data.db.dao.ExpenseDao
import com.yourco.spend.data.db.dao.LearnedMappingDao
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
        return AppDatabase.buildDatabase(context)
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideLearnedMappingDao(database: AppDatabase): LearnedMappingDao {
        return database.learnedMappingDao()
    }
}
