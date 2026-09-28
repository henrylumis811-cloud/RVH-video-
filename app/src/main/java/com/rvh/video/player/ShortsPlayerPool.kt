package com.rvh.video.player

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Shorts needs several ExoPlayer instances alive at once (current page +
 * immediate neighbors) so swiping feels instant rather than showing a
 * loading spinner each time — but keeping ALL scanned shorts' players
 * alive would blow memory on the low-end/older devices this app has to
 * run on (Android 11 floor). This pool enforces a hard cap: only pages
 * within [poolRadius] of the current index keep a live player; anything
 * further gets released outright, not just paused.
 *
 * Backed by a SnapshotStateMap (not a plain MutableMap): players are built
 * asynchronously inside a coroutine (see ShortsScreen's LaunchedEffect),
 * so a page's AndroidView may already have composed by the time its
 * player exists. AndroidView's `update` block only re-runs on
 * recomposition, and a plain map mutation wouldn't trigger one — the
 * page's video would stay blank. A SnapshotStateMap makes `playerFor`
 * reads inside `update` participate in Compose's snapshot system, so
 * assigning a player after the fact correctly triggers recomposition.
 */
class ShortsPlayerPool(
    private val context: Context,
    private val poolRadius: Int = 1,
) {
    private val appContext = context.applicationContext
    private val players = mutableStateMapOf<Int, ExoPlayer>()
    private val lifecyclePlayingIndices = mutableSetOf<Int>()

    /** Call whenever the pager settles on a new page. Builds players for the new window, releases the rest. */
    fun onPageSettled(currentIndex: Int, urisByIndex: (Int) -> String?) {
        val keepIndices = (currentIndex - poolRadius..currentIndex + poolRadius).toSet()

        // Release anything outside the window first — frees memory before
        // we potentially allocate new players for the new window.
        val toRelease = players.keys.filterNot { it in keepIndices }
        toRelease.forEach { index ->
            players.remove(index)?.release()
        }

        keepIndices.forEach { index ->
            val uri = urisByIndex(index) ?: return@forEach
            val player = players.getOrPut(index) { ExoPlayer.Builder(appContext).build() }
            val currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
            if (currentUri != uri) {
                player.setMediaItem(MediaItem.fromUri(uri))
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.prepare()
            }
            // Only the settled page is allowed to advance. Neighbors stay
            // prepared but paused, avoiding three concurrent decode loops.
            player.playWhenReady = index == currentIndex
        }
    }

    fun playerFor(index: Int): ExoPlayer? = players[index]

    /**
     * Pauses the pool for an Activity lifecycle transition while remembering
     * which pages were actually playing. This lets the active Short resume
     * after a transient background/foreground transition without overriding
     * a user's intentional pause.
     */
    fun pauseAll() {
        lifecyclePlayingIndices.clear()
        players.forEach { (index, player) ->
            if (player.playWhenReady) lifecyclePlayingIndices += index
            player.playWhenReady = false
        }
    }

    /** Resumes only players that were playing when the lifecycle pause occurred. */
    fun resumeAfterLifecycle() {
        lifecyclePlayingIndices.forEach { index ->
            players[index]?.playWhenReady = true
        }
        lifecyclePlayingIndices.clear()
    }

    fun releaseAll() {
        lifecyclePlayingIndices.clear()
        players.values.forEach { it.release() }
        players.clear()
    }
}
