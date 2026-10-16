package com.yourco.spend.data.repo

import com.yourco.spend.data.db.dao.CategoryDao
import com.yourco.spend.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryEntity>>
    fun observeActive(): Flow<List<CategoryEntity>>
    suspend fun getById(id: String): CategoryEntity?
    suspend fun seedDefaultsIfEmpty()
    suspend fun updateNameOverride(id: String, nameOverride: String?)
    suspend fun setHidden(id: String, isHidden: Boolean)
    suspend fun upsert(category: CategoryEntity)
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun observeAll(): Flow<List<CategoryEntity>> {
        return categoryDao.observeAll()
    }

    override fun observeActive(): Flow<List<CategoryEntity>> {
        return categoryDao.observeActive()
    }

    override suspend fun getById(id: String): CategoryEntity? {
        return categoryDao.getById(id)
    }

    override suspend fun seedDefaultsIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.upsertAll(CategoryEntity.DEFAULT_CATEGORIES)
        }
    }

    override suspend fun updateNameOverride(id: String, nameOverride: String?) {
        categoryDao.updateNameOverride(id, nameOverride)
    }

    override suspend fun setHidden(id: String, isHidden: Boolean) {
        categoryDao.setHidden(id, isHidden)
    }

    override suspend fun upsert(category: CategoryEntity) {
        categoryDao.upsert(category)
    }
}
