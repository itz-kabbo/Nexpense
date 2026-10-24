package com.nexusbench.nexpense.parser

enum class Confidence { HIGH, MEDIUM, LOW }

data class ParsedExpense(
    val amountMinor: Long,
    val note: String,
    val categoryId: String,
    val confidence: Confidence,
)

sealed interface ParseResult {
    data class Ok(val value: ParsedExpense) : ParseResult
    data object NoAmount : ParseResult
}
