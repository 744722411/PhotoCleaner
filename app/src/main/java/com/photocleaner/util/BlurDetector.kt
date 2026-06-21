package com.photocleaner.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object BlurDetector {
    private const val BLUR_THRESHOLD = 100.0
    private const val SAMPLE_SIZE = 4

    fun isBlurry(bitmap: Bitmap): Boolean {
        val gray = toGrayscale(bitmap)
        val laplacianVariance = computeLaplacianVariance(gray)
        return laplacianVariance < BLUR_THRESHOLD
    }

    private fun toGrayscale(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width / SAMPLE_SIZE
        val height = bitmap.height / SAMPLE_SIZE
        val gray = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x * SAMPLE_SIZE, y * SAMPLE_SIZE)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                gray[y][x] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }
        return gray
    }

    private fun computeLaplacianVariance(gray: Array<IntArray>): Double {
        val height = gray.size
        if (height < 3) return 0.0
        val width = gray[0].size
        if (width < 3) return 0.0

        val laplacian = Array(height - 2) { IntArray(width - 2) }
        var sum = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val value = -gray[y - 1][x] - gray[y + 1][x] -
                        gray[y][x - 1] - gray[y][x + 1] +
                        4 * gray[y][x]
                laplacian[y - 1][x - 1] = value
                sum += value
                count++
            }
        }

        if (count == 0) return 0.0
        val mean = sum / count
        var variance = 0.0

        for (row in laplacian) {
            for (v in row) {
                val diff = v - mean
                variance += diff * diff
            }
        }

        return variance / count
    }
}
