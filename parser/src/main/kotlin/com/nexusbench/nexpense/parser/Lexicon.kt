package com.nexusbench.nexpense.parser

class Lexicon(val keywords: Map<String, String>) {
    companion object {
        val default = Lexicon(
            buildMap {
                fun add(category: String, vararg words: String) = words.forEach { put(it, category) }
                add(
                    "food", "coffee", "latte", "tea", "boba", "breakfast", "brunch", "lunch", "dinner",
                    "snack", "pizza", "burger", "ramen", "restaurant", "beer", "wine", "uber eats",
                    "咖啡", "拿鐵", "茶", "早餐", "午餐", "晚餐", "宵夜", "便當", "飲料", "手搖", "餐廳"
                )
                add("groceries", "grocery", "groceries", "supermarket", "超市", "水果", "全聯")
                add(
                    "transport", "taxi", "uber", "bus", "train", "metro", "subway", "mrt", "gas", "fuel",
                    "parking", "toll", "計程車", "公車", "捷運", "高鐵", "火車", "加油", "停車", "過路費"
                )
                add("shopping", "clothes", "shoes", "amazon", "gift", "衣服", "禮物", "網購")
                add("home", "rent", "electricity", "wifi", "utilities", "bill", "房租", "電費", "水費", "帳單")
                add("health", "doctor", "pharmacy", "medicine", "gym", "dentist", "看醫生", "健身", "牙醫", "診所")
                add("fun", "movie", "cinema", "game", "concert", "netflix", "spotify", "電影", "遊戲", "演唱會", "訂閱")
                add("travel", "flight", "hotel", "airbnb", "hostel", "機票", "飯店", "住宿", "旅行")
            }
        )
    }
}
