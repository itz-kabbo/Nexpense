package com.yourco.spend.data.export

import com.yourco.spend.core.money.fractionDigits
import com.yourco.spend.data.repo.ExpenseRepository
import java.io.OutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val clock: Clock,
) {
    private val zoneId: ZoneId get() = clock.zone
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    suspend fun exportCsv(): String {
        val expenses = expenseRepository.getAll().filter { it.deletedAt == null }
        val sb = StringBuilder()
        sb.append("date,time,amount,currency,category,note\n")

        for (expense in expenses) {
            val date = expense.localDate
            val time = Instant.ofEpochMilli(expense.createdAt)
                .atZone(zoneId)
                .toLocalTime()
                .format(timeFormatter)
            val digits = fractionDigits(expense.currency)
            val amountStr = BigDecimal(expense.amountMinor).movePointLeft(digits).toPlainString()
            val currency = expense.currency
            val category = escapeCsv(expense.categoryId)
            val note = escapeCsv(expense.note)

            sb.append(date)
                .append(',')
                .append(time)
                .append(',')
                .append(amountStr)
                .append(',')
                .append(currency)
                .append(',')
                .append(category)
                .append(',')
                .append(note)
                .append('\n')
        }

        return sb.toString()
    }

    suspend fun export(outputStream: OutputStream) {
        val csv = exportCsv()
        outputStream.write(csv.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    private fun escapeCsv(value: String): String {
        val spreadsheetSafeValue = when (value.trimStart().firstOrNull()) {
            '=', '+', '-', '@' -> "'" + value
            else -> value
        }
        return if (spreadsheetSafeValue.contains(',') || spreadsheetSafeValue.contains('"') ||
            spreadsheetSafeValue.contains('\n') || spreadsheetSafeValue.contains('\r')
        ) {
            "\"" + spreadsheetSafeValue.replace("\"", "\"\"") + "\""
        } else {
            spreadsheetSafeValue
        }
    }
}
