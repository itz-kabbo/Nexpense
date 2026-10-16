package com.yourco.spend.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.system.measureTimeMillis

class ExpenseParserFuzzAndBenchmarkTest {

    private val parser = ExpenseParser()

    @Test
    fun fuzzTest_100_000_random_strings_never_throw() {
        val random = Random(42)
        val charPool = (
            "0123456789" +
            "abcdefghijklmnopqrstuvwxyz" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            " \t\n\r" +
            ",.;:-_!@#$%^&*()+=[]{}|<>?/\\~`'\"" +
            "$¥€£₩元塊NT" +
            "咖啡拿鐵茶早餐午餐晚餐宵夜便當飲料手搖餐廳超市水果全聯" +
            "計程車公車捷運高鐵火車加油停車過路費衣服禮物網購房租電費水費帳單" +
            "看醫生健身牙醫診所電影遊戲演唱會訂閱機票飯店住宿旅行" +
            "１２３４５６７８９０．，；－"
        ).toCharArray()

        val sampleInputs = listOf(
            "", " ", "0", "0.00", "-1", "-5 coffee", "99999999", "10000000",
            "6 coffee", "18 taxi", "NT$120 便當", "12.5 lunch", "coffee 6",
            "$6.50 latte", "1,200 rent", "12,5 pizza", "7-eleven snack 35",
            "uber eats 24", "steak 30", "6 cofee", "午餐 150", "taxi"
        )

        for (i in 0 until 100_000) {
            val input = if (i < sampleInputs.size) {
                sampleInputs[i]
            } else if (random.nextInt(10) == 0) {
                // Occasionally mix known sample with random noise
                val base = sampleInputs[random.nextInt(sampleInputs.size)]
                val extraChar = charPool[random.nextInt(charPool.size)]
                "$base $extraChar"
            } else {
                val len = random.nextInt(40)
                val buf = CharArray(len) { charPool[random.nextInt(charPool.size)] }
                String(buf)
            }

            val fractionDigits = random.nextInt(4) // 0, 1, 2, 3

            try {
                parser.parse(input, fractionDigits)
                if (i % 5 == 0) {
                    parser.parseMany(input, fractionDigits)
                }
            } catch (t: Throwable) {
                throw AssertionError("ExpenseParser threw on fuzz iteration $i with input: \"$input\"", t)
            }
        }
    }

    @Test
    fun benchmark_10_000_parses_under_200_ms() {
        val sampleInputs = listOf(
            "6 coffee",
            "18 taxi",
            "NT$120 便當",
            "12.5 lunch",
            "coffee 6",
            "$6.50 latte",
            "1,200 rent",
            "12,5 pizza",
            "7-eleven snack 35",
            "uber eats 24",
            "steak 30",
            "6 cofee",
            "午餐 150",
            "taxi",
            "0 coffee",
            "１２．５ lunch",
            "42 dinner",
            "15 xyzzyq"
        )

        // Warmup: 10,000 iterations to allow JVM HotSpot C2 JIT compilation
        for (i in 0 until 10_000) {
            val input = sampleInputs[i % sampleInputs.size]
            parser.parse(input, 2)
        }

        // Measured run: 10,000 parses
        val count = 10_000
        val elapsedMs = measureTimeMillis {
            for (i in 0 until count) {
                val input = sampleInputs[i % sampleInputs.size]
                parser.parse(input, 2)
            }
        }

        println("Benchmark: $count parses completed in ${elapsedMs}ms (${(elapsedMs * 1000.0) / count} µs/parse)")
        assertTrue(
            "Expected 10,000 parses in under 200 ms, but took ${elapsedMs}ms",
            elapsedMs < 200
        )
    }
}
