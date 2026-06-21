package com.photocleaner.ui.scan

data class ScanLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val photoName: String = "",
    val status: LogStatus = LogStatus.INFO,
    val message: String = "",
    val details: String = ""
)

enum class LogStatus {
    PROCESSING,  // Currently being processed
    SUCCESS,     // AI analysis succeeded
    LOCAL_HIT,   // Local detection found issue
    SKIP,        // Skipped (e.g. no API key)
    ERROR,       // Error occurred
    INFO         // General info message
}
