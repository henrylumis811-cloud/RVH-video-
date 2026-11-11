package com.rvh.video

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.rvh.video.player.PlaybackClient
import com.rvh.video.player.PlaybackSessionStore

/** Application-wide UI dependencies. The playback engine itself is service-owned. */
class RvhVideoApp : Application(), ImageLoaderFactory {

    /** UI-side playback boundary; the actual player is owned by PlaybackService. */
    val playbackClient: PlaybackClient by lazy { PlaybackClient(PlaybackSessionStore(this)) }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("thumbnail_cache"))
                    .maxSizePercent(0.06)
                    .build()
            }
            .build()
}
