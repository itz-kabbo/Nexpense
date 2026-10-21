package com.yourco.spend.core.language

import java.util.Locale

data class LanguageItem(
    val tag: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean = false,
) {
    val searchKeywords: String = "$tag $nativeName $englishName".lowercase(Locale.ROOT)

    fun displayLabel(): String {
        return if (tag.isEmpty()) {
            nativeName
        } else {
            "$nativeName ($englishName)"
        }
    }
}

object LanguageCatalog {

    val SYSTEM_DEFAULT = LanguageItem(
        tag = "",
        nativeName = "System default",
        englishName = "System default",
    )

    private val LANGUAGES: List<LanguageItem> = listOf(
        SYSTEM_DEFAULT,
        LanguageItem("en", "English", "English"),
        LanguageItem("zh-TW", "繁體中文", "Traditional Chinese"),
        LanguageItem("zh-CN", "简体中文", "Simplified Chinese"),
        LanguageItem("es", "Español", "Spanish"),
        LanguageItem("fr", "Français", "French"),
        LanguageItem("de", "Deutsch", "German"),
        LanguageItem("ja", "日本語", "Japanese"),
        LanguageItem("ko", "한국어", "Korean"),
        LanguageItem("pt", "Português", "Portuguese"),
        LanguageItem("it", "Italiano", "Italian"),
        LanguageItem("ru", "Русский", "Russian"),
        LanguageItem("hi", "हिन्दी", "Hindi"),
        LanguageItem("bn", "বাংলা", "Bengali"),
        LanguageItem("ar", "العربية", "Arabic", isRtl = true),
        LanguageItem("tr", "Türkçe", "Turkish"),
        LanguageItem("vi", "Tiếng Việt", "Vietnamese"),
        LanguageItem("id", "Bahasa Indonesia", "Indonesian"),
        LanguageItem("th", "ไทย", "Thai"),
        LanguageItem("nl", "Nederlands", "Dutch"),
        LanguageItem("pl", "Polski", "Polish"),
        LanguageItem("sv", "Svenska", "Swedish"),
        LanguageItem("uk", "Українська", "Ukrainian"),
        LanguageItem("cs", "Čeština", "Czech"),
        LanguageItem("el", "Ελληνικά", "Greek"),
        LanguageItem("he", "עברית", "Hebrew", isRtl = true),
    )

    private val TAG_MAP: Map<String, LanguageItem> = LANGUAGES.associateBy { it.tag.lowercase(Locale.ROOT) }

    fun getAllLanguages(): List<LanguageItem> = LANGUAGES

    fun findLanguage(tag: String): LanguageItem? {
        val lower = tag.trim().lowercase(Locale.ROOT)
        TAG_MAP[lower]?.let { return it }

        // Also check if prefix matches (e.g. "zh" -> "zh-TW")
        if (lower.startsWith("zh")) {
            return if (lower.contains("cn") || lower.contains("hans")) TAG_MAP["zh-cn"] else TAG_MAP["zh-tw"]
        }
        val prefix = lower.substringBefore("-").substringBefore("_")
        return TAG_MAP[prefix] ?: SYSTEM_DEFAULT
    }

    fun searchLanguages(query: String): List<LanguageItem> {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return LANGUAGES
        return LANGUAGES.filter { it.searchKeywords.contains(trimmed) }
    }
}
