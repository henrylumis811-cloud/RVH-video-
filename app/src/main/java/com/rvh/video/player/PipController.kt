package com.rvh.video.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.util.Rational

/**
 * PiP auto-enter-on-leave (the system automatically puts the app into PiP
 * when the user navigates away, e.g. presses Home) was only made reliable
 * via PictureInPictureParams.Builder.setAutoEnterEnabled starting API 31
 * (Android 12). On API 30 (Android 11) that builder method doesn't exist,
 * so we can't rely on it — instead Android 11 requires an explicit call to
 * Activity.enterPictureInPictureMode() at the moment the user leaves,
 * which we trigger from Activity.onUserLeaveHint().
 */
object PipController {

    private val VIDEO_ASPECT_RATIO = Rational(16, 9)

    // Last known on-screen bounds of the actual video surface, in window
    // coordinates. Without passing this as sourceRectHint, the system
    // falls back to scaling the ENTIRE Activity layout (title row,
    // prev/next controls, everything) down into the floating PiP window —
    // which is exactly why the PiP window was showing cramped title text
    // squeezed in alongside the video instead of just the video.
    private var lastSourceRect: Rect? = null

    /**
     * Call this once when music playback starts (API 31+ only) so the
     * system knows to auto-enter PiP if the user backgrounds the app —
     * no-op below API 31.
     */
    fun configureAutoEnter(activity: Activity, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(VIDEO_ASPECT_RATIO)
                .setAutoEnterEnabled(enabled)
            lastSourceRect?.let { builder.setSourceRectHint(it) }
            activity.setPictureInPictureParams(builder.build())
        }
        // API 30: nothing to configure ahead of time — see enterManually().
    }

    /**
     * Called from the video surface's onGloballyPositioned so the system
     * always has an up-to-date crop region — the video's on-screen bounds
     * can change (e.g. the two-stage back handling rotating between
     * landscape and portrait), so this isn't a one-time setup call.
     */
    fun updateSourceRectHint(activity: Activity, bounds: Rect) {
        lastSourceRect = bounds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // setSourceRectHint requires API 26+
            val builder = PictureInPictureParams.Builder().setSourceRectHint(bounds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAspectRatio(VIDEO_ASPECT_RATIO)
            }
            activity.setPictureInPictureParams(builder.build())
        }
    }

    /**
     * Explicit PiP trigger — used by the mini-bar's PiP button on every
     * supported version, and also called from onUserLeaveHint on API 30
     * specifically, since that OS version has no auto-enter mechanism at all.
     */
    fun enterManually(activity: Activity) {
        val builder = PictureInPictureParams.Builder().setAspectRatio(VIDEO_ASPECT_RATIO)
        lastSourceRect?.let { builder.setSourceRectHint(it) }
        activity.enterPictureInPictureMode(builder.build())
    }

    fun isPreAutoEnterOs(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
}
