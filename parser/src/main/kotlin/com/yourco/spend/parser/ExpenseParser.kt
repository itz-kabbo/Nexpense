package com.yourco.spend.parser

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.util.regex.Pattern

class ExpenseParser(
    private val lexicon: Lexicon = Lexicon.default,
    private val learned: (normalizedNote: String) -> String? = { null },
) {
    companion object {
        private val currencyMarks = Regex("""(?:NT|US)?[$¥€£₩]|[元塊]""", RegexOption.IGNORE_CASE)
        private val spaces = Regex("""[ ]+""")
        private val numberPattern = Pattern.compile(
            """(?<![A-Za-z0-9])(?<![A-Za-z]-)""" +
                """([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:[.,]([0-9]{1,2}))?""" +
                """(?![A-Za-z0-9])(?!-[A-Za-z])"""
        )
        private val maxAmount = BigDecimal(10_000_000)
    }

    private class KeywordRule(
        val key: String,
        val category: String,
        val isCjk: Boolean,
        val paddedKey: String,
    )

    private class NumberHit(
        val startIndex: Int,
        val endIndex: Int,
        val whole: String,
        val frac: String,
    )

    private val rules: Array<KeywordRule> = lexicon.keywords.entries.map { (key, cat) ->
        val cjk = key.any { it.code >= 0x2E80 }
        KeywordRule(
            key = key,
            category = cat,
            isCjk = cjk,
            paddedKey = if (cjk) key else " $key ",
        )
    }.toTypedArray()

    private val fuzzyCandidates: Array<KeywordRule> = rules
        .filter { !it.isCjk && it.key.length >= 5 }
        .toTypedArray()

    fun parse(input: String, fractionDigits: Int = 2): ParseResult {
        val digits = fractionDigits.coerceAtLeast(0)
        val normalized = if (Normalizer.isNormalized(input, Normalizer.Form.NFKC)) {
            input
        } else {
            Normalizer.normalize(input, Normalizer.Form.NFKC)
        }
        val stripped = normalized.replace(currencyMarks, " ")
        val text = (if (stripped.contains("  ")) stripped.replace(spaces, " ") else stripped).trim()

        val matcher = numberPattern.matcher(text)
        var firstHit: NumberHit? = null
        var lastHit: NumberHit? = null
        var startHit: NumberHit? = null

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val whole = matcher.group(1).replace(",", "")
            val frac = matcher.group(2) ?: ""
            val hit = NumberHit(start, end, whole, frac)
            if (firstHit == null) firstHit = hit
            if (start == 0 && startHit == null) startHit = hit
            if (end == text.length) lastHit = hit
        }

        val hit = startHit ?: lastHit ?: firstHit ?: return ParseResult.NoAmount

        val value = BigDecimal(if (hit.frac.isEmpty()) hit.whole else "${hit.whole}.${hit.frac}")
            .setScale(digits, RoundingMode.HALF_UP)
        if (value.signum() <= 0 || value > maxAmount) return ParseResult.NoAmount
        val minor = value.movePointRight(digits).longValueExact()

        val noteRaw = text.removeRange(hit.startIndex, hit.endIndex)
        val noteCollapsed = if (noteRaw.contains("  ")) noteRaw.replace(spaces, " ") else noteRaw
        val note = noteCollapsed.trim(' ', ',', ';', '-', '.')

        val (categoryId, confidence) = resolve(note.lowercase())
        return ParseResult.Ok(ParsedExpense(minor, note, categoryId, confidence))
    }

    private fun resolve(note: String): Pair<String, Confidence> {
        if (note.isBlank()) return "other" to Confidence.LOW
        learned(note)?.let { return it to Confidence.HIGH }
        val padded = " $note "
        var bestCategory: String? = null
        var bestLength = -1

        for (rule in rules) {
            val matched = if (rule.isCjk) note.contains(rule.key) else padded.contains(rule.paddedKey)
            if (matched && rule.key.length > bestLength) {
                bestCategory = rule.category
                bestLength = rule.key.length
            }
        }
        if (bestCategory != null) return bestCategory to Confidence.HIGH

        for (word in note.split(' ')) {
            if (word.length < 5) continue
            for (rule in fuzzyCandidates) {
                if (editDistance(word, rule.key) <= 1) {
                    return rule.category to Confidence.LOW
                }
            }
        }
        return "other" to Confidence.LOW
    }
}

internal fun editDistance(a: String, b: String): Int {
    if (kotlin.math.abs(a.length - b.length) > 1) return 2
    val dp = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..b.length) {
            val tmp = dp[j]
            dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
            prev = tmp
        }
    }
    return dp[b.length]
}

// VERIFY: multi-item entry: whether original accepts several items in one entry is unknown.
private val itemSeparator = Regex("""[;；，]|,[ ]+""")

fun ExpenseParser.parseMany(input: String, fractionDigits: Int = 2): List<ParsedExpense> =
    input.lines()
        .flatMap { it.split(itemSeparator) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { (parse(it, fractionDigits) as? ParseResult.Ok)?.value }
