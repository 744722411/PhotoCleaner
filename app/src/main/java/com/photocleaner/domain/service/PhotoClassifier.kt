package com.photocleaner.domain.service

import com.photocleaner.domain.model.Photo

/**
 * Domain service responsible for on-device image analysis (blur / blank /
 * screenshot / document detection, perceptual dHash). Pure detection logic —
 * does not perform any data access.
 */
interface PhotoClassifier {
    suspend fun classify(photo: Photo): Photo
}
