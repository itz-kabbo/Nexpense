package com.yourco.spend.feature.record

import androidx.compose.runtime.Immutable
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.parser.Confidence

sealed interface EntryPreview {
    data object Empty : EntryPreview
    data class NeedsAmount(val message: String = "") : EntryPreview
    data class Valid(
        val amountMinor: Long,
        val formattedAmount: String,
        val note: String,
        val categoryId: String,
        val categoryName: String,
        val confidence: Confidence,
        val categoryNameRes: Int? = CategoryEntity.defaultNameRes(categoryId),
    ) : EntryPreview
}

@Immutable
data class EntryUiState(
    val text: String = "",
    val preview: EntryPreview = EntryPreview.Empty,
    val selectedCategoryOverride: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val currencyCode: String = "USD",
)
