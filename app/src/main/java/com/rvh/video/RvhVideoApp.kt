package com.rvh.video

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.rvh.video.player.PlayerManager

/** Application-wide dependencies that must have exactly one owner. */
class RvhVideoApp : Application(), ImageLoaderFactory {

    /** One app-level playback engine shared by Music and Movie playback. */
    val playerManager: PlayerManager by lazy { PlayerManager(this) }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("thumbnail_cache"))
                    .maxSizePercent(0.03)
                    .build()
            }
            .build()
}
