package com.jmtube.app.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import coil3.video.VideoFrameDecoder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.jmtube.app.BuildConfig
import com.jmtube.app.data.localmedia.MediaRetrieverDecoderFactory
import com.jmtube.app.data.localmedia.MediaStoreThumbnailFetcher
import com.jmtube.app.data.recommendation.FlowNeuroEngine
import com.jmtube.app.innertube.YouTube
import com.jmtube.app.player.cache.PlayerCacheManager
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import javax.inject.Singleton

/** The image cache folder under the app's cache directory. */
const val IMAGE_CACHE_DIR = "image_cache"

private const val AUTOMATIC_DISK_SHARE = 0.02

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideYouTube(): YouTube = YouTube

    /** Delegates to the legacy singleton so injected consumers share the one engine the app already runs. */
    @Provides
    @Singleton
    fun provideFlowNeuroEngine(
        @ApplicationContext context: Context,
    ): FlowNeuroEngine = FlowNeuroEngine.getInstance(context)

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader =
        ImageLoader
            .Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
                add(MediaRetrieverDecoderFactory(VideoFrameDecoder.Factory()))
                add(MediaStoreThumbnailFetcher.Factory(context))
            }.memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizePercent(context, 0.10)
                    .build()
            }.diskCache {
                // Coil builds this lazily on its first disk access, off the main thread, so the size
                // setting is read there rather than on the cold-start path.
                val limit = PlayerCacheManager.configuredLimits(context).artworkBytes
                DiskCache
                    .Builder()
                    .directory(context.cacheDir.resolve(IMAGE_CACHE_DIR).toOkioPath())
                    .apply { if (limit != null) maxSizeBytes(limit) else maxSizePercent(AUTOMATIC_DISK_SHARE) }
                    .build()
            }.crossfade(true)
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
            .build()
}
