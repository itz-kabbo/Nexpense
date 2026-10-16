package com.yourco.spend.di

import com.yourco.spend.parser.ExpenseParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    fun provideExpenseParser(): ExpenseParser = ExpenseParser()
}
