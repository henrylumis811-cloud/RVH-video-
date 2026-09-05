package com.rvh.video

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.rvh.video.data.local.RvhDatabase
import com.rvh.video.data.local.VideoRepository
import com.rvh.video.ui.home.HomeViewModel
import com.rvh.video.ui.home.PlaybackHistoryViewModel
import com.rvh.video.ui.collections.CollectionsViewModel
import com.rvh.video.ui.details.MediaDetailsViewModel
import com.rvh.video.ui.movies.MoviesViewModel
import com.rvh.video.ui.music.MusicViewModel
import com.rvh.video.ui.shorts.ShortsViewModel
import com.rvh.video.ui.search.SearchViewModel

/**
 * All three section ViewModels need a VideoRepository (and Music also
 * needs the Application for its PlayerManager) — one shared factory
 * avoids repeating the "how do I build a repository" wiring three times.
 */
class RvhViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    private val repository: VideoRepository by lazy {
        VideoRepository(RvhDatabase.get(application).localVideoDao(), RvhDatabase.get(application).collectionDao(), RvhDatabase.get(application).playbackHistoryDao())
    }

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(repository) as T
            PlaybackHistoryViewModel::class.java -> PlaybackHistoryViewModel(repository, (application as RvhVideoApp).playerManager) as T
            CollectionsViewModel::class.java -> CollectionsViewModel(repository) as T
            MediaDetailsViewModel::class.java -> MediaDetailsViewModel(repository) as T
            MoviesViewModel::class.java -> MoviesViewModel(application, repository) as T
            ShortsViewModel::class.java -> ShortsViewModel(repository) as T
            SearchViewModel::class.java -> SearchViewModel(repository) as T
            MusicViewModel::class.java -> MusicViewModel(application, repository, (application as RvhVideoApp).playerManager) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
