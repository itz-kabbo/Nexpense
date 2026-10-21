package com.yourco.spend.di

import com.yourco.spend.data.repo.CategoryRepository
import com.yourco.spend.data.repo.CategoryRepositoryImpl
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.ExpenseRepositoryImpl
import com.yourco.spend.data.repo.LearnedMappingRepository
import com.yourco.spend.data.repo.LearnedMappingRepositoryImpl
import com.yourco.spend.data.repo.SettingsRepository
import com.yourco.spend.data.repo.SettingsRepositoryImpl
import com.yourco.spend.data.repo.ThemeRepository
import com.yourco.spend.data.repo.ThemeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindLearnedMappingRepository(impl: LearnedMappingRepositoryImpl): LearnedMappingRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}
