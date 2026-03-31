package app.gamegrub.di

import android.content.Context
import app.gamegrub.NetworkMonitor
import app.gamegrub.ui.utils.AnimatedPngDecoder
import app.gamegrub.ui.utils.IconDecoder
import coil.ImageLoader
import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okio.Path.Companion.toOkioPath

@InstallIn(SingletonComponent::class)
@Module
class ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        val memoryCache = MemoryCache.Builder(context)
            .maxSizePercent(0.1)
            .strongReferencesEnabled(true)
            .build()

        val diskCache = DiskCache.Builder()
            .maxSizePercent(0.03)
            .directory(context.cacheDir.resolve("image_cache").toOkioPath())
            .build()

        return ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache(memoryCache)
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache(diskCache)
            .components {
                add(
                    Interceptor { chain ->
                        val request = if (!NetworkMonitor.hasInternet.value) {
                            chain.request.newBuilder()
                                .networkCachePolicy(CachePolicy.DISABLED)
                                .build()
                        } else {
                            chain.request
                        }
                        chain.proceed(request)
                    },
                )
                add(IconDecoder.Factory())
                add(AnimatedPngDecoder.Factory())
            }
            .build()
    }
}
