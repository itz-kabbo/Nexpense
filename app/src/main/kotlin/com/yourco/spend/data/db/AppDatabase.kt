package com.yourco.spend.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yourco.spend.data.db.dao.CategoryDao
import com.yourco.spend.data.db.dao.ExpenseDao
import com.yourco.spend.data.db.dao.LearnedMappingDao
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.data.db.entity.LearnedMappingEntity
import com.yourco.spend.data.db.migration.Migrations

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        LearnedMappingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun learnedMappingDao(): LearnedMappingDao

    companion object {
        const val DATABASE_NAME = "mote.db"

        fun createCallback(): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed default categories on database creation
                CategoryEntity.DEFAULT_CATEGORIES.forEach { category ->
                    db.execSQL(
                        "INSERT INTO category (id, name_override, icon, sort_order, is_hidden, is_builtin) " +
                            "VALUES ('${category.id}', NULL, '${category.icon}', ${category.sortOrder}, 0, 1)"
                    )
                }
            }
        }

        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(createCallback())
                .addMigrations(*Migrations.ALL)
                .build()
        }
    }
}
