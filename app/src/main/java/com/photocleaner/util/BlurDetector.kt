package com.photocleaner.util

import android.graphics.Bitmap
import android.graphics.Color

object BlurDetector {
    private const val BLUR_THRESHOLD = 100.0
    private const val SAMPLE_SIZE = 4

    fun isBlurry(bitmap: Bitmap): Boolean {
        val width = bitmap.width / SAMPLE_SIZE
        val height = bitmap.height / SAMPLE_SIZE
        if (width < 3 || height < 3) return false

        // Single flat allocation instead of Array<IntArray> to reduce GC pressure.
        val gray = IntArray(width * height)
        val rowPixels = IntArray(bitmap.width)

        var idx = 0
        for (y in 0 until height) {
            bitmap.getPixels(rowPixels, 0, bitmap.width, 0, y * SAMPLE_SIZE, bitmap.width, 1)
            for (x in 0 until width) {
                val pixel = rowPixels[x * SAMPLE_SIZE]
                gray[idx++] = (0.299 * Color.red(pixel) +
                    0.587 * Color.green(pixel) +
                    0.114 * Color.blue(pixel)).toInt()
            }
        }

        val laplacian = IntArray((width - 2) * (height - 2))
        var sum = 0.0
        var count = 0
        var lIdx = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val value = -gray[(y - 1) * width + x] - gray[(y + 1) * width + x] -
                    gray[y * width + x - 1] - gray[y * width + x + 1] +
                    4 * gray[y * width + x]
                laplacian[lIdx++] = value
                sum += value
                count++
            }
        }
        if (count == 0) return false

        val mean = sum / count
        var variance = 0.0
        for (v in laplacian) {
            val diff = v - mean
            variance += diff * diff
        }
        return (variance / count) < BLUR_THRESHOLD
    }
}
