package com.photocleaner.util

import android.graphics.Bitmap
import android.graphics.Color

object ImageUtils {
    private const val BLANK_THRESHOLD = 15.0
    private const val SAMPLE_STEP = 5

    fun isBlank(bitmap: Bitmap): Boolean {
        var totalBrightness = 0.0
        var count = 0

        val rowPixels = IntArray(bitmap.width)
        for (y in 0 until bitmap.height step SAMPLE_STEP) {
            bitmap.getPixels(rowPixels, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (x in 0 until bitmap.width step SAMPLE_STEP) {
                val pixel = rowPixels[x]
                val brightness = (Color.red(pixel) * 0.299 +
                        Color.green(pixel) * 0.587 +
                        Color.blue(pixel) * 0.114) / 255.0
                totalBrightness += brightness
                count++
            }
        }

        if (count == 0) return false
        val avgBrightness = totalBrightness / count

        // Check if almost all black (near 0) or almost all white (near 1)
        if (avgBrightness < 0.02 || avgBrightness > 0.98) return true

        // Check variance - very low variance means uniform color
        var variance = 0.0
        for (y in 0 until bitmap.height step SAMPLE_STEP) {
            bitmap.getPixels(rowPixels, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (x in 0 until bitmap.width step SAMPLE_STEP) {
                val pixel = rowPixels[x]
                val brightness = (Color.red(pixel) * 0.299 +
                        Color.green(pixel) * 0.587 +
                        Color.blue(pixel) * 0.114) / 255.0
                val diff = brightness - avgBrightness
                variance += diff * diff
            }
        }
        variance /= count

        return variance < 0.001
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1fMB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2fGB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
