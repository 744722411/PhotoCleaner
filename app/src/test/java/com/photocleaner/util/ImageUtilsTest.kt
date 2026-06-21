package com.photocleaner.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageUtilsTest {

    @Test
    fun testFormatFileSize() {
        assertEquals("500B", ImageUtils.formatFileSize(500))
        assertEquals("1KB", ImageUtils.formatFileSize(1024))
        assertEquals("1.5MB", ImageUtils.formatFileSize(1024 * 1024 + 512 * 1024))
        assertEquals("2.00GB", ImageUtils.formatFileSize(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun testHammingDistance() {
        val hash1 = 0b11110000L
        val hash2 = 0b11100001L
        // 11110000
        // 11100001
        // XOR: 00010001 -> 2 bits
        assertEquals(2, ImageUtils.hammingDistance(hash1, hash2))
    }
}
