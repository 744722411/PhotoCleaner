package com.photocleaner.service

data class ScanLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val photoName: String = "",
    val status: LogStatus = LogStatus.INFO,
    val message: String = "",
    val details: String = ""
)

enum class LogStatus {
    PROCESSING,
    SUCCESS,
    LOCAL_HIT,
    SKIP,
    ERROR,
    INFO
}
