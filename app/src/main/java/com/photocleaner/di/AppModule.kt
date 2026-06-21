package com.photocleaner.di

import com.photocleaner.data.remote.OpenAIApi
import com.photocleaner.data.repository.PhotoRepositoryImpl
import com.photocleaner.data.repository.SettingsRepository
import com.photocleaner.domain.repository.PhotoRepository
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(settingsRepository: SettingsRepository): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (com.photocleaner.BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .addInterceptor { chain ->
            val currentBaseUrl = runBlocking { settingsRepository.getBaseUrlSync() }
            val normalizedUrl = if (currentBaseUrl.endsWith("/")) currentBaseUrl else "$currentBaseUrl/"
            val baseUri = java.net.URI(normalizedUrl)
            val original = chain.request()
            val urlBuilder = original.url.newBuilder()
                .scheme(baseUri.scheme)
                .host(baseUri.host)
            if (baseUri.port > 0) {
                urlBuilder.port(baseUri.port)
            }
            val newUrl = urlBuilder.build()
            val newRequest = original.newBuilder().url(newUrl).build()
            chain.proceed(newRequest)
        }
        .addInterceptor { chain ->
            val apiKey = runBlocking { settingsRepository.getApiKeySync() }
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer $apiKey")
                .build()
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.example.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAIApi(retrofit: Retrofit): OpenAIApi =
        retrofit.create(OpenAIApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository
}
