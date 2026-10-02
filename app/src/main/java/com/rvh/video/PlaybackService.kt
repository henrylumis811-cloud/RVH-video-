package com.rvh.video

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import kotlin.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rvh.video.player.PlayerManager

/**
 * System/background playback boundary for RVH Video.
 *
 * The Activity is a UI client. Media3 owns the session lifecycle here so
 * playback controls remain available when the Activity is recreated or leaves
 * the foreground.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var playerManager: PlayerManager

    override fun onCreate() {
        super.onCreate()
        playerManager = PlayerManager(this)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_PLAYBACK
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, playerManager.getOrCreate())
            .setSessionActivity(sessionActivity)
            .setCallback(sessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    @OptIn(UnstableApi::class)
    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            // RVH's app controller and trusted system media controllers get the
            // normal playback command surface. Unknown third-party controllers
            // are read-only instead of receiving full control over local media.
            // The notification controller is explicitly retained because Media3
            // requires it for the system media notification to function.
            val builder = MediaSession.ConnectionResult.AcceptedResultBuilder(
                session,
                controller,
            )
            return if (controller.isTrusted || session.isMediaNotificationController(controller)) {
                if (controller.isTrusted) {
                    builder.setAvailableSessionCommands(
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                            .buildUpon()
                            .add(CLEAR_PLAYBACK_COMMAND)
                            .add(SET_PLAYBACK_QUEUE_COMMAND)
                            .add(SET_PLAYBACK_QUEUE_MODE_COMMAND)
                            .add(NEXT_QUEUE_ITEM_COMMAND)
                            .add(PREVIOUS_QUEUE_ITEM_COMMAND)
                            .build()
                    )
                } else {
                    builder.setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                }
                builder.build()
            } else {
                MediaSession.ConnectionResult.accept(
                    MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_SESSION_COMMANDS,
                    MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_PLAYER_COMMANDS,
                )
            }
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): com.google.common.util.concurrent.ListenableFuture<SessionResult> {
            if (customCommand.customAction == SET_PLAYBACK_QUEUE_COMMAND.customAction) {
                if (!controller.isTrusted) {
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
                }
                val uris = args.getStringArrayList(EXTRA_QUEUE_URIS).orEmpty()
                val startIndex = args.getInt(EXTRA_QUEUE_START_INDEX, 0)
                val startPositionMs = args.getLong(EXTRA_QUEUE_START_POSITION_MS, 0L)
                val shuffle = args.getBoolean(EXTRA_QUEUE_SHUFFLE, false)
                val repeatOne = args.getBoolean(EXTRA_QUEUE_REPEAT_ONE, false)
                val autoAdvance = args.getBoolean(EXTRA_QUEUE_AUTO_ADVANCE, true)
                playerManager.setQueue(uris, startIndex, startPositionMs, shuffle, repeatOne, autoAdvance)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == NEXT_QUEUE_ITEM_COMMAND.customAction) {
                if (!controller.isTrusted) return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
                playerManager.nextQueueItem()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == PREVIOUS_QUEUE_ITEM_COMMAND.customAction) {
                if (!controller.isTrusted) return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
                playerManager.previousQueueItem()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == SET_PLAYBACK_QUEUE_MODE_COMMAND.customAction) {
                if (!controller.isTrusted) {
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
                }
                playerManager.setQueueMode(
                    shuffle = args.getBoolean(EXTRA_QUEUE_SHUFFLE, false),
                    repeatOne = args.getBoolean(EXTRA_QUEUE_REPEAT_ONE, false),
                    autoAdvance = args.getBoolean(EXTRA_QUEUE_AUTO_ADVANCE, true),
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == CLEAR_PLAYBACK_COMMAND.customAction) {
                if (!controller.isTrusted) {
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
                }
                playerManager.stopAndClear()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val snapshot = playerManager.savedPlaybackSnapshot()
            val uri = snapshot.uri ?: return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    emptyList(),
                    C.INDEX_UNSET,
                    C.TIME_UNSET,
                )
            )

            val item = MediaItem.Builder()
                .setMediaId(uri)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(snapshot.title?.takeIf { it.isNotBlank() } ?: displayTitle(uri))
                        .build()
                )
                .build()

            // Restore the same playback speed used by the in-app resume flow
            // before Media3 prepares/resumes the restored item. This keeps
            // Android-driven resumption behavior consistent with RVH itself.
            mediaSession.player.setPlaybackParameters(
                androidx.media3.common.PlaybackParameters(snapshot.playbackSpeed)
            )

            // The same persisted position used by the in-app resume flow is
            // now also available to Android's external/media-button resumption.
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    listOf(item),
                    0,
                    if (isForPlayback) snapshot.positionMs else C.TIME_UNSET,
                )
            )
        }
    }

    private fun displayTitle(uri: String): String {
        val raw = android.net.Uri.parse(uri).lastPathSegment ?: uri
        return raw.substringAfterLast('/').substringBeforeLast('.').ifBlank { "RVH Video" }
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Let Media3 decide whether playback is genuinely ongoing. This is
        // more authoritative than the app-side wrapper because the service
        // owns the actual session/player lifecycle.
        if (!isPlaybackOngoing) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        val CLEAR_PLAYBACK_COMMAND = SessionCommand(
            "com.rvh.video.session.CLEAR_PLAYBACK",
            Bundle.EMPTY,
        )
        val SET_PLAYBACK_QUEUE_COMMAND = SessionCommand(
            "com.rvh.video.session.SET_PLAYBACK_QUEUE",
            Bundle.EMPTY,
        )
        val SET_PLAYBACK_QUEUE_MODE_COMMAND = SessionCommand(
            "com.rvh.video.session.SET_PLAYBACK_QUEUE_MODE",
            Bundle.EMPTY,
        )
        val NEXT_QUEUE_ITEM_COMMAND = SessionCommand(
            "com.rvh.video.session.NEXT_QUEUE_ITEM",
            Bundle.EMPTY,
        )
        val PREVIOUS_QUEUE_ITEM_COMMAND = SessionCommand(
            "com.rvh.video.session.PREVIOUS_QUEUE_ITEM",
            Bundle.EMPTY,
        )
        const val EXTRA_QUEUE_URIS = "queue_uris"
        const val EXTRA_QUEUE_START_INDEX = "queue_start_index"
        const val EXTRA_QUEUE_START_POSITION_MS = "queue_start_position_ms"
        const val EXTRA_QUEUE_SHUFFLE = "queue_shuffle"
        const val EXTRA_QUEUE_REPEAT_ONE = "queue_repeat_one"
        const val EXTRA_QUEUE_AUTO_ADVANCE = "queue_auto_advance"
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        if (::playerManager.isInitialized) playerManager.release()
        super.onDestroy()
    }
}
