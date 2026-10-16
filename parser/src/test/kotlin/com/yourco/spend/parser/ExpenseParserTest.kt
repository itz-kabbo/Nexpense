package com.yourco.spend.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseParserTest {

    private val parser = ExpenseParser()

    // --- Appendix A Test Cases ---

    @Test
    fun parse_6_coffee() {
        val result = parser.parse("6 coffee")
        assertEquals(
            ParseResult.Ok(ParsedExpense(600L, "coffee", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_18_taxi() {
        val result = parser.parse("18 taxi")
        assertEquals(
            ParseResult.Ok(ParsedExpense(1800L, "taxi", "transport", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_42_dinner() {
        val result = parser.parse("42 dinner")
        assertEquals(
            ParseResult.Ok(ParsedExpense(4200L, "dinner", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_12_point_5_lunch() {
        val result = parser.parse("12.5 lunch")
        assertEquals(
            ParseResult.Ok(ParsedExpense(1250L, "lunch", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_coffee_6() {
        val result = parser.parse("coffee 6")
        assertEquals(
            ParseResult.Ok(ParsedExpense(600L, "coffee", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_dollar_6_50_latte() {
        val result = parser.parse("$6.50 latte")
        assertEquals(
            ParseResult.Ok(ParsedExpense(650L, "latte", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_NT_dollar_120_bento() {
        val result = parser.parse("NT$120 便當")
        assertEquals(
            ParseResult.Ok(ParsedExpense(12000L, "便當", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_lunch_150_cjk() {
        val result = parser.parse("午餐 150")
        assertEquals(
            ParseResult.Ok(ParsedExpense(15000L, "午餐", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_full_width_digits() {
        val result = parser.parse("１２．５ lunch")
        assertEquals(
            ParseResult.Ok(ParsedExpense(1250L, "lunch", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_1_200_rent() {
        val result = parser.parse("1,200 rent")
        assertEquals(
            ParseResult.Ok(ParsedExpense(120000L, "rent", "home", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_12_comma_5_pizza() {
        val result = parser.parse("12,5 pizza")
        assertEquals(
            ParseResult.Ok(ParsedExpense(1250L, "pizza", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_7_eleven_snack_35() {
        val result = parser.parse("7-eleven snack 35")
        assertEquals(
            ParseResult.Ok(ParsedExpense(3500L, "7-eleven snack", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_uber_eats_24() {
        val result = parser.parse("uber eats 24")
        assertEquals(
            ParseResult.Ok(ParsedExpense(2400L, "uber eats", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_uber_24() {
        val result = parser.parse("uber 24")
        assertEquals(
            ParseResult.Ok(ParsedExpense(2400L, "uber", "transport", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_steak_30_word_boundary() {
        val result = parser.parse("steak 30")
        assertEquals(
            ParseResult.Ok(ParsedExpense(3000L, "steak", "other", Confidence.LOW)),
            result
        )
    }

    @Test
    fun parse_15_xyzzyq_unknown_word() {
        val result = parser.parse("15 xyzzyq")
        assertEquals(
            ParseResult.Ok(ParsedExpense(1500L, "xyzzyq", "other", Confidence.LOW)),
            result
        )
    }

    @Test
    fun parse_6_cofee_fuzzy() {
        val result = parser.parse("6 cofee")
        assertEquals(
            ParseResult.Ok(ParsedExpense(600L, "cofee", "food", Confidence.LOW)),
            result
        )
    }

    @Test
    fun parse_12_point_5_lunch_fraction_digits_0() {
        val result = parser.parse("12.5 lunch", fractionDigits = 0)
        assertEquals(
            ParseResult.Ok(ParsedExpense(13L, "lunch", "food", Confidence.HIGH)),
            result
        )
    }

    @Test
    fun parse_taxi_no_amount() {
        val result = parser.parse("taxi")
        assertEquals(ParseResult.NoAmount, result)
    }

    @Test
    fun parse_0_coffee_no_amount() {
        val result = parser.parse("0 coffee")
        assertEquals(ParseResult.NoAmount, result)
    }

    @Test
    fun parse_empty_string_no_amount() {
        val result = parser.parse("")
        assertEquals(ParseResult.NoAmount, result)
    }

    @Test
    fun parse_above_limit_no_amount() {
        val result = parser.parse("99999999 gift")
        assertEquals(ParseResult.NoAmount, result)
    }

    // --- Learned Mapping Precedence ---

    @Test
    fun parse_learned_mapping_precedence() {
        val learningParser = ExpenseParser(
            learned = { note -> if (note == "coffee") "groceries" else null }
        )
        val result = learningParser.parse("6 coffee")
        assertEquals(
            ParseResult.Ok(ParsedExpense(600L, "coffee", "groceries", Confidence.HIGH)),
            result
        )
    }

    // --- Multi-Item Parsing (parseMany) ---

    @Test
    fun parseMany_multiple_items_semicolon_and_newline() {
        val results = parser.parseMany("6 coffee; 18 taxi\n42 dinner")
        assertEquals(3, results.size)
        assertEquals(ParsedExpense(600L, "coffee", "food", Confidence.HIGH), results[0])
        assertEquals(ParsedExpense(1800L, "taxi", "transport", Confidence.HIGH), results[1])
        assertEquals(ParsedExpense(4200L, "dinner", "food", Confidence.HIGH), results[2])
    }

    @Test
    fun parseMany_chinese_comma_and_comma_space() {
        val results = parser.parseMany("6 coffee，18 taxi, 42 dinner")
        assertEquals(3, results.size)
        assertEquals(ParsedExpense(600L, "coffee", "food", Confidence.HIGH), results[0])
        assertEquals(ParsedExpense(1800L, "taxi", "transport", Confidence.HIGH), results[1])
        assertEquals(ParsedExpense(4200L, "dinner", "food", Confidence.HIGH), results[2])
    }

    @Test
    fun parseMany_preserves_thousands_and_decimal_commas() {
        val results = parser.parseMany("1,200 rent; 12,5 pizza")
        assertEquals(2, results.size)
        assertEquals(ParsedExpense(120000L, "rent", "home", Confidence.HIGH), results[0])
        assertEquals(ParsedExpense(1250L, "pizza", "food", Confidence.HIGH), results[1])
    }

    // --- Additional Edge Cases ---

    @Test
    fun parse_taiwan_currency_words() {
        val r1 = parser.parse("100元 咖啡")
        assertEquals(ParseResult.Ok(ParsedExpense(10000L, "咖啡", "food", Confidence.HIGH)), r1)

        val r2 = parser.parse("咖啡 100元")
        assertEquals(ParseResult.Ok(ParsedExpense(10000L, "咖啡", "food", Confidence.HIGH)), r2)

        val r3 = parser.parse("100塊 便當")
        assertEquals(ParseResult.Ok(ParsedExpense(10000L, "便當", "food", Confidence.HIGH)), r3)
    }

    @Test
    fun parse_number_in_middle() {
        val result = parser.parse("bought 15 apples")
        assertEquals(ParseResult.Ok(ParsedExpense(1500L, "bought apples", "other", Confidence.LOW)), result)
    }

    @Test
    fun parse_multiple_numbers_start_wins() {
        val result = parser.parse("6 coffee 18")
        assertEquals(ParseResult.Ok(ParsedExpense(600L, "coffee 18", "food", Confidence.HIGH)), result)
    }

    @Test
    fun parse_amount_only_without_note() {
        val result = parser.parse("6")
        assertEquals(ParseResult.Ok(ParsedExpense(600L, "", "other", Confidence.LOW)), result)
    }

    @Test
    fun parse_exact_limit_boundary() {
        val atLimit = parser.parse("10000000 rent")
        assertEquals(ParseResult.Ok(ParsedExpense(1000000000L, "rent", "home", Confidence.HIGH)), atLimit)

        val overLimit = parser.parse("10000000.01 rent")
        assertEquals(ParseResult.NoAmount, overLimit)
    }

    @Test
    fun parse_zero_rejected() {
        assertEquals(ParseResult.NoAmount, parser.parse("0 coffee"))
        assertEquals(ParseResult.NoAmount, parser.parse("0"))
        assertEquals(ParseResult.NoAmount, parser.parse("0.00"))
        assertEquals(ParseResult.NoAmount, parser.parse("0.00 coffee"))
    }

    @Test
    fun test_edit_distance_helper() {
        assertEquals(0, editDistance("coffee", "coffee"))
        assertEquals(1, editDistance("cofee", "coffee"))
        assertEquals(1, editDistance("coffee", "cofee"))
        assertEquals(1, editDistance("coffe", "coffee"))
        assertEquals(2, editDistance("cofe", "coffee")) // length diff 2
        assertEquals(2, editDistance("tea", "steak"))   // length diff 2
    }
}
