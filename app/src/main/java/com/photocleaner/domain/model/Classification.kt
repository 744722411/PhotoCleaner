package com.photocleaner.domain.model

enum class Classification {
    USELESS,
    KEEP,
    UNCERTAIN,
    UNKNOWN;

    val displayName: String
        get() = when (this) {
            USELESS -> "建议清理"
            KEEP -> "建议保留"
            UNCERTAIN -> "需人工审查"
            UNKNOWN -> "未分类"
        }

    val displayNameEn: String
        get() = name.lowercase()

    companion object {
        fun fromString(value: String): Classification =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}
