package com.photocleaner.domain.model

enum class Classification {
    USELESS,
    KEEP,
    UNCERTAIN,
    UNKNOWN;

    val displayName: String
        get() = when (this) {
            USELESS -> "无用"
            KEEP -> "保留"
            UNCERTAIN -> "待定"
            UNKNOWN -> "未分类"
        }

    val displayNameEn: String
        get() = name.lowercase()
}
