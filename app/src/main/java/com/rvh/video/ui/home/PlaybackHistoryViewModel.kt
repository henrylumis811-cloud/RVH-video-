package com.rvh.video.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.player.PlayerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackHistoryViewModel(
    private val repository: VideoRepository,
    private val playerManager: PlayerManager,
) : ViewModel() {
    val history = repository.observePlaybackHistory(16)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            var lastUri: String? = null
            while (isActive) {
                val state = playerManager.state.value
                val uri = state.uri
                if (uri != null && state.isPlaying) {
                    if (uri != lastUri) {
                        repository.recordPlayStart(uri)
                        lastUri = uri
                    }
                    repository.updatePlaybackHistory(
                        uri = uri,
                        positionMs = playerManager.currentPositionMs(),
                        durationMs = state.durationMs,
                        completed = false,
                    )
                } else if (uri != null && state.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    repository.updatePlaybackHistory(
                        uri = uri,
                        positionMs = playerManager.currentPositionMs(),
                        durationMs = state.durationMs,
                        completed = true,
                    )
                    lastUri = uri
                }
                delay(2_000)
            }
        }
    }

    fun clearHistory() = viewModelScope.launch { repository.clearPlaybackHistory() }
}
