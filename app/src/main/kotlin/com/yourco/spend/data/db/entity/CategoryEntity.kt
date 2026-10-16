package com.yourco.spend.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yourco.spend.R

// VERIFY: Built-in categories list (food, groceries, transport, shopping, home, health, fun, travel, other).
@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name_override")
    val nameOverride: String? = null,

    val icon: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    @ColumnInfo(name = "is_builtin")
    val isBuiltin: Boolean = true,
) {
    fun defaultNameRes(): Int? = defaultNameRes(id)

    companion object {
        const val CATEGORY_FOOD = "food"
        const val CATEGORY_GROCERIES = "groceries"
        const val CATEGORY_TRANSPORT = "transport"
        const val CATEGORY_SHOPPING = "shopping"
        const val CATEGORY_HOME = "home"
        const val CATEGORY_HEALTH = "health"
        const val CATEGORY_FUN = "fun"
        const val CATEGORY_TRAVEL = "travel"
        const val CATEGORY_OTHER = "other"

        fun defaultNameRes(id: String): Int? = when (id) {
            CATEGORY_FOOD -> R.string.category_food
            CATEGORY_GROCERIES -> R.string.category_groceries
            CATEGORY_TRANSPORT -> R.string.category_transport
            CATEGORY_SHOPPING -> R.string.category_shopping
            CATEGORY_HOME -> R.string.category_home
            CATEGORY_HEALTH -> R.string.category_health
            CATEGORY_FUN -> R.string.category_fun
            CATEGORY_TRAVEL -> R.string.category_travel
            CATEGORY_OTHER -> R.string.category_other
            else -> null
        }

        // VERIFY: Built-in categories list (food, groceries, transport, shopping, home, health, fun, travel, other).
        val DEFAULT_CATEGORIES: List<CategoryEntity> = listOf(
            CategoryEntity(
                id = CATEGORY_FOOD,
                icon = "restaurant",
                sortOrder = 0,
                isHidden = false,
                isBuiltin = true,
            ),
            CategoryEntity(
                id = CATEGORY_GROCERIES,
                icon = "shopping_cart",
                sortOrder = 1,
                isHidden = false,
                isBuiltin = true,
            ),
            CategoryEntity(
                id = CATEGORY_TRANSPORT,
                icon = "directions_car",
                sortOrder = 2,
                isHidden = false,
                isBuiltin = true,
            ),
            CategoryEntity(
                id = CATEGORY_SHOPPING,
                icon = "shopping_bag",
                sortOrder = 3,
                isHidden = false,
                isBuiltin = true,
            ),
            CategoryEntity(
                id = CATEGORY_HOME,
                icon = "home",
                sortOrder = 4,
                isHidden = false,
                isBuiltin = true,
            ),
            CategoryEntity(
                id = CATEGORY_HEALTH,
                icon = "medical_services",
                sortOrder = 5,
                isHidden = false,
                isBuiltin = true,
            ),
            CategoryEntity(
                id = CATEGORY_FUN,
                icon = "movie",
                sortOrder = 6,
                isHidden = false,
                isBuiltin = true,
            ),
            CategoryEntity(
                id = CATEGORY_TRAVEL,
                icon = "flight",
                sortOrder = 7,
                isHidden = false,
                isBuiltin = true,
            ),
            CategoryEntity(
                id = CATEGORY_OTHER,
                icon = "more_horiz",
                sortOrder = 8,
                isHidden = false,
                isBuiltin = true,
            ),
        )
    }
}
