package com.rvh.video.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * "Glass" here means translucent tint + hairline border + clipped shape —
 * deliberately NOT a RenderEffect/graphicsLayer blur.
 *
 * Earlier versions of this modifier wrapped a graphicsLayer with
 * RenderEffect.createBlurEffect around the panel's own content on API 31+.
 * That was a real bug, not a cosmetic one: graphicsLayer's blur applies to
 * everything rendered INSIDE that layer, which includes the panel's own
 * children — so every card using this modifier (Profile settings rows,
 * search fields, the background-playback bar, duration badges) was
 * blurring its own text and icons into illegibility, not blurring
 * background content bleeding through from behind.
 *
 * True backdrop-only blur (sampling already-composited pixels behind a
 * layer, leaving the layer's own content sharp) needs either a separate
 * captured bitmap of the background or platform-level window blur
 * support — not a straightforward Modifier for arbitrary sibling content
 * in Compose, and moot anyway for anything sitting over ExoPlayer's
 * SurfaceView output (see ShortsCommentSheet's note on that). Rather than
 * ship a blur that's broken for some panels and silently absent for
 * others, this keeps the visual language (translucency, subtle depth,
 * layered elevation) consistent everywhere and correct everywhere,
 * uniformly across Android 11-16 — no API-level branching needed for
 * this anymore.
 */
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    blurRadius: Dp = 24.dp, // kept as a no-op param so existing call sites don't need touching; no longer used
    tint: androidx.compose.ui.graphics.Color = GlassTintLight,
    borderColor: androidx.compose.ui.graphics.Color = GlassBorder,
): Modifier = this
    .clip(shape)
    .background(tint, shape)
    .border(1.dp, borderColor, shape)

/** Convenience: the mockup's floating pill buttons (message icon, mini-player bar). */
fun Modifier.glassPill(blurRadius: Dp = 18.dp) = glassSurface(
    shape = RoundedCornerShape(50),
    blurRadius = blurRadius,
)
