package com.nexusbench.nexpense.di

import com.nexusbench.nexpense.data.repo.CategoryRepository
import com.nexusbench.nexpense.data.repo.CategoryRepositoryImpl
import com.nexusbench.nexpense.data.repo.ExpenseRepository
import com.nexusbench.nexpense.data.repo.ExpenseRepositoryImpl
import com.nexusbench.nexpense.data.repo.LearnedMappingRepository
import com.nexusbench.nexpense.data.repo.LearnedMappingRepositoryImpl
import com.nexusbench.nexpense.data.repo.SettingsRepository
import com.nexusbench.nexpense.data.repo.SettingsRepositoryImpl
import com.nexusbench.nexpense.data.repo.ThemeRepository
import com.nexusbench.nexpense.data.repo.ThemeRepositoryImpl
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
