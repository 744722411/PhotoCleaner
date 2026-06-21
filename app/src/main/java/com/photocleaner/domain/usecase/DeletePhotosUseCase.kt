package com.photocleaner.domain.usecase

import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeletePhotosUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    suspend operator fun invoke(photos: List<Photo>): Unit = withContext(Dispatchers.IO) {
        repository.deletePhotos(photos)
    }

    suspend fun restore(photos: List<Photo>): Unit = withContext(Dispatchers.IO) {
        repository.restorePhotos(photos)
    }
}
