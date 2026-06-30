package com.photocleaner.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

object ScreenshotDetector {
    private val SCREENSHOT_PATTERNS = listOf(
        "screenshot", "Screenshot", "Screenshot_", "屏幕截图", "截屏",
        "screen_shot", "screen-shot", "IMG_.*SCREEN", "SCR_"
    )

    private val KNOWN_RESOLUTIONS = setOf(
        1080 to 1920, 1920 to 1080,
        1440 to 2560, 2560 to 1440,
        1080 to 2400, 2400 to 1080,
        1220 to 2712, 2712 to 1220,
        1280 to 720, 720 to 1280,
        1080 to 2340, 2340 to 1080,
        1170 to 2532, 2532 to 1170,
        1284 to 2778, 2778 to 1284,
        1290 to 2796, 2796 to 1290,
        1176 to 2556, 2556 to 1176,
        1440 to 3200, 3200 to 1440,
        1080 to 2340, 2340 to 1080,
        1644 to 3840, 3840 to 1644,
        1440 to 3120, 3120 to 1440
    )

    fun isScreenshot(
        context: Context,
        uri: Uri,
        displayName: String,
        width: Int,
        height: Int
    ): Boolean {
        // Check filename patterns
        val nameMatch = SCREENSHOT_PATTERNS.any { pattern ->
            displayName.contains(pattern, ignoreCase = true) ||
                    Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(displayName)
        }
        if (nameMatch) return true

        // Check resolution
        val resolutionMatch = KNOWN_RESOLUTIONS.any { (w, h) ->
            (width == w && height == h) || (width == h && height == w)
        }

        // Check for no EXIF camera data (screenshots typically lack camera EXIF)
        val hasNoCameraExif = try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                make.isNullOrEmpty() && model.isNullOrEmpty()
            } ?: false
        } catch (_: Exception) {
            true
        }

        return resolutionMatch && hasNoCameraExif
    }
}
