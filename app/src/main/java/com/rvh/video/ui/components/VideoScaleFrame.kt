package com.rvh.video.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Provides a real target-ratio viewport for the player. Media3 controls how
 * the video is fitted inside this viewport; this container controls the
 * viewport's actual 16:9/4:3 geometry.
 */
@Composable
fun VideoScaleFrame(
    scaleMode: VideoScaleMode,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    if (scaleMode.targetAspectRatio == null) {
        Box(modifier = modifier.fillMaxSize()) {
            content(Modifier.fillMaxSize())
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val targetRatio = scaleMode.targetAspectRatio ?: 1f
        val containerRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else targetRatio
        val frameWidth = if (containerRatio >= targetRatio) {
            maxHeight * targetRatio
        } else {
            maxWidth
        }
        val frameHeight = if (containerRatio >= targetRatio) {
            maxHeight
        } else {
            maxWidth / targetRatio
        }

        content(Modifier.width(frameWidth).height(frameHeight))
    }
}
