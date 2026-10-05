package com.rvh.video.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.player.PlaybackClient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaybackHistoryViewModel(
    private val repository: VideoRepository,
    private val playbackClient: PlaybackClient,
) : ViewModel() {
    val history = repository.observePlaybackHistory(16)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            var lastUri: String? = null
            playbackClient.state.collectLatest { state ->
                val uri = state.uri ?: return@collectLatest
                if (state.isPlaying) {
                    if (uri != lastUri) {
                        repository.recordPlayStart(uri)
                        lastUri = uri
                    }
                    repository.updatePlaybackHistory(
                        uri = uri,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        completed = false,
                    )
                } else if (state.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    repository.updatePlaybackHistory(
                        uri = uri,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        completed = true,
                    )
                    lastUri = uri
                }
            }
        }
    }

    fun clearHistory() = viewModelScope.launch { repository.clearPlaybackHistory() }
}
