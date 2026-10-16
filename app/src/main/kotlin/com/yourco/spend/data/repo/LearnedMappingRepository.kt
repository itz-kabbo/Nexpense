package com.yourco.spend.data.repo

import com.yourco.spend.data.db.dao.LearnedMappingDao
import com.yourco.spend.data.db.entity.LearnedMappingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface LearnedMappingRepository {
    fun observeAll(): Flow<List<LearnedMappingEntity>>
    fun observeAllAsMap(): Flow<Map<String, String>>
    suspend fun getMapping(normalizedNote: String): String?
    suspend fun recordMapping(normalizedNote: String, categoryId: String)
}

@Singleton
class LearnedMappingRepositoryImpl @Inject constructor(
    private val learnedMappingDao: LearnedMappingDao,
    private val clock: Clock,
) : LearnedMappingRepository {

    override fun observeAll(): Flow<List<LearnedMappingEntity>> {
        return learnedMappingDao.observeAll()
    }

    override fun observeAllAsMap(): Flow<Map<String, String>> {
        return learnedMappingDao.observeAll().map { list ->
            list.associate { it.normalizedNote to it.categoryId }
        }
    }

    override suspend fun getMapping(normalizedNote: String): String? {
        val key = normalizedNote.lowercase().trim()
        return learnedMappingDao.get(key)?.categoryId
    }

    override suspend fun recordMapping(normalizedNote: String, categoryId: String) {
        val key = normalizedNote.lowercase().trim()
        val existing = learnedMappingDao.get(key)
        val now = clock.millis()
        val entity = if (existing != null) {
            existing.copy(
                categoryId = categoryId,
                hits = existing.hits + 1,
                updatedAt = now,
            )
        } else {
            LearnedMappingEntity(
                normalizedNote = key,
                categoryId = categoryId,
                hits = 1,
                updatedAt = now,
            )
        }
        learnedMappingDao.upsert(entity)
    }
}
