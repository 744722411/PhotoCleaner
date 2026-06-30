package com.photocleaner.util

import android.graphics.Bitmap
import android.graphics.Color

object ImageUtils {
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

    /**
     * Computes the difference hash (dHash) of a bitmap for perceptual duplicate detection.
     * Returns a 64-bit Long.
     */
    fun computeDHash(bitmap: Bitmap): Long {
        var hash = 0L
        try {
            // Resize to 9x8 for dHash
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
            val pixels = IntArray(72)
            scaledBitmap.getPixels(pixels, 0, 9, 0, 0, 9, 8)

            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val p1 = pixels[y * 9 + x]
                    val p2 = pixels[y * 9 + x + 1]

                    // Calculate grayscale brightness
                    val b1 = (Color.red(p1) * 299 + Color.green(p1) * 587 + Color.blue(p1) * 114) / 1000
                    val b2 = (Color.red(p2) * 299 + Color.green(p2) * 587 + Color.blue(p2) * 114) / 1000

                    hash = hash shl 1
                    if (b1 > b2) {
                        hash = hash or 1L
                    }
                }
            }
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return hash
    }

    /**
     * Calculates the Hamming distance between two dHashes.
     * Distance <= 5 usually means the images are visually identical.
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        val xor = hash1 xor hash2
        return java.lang.Long.bitCount(xor)
    }
}
