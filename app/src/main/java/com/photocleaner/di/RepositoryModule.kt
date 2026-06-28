package com.photocleaner.di

import com.photocleaner.data.classification.PhotoClassifierImpl
import com.photocleaner.data.repository.PhotoRepositoryImpl
import com.photocleaner.data.repository.SettingsRepositoryImpl
import com.photocleaner.data.service.MediaStoreTrashService
import com.photocleaner.data.service.TrashService
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.repository.SettingsRepository
import com.photocleaner.domain.service.PhotoClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPhotoClassifier(impl: PhotoClassifierImpl): PhotoClassifier

    @Binds
    @Singleton
    abstract fun bindTrashService(impl: MediaStoreTrashService): TrashService
}
