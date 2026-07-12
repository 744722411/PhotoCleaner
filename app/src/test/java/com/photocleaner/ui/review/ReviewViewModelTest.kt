package com.photocleaner.ui.review

import com.photocleaner.MainDispatcherRule
import com.photocleaner.data.service.TrashService
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.usecase.DeletePhotosUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<PhotoRepository>(relaxed = true)
    private val trashService = mockk<TrashService>()
    private lateinit var viewModel: ReviewViewModel
    private val photo = Photo(
        id = 1,
        uri = "content://media/external/images/media/1",
        displayName = "photo.jpg",
        mimeType = "image/jpeg",
        width = 100,
        height = 100,
        size = 1024,
        dateAdded = 1,
        dateModified = 1,
        classification = Classification.USELESS
    )

    @Before
    fun setUp() {
        every { repository.getAllPhotos() } returns flowOf(listOf(photo))
        viewModel = ReviewViewModel(repository, DeletePhotosUseCase(repository), trashService)
    }

    @Test
    fun cancelSystemTrashRestoresPendingPhoto() = runTest(mainDispatcherRule.dispatcher) {
        advanceUntilIdle()
        coEvery { trashService.createDeletePendingIntent(any()) } returns mockk()
        viewModel.deletePhoto(photo)
        advanceUntilIdle()
        viewModel.onTrashCanceled()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleteRequestInFlight)
        coVerify(exactly = 0) { repository.deletePhotos(any()) }
    }

    @Test
    fun confirmedSystemTrashUpdatesLocalState() = runTest(mainDispatcherRule.dispatcher) {
        advanceUntilIdle()
        val pendingIntent = mockk<android.app.PendingIntent>()
        coEvery { trashService.createDeletePendingIntent(any()) } returns pendingIntent
        coEvery { repository.findDeletedPhotoIds(any()) } returns listOf(photo.id)
        val event = async { viewModel.event.first() }
        viewModel.deletePhoto(photo)
        advanceUntilIdle()
        assertTrue(event.await() is ReviewEvent.LaunchTrashIntent)
        viewModel.onTrashConfirmed()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deletePhotos(listOf(photo)) }
    }

    @Test
    fun unverifiedDeleteRestoresPhotoAndReportsFailure() = runTest(mainDispatcherRule.dispatcher) {
        advanceUntilIdle()
        coEvery { trashService.createDeletePendingIntent(any()) } returns mockk()
        coEvery { repository.findDeletedPhotoIds(any()) } returns emptyList()

        viewModel.deletePhoto(photo)
        advanceUntilIdle()
        viewModel.onTrashConfirmed()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.deletePhotos(any()) }
        assertFalse(viewModel.uiState.value.isDeleteRequestInFlight)
        assertTrue(viewModel.uiState.value.error?.contains("1 张照片") == true)
    }

    @Test
    fun failedTrashRequestClearsPendingState() = runTest(mainDispatcherRule.dispatcher) {
        advanceUntilIdle()
        coEvery { trashService.createDeletePendingIntent(any()) } returns null

        viewModel.deletePhoto(photo)
        viewModel.commitPendingDeletes()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleteRequestInFlight)
        assertTrue(viewModel.uiState.value.error != null)
    }
}
