package com.yourco.spend.di

import com.yourco.spend.data.repo.LearnedMappingRepository
import com.yourco.spend.parser.ExpenseParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @Singleton
    fun provideExpenseParser(
        learnedMappingRepository: LearnedMappingRepository,
    ): ExpenseParser {
        var mapCache: Map<String, String> = emptyMap()
        CoroutineScope(Dispatchers.Default).launch {
            learnedMappingRepository.observeAllAsMap().collect { map ->
                mapCache = map
            }
        }
        return ExpenseParser(
            learned = { note -> mapCache[note] }
        )
    }
}
