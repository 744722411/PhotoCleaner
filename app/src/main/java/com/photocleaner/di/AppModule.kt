package com.photocleaner.di

import com.photocleaner.data.remote.OpenAIApi
import com.photocleaner.data.repository.PhotoRepositoryImpl
import com.photocleaner.data.repository.SettingsRepository
import com.photocleaner.data.remote.dto.MessageContent
import com.photocleaner.data.remote.dto.ImageContent
import com.photocleaner.domain.repository.PhotoRepository
import com.squareup.moshi.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(MessageContentAdapterFactory())
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
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .addInterceptor { chain ->
            val currentBaseUrl = settingsRepository.getBaseUrlSyncMemory()
            val original = chain.request()
            try {
                if (currentBaseUrl.isNotBlank()) {
                    val normalizedUrl = if (currentBaseUrl.endsWith("/")) currentBaseUrl else "$currentBaseUrl/"
                    val baseUri = java.net.URI(normalizedUrl)
                    val scheme = baseUri.scheme ?: "https"
                    val host = baseUri.host
                    if (!host.isNullOrBlank()) {
                        val urlBuilder = original.url.newBuilder()
                            .scheme(scheme)
                            .host(host)
                        if (baseUri.port > 0) {
                            urlBuilder.port(baseUri.port)
                        }
                        val newUrl = urlBuilder.build()
                        val newRequest = original.newBuilder().url(newUrl).build()
                        chain.proceed(newRequest)
                    } else {
                        chain.proceed(original)
                    }
                } else {
                    chain.proceed(original)
                }
            } catch (e: Exception) {
                android.util.Log.e("AppModule", "Failed to rewrite URL: $currentBaseUrl", e)
                chain.proceed(original)
            }
        }
        .addInterceptor { chain ->
            val apiKey = settingsRepository.getApiKeySyncMemory()
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

/**
 * Moshi Factory that handles MessageContent polymorphism.
 * MessageContent can be either a plain string or an array of image objects.
 */
class MessageContentAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != MessageContent::class.java) return null
        return MessageContentJsonAdapter(moshi)
    }
}

private class MessageContentJsonAdapter(private val moshi: Moshi) : JsonAdapter<MessageContent>() {
    private val imageListAdapter: JsonAdapter<List<ImageContent>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, ImageContent::class.java)
    )

    override fun fromJson(reader: JsonReader): MessageContent? {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> {
                MessageContent.TextContent(reader.nextString())
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = imageListAdapter.fromJson(reader) ?: emptyList()
                MessageContent.ImageListContent(list)
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: MessageContent?) {
        when (value) {
            is MessageContent.TextContent -> writer.value(value.text)
            is MessageContent.ImageListContent -> {
                imageListAdapter.toJson(writer, value.images)
            }
            null -> writer.nullValue()
        }
    }
}
