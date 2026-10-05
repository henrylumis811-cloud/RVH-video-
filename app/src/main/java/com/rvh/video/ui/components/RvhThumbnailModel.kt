package com.rvh.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import com.rvh.video.data.local.VideoThumbnailStore
import kotlinx.coroutines.withContext

/**
 * Resolves a video's canonical thumbnail without doing frame extraction on the
 * Compose thread. Once generated, every screen points Coil at the same JPEG.
 */
@Composable
fun rememberRvhThumbnailModel(
    uri: String,
    dateModifiedEpochSeconds: Long,
    widthPx: Int,
    heightPx: Int,
): Any? {
    val context = LocalContext.current
    val model by produceState<Any?>(
        initialValue = VideoThumbnailStore.fileFor(context, uri, dateModifiedEpochSeconds)
            .takeIf { it.exists() && it.length() > 0L },
        uri,
        dateModifiedEpochSeconds,
        widthPx,
        heightPx,
    ) {
        val file = withContext(Dispatchers.IO) {
            VideoThumbnailStore.getOrCreate(
                context = context.applicationContext,
                uri = uri,
                dateModifiedEpochSeconds = dateModifiedEpochSeconds,
                widthPx = widthPx,
                heightPx = heightPx,
            )
        }
        if (file != null) value = file
    }
    return model
}
