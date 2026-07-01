package com.photocleaner.data.classification

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.service.PhotoClassifier
import com.photocleaner.util.BlurDetector
import com.photocleaner.util.ImageUtils
import com.photocleaner.util.ScreenshotDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@Singleton
class PhotoClassifierImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoClassifier {

    override suspend fun classify(photo: Photo): Photo {
        return try {
            val uri = Uri.parse(photo.uri)

            // Step 1: fast screenshot detection (no bitmap decode needed).
            if (ScreenshotDetector.isScreenshot(context, uri, photo.displayName, photo.width, photo.height)) {
                return photo.copy(
                    isLocalUseless = true,
                    localReason = "截图",
                    classification = Classification.UNCERTAIN,
                    confidence = 0.7f,
                    category = "screenshot"
                )
            }

            // Step 2: decode a downsampled bitmap for heavier local detectors.
            val sampleSize = context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                calculateInSampleSize(options, 800, 800)
            } ?: 1

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                )
            } ?: return photo

            if (ImageUtils.isBlank(bitmap)) {
                return photo.copy(
                    isLocalUseless = true,
                    localReason = "空白照片",
                    classification = Classification.USELESS,
                    confidence = 0.95f,
                    category = "blank_photo"
                )
            }

            if (BlurDetector.isBlurry(bitmap)) {
                return photo.copy(
                    isLocalUseless = true,
                    localReason = "模糊照片",
                    classification = Classification.USELESS,
                    confidence = 0.9f,
                    category = "blurry_photo"
                )
            }

            var mlKitCategory = ""
            var mlKitClassification: Classification? = null
            try {
                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val labels = labeler.process(inputImage).await()
                val documentLabels = setOf("Receipt", "Document", "Text", "Font", "Barcode")
                for (label in labels) {
                    if (label.confidence > 0.7f && documentLabels.contains(label.text)) {
                        mlKitCategory = label.text.lowercase()
                        mlKitClassification = Classification.UNCERTAIN
                        break
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // ML Kit is optional; fall through to the plain dHash result.
            }

            val dHash = ImageUtils.computeDHash(bitmap)
            val withCategory = if (mlKitClassification != null) {
                photo.copy(
                    isLocalUseless = true,
                    localReason = "文档票据",
                    classification = Classification.UNCERTAIN,
                    confidence = 0.8f,
                    category = mlKitCategory
                )
            } else {
                photo
            }
            withCategory.copy(dHash = dHash)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            photo
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
