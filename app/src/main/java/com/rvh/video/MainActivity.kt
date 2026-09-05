package com.rvh.video

import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.glassSurface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rvh.video.data.local.AppSettings
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.player.PipController
import androidx.media3.session.MediaSession
import com.rvh.video.ui.components.MediaPermissionGate
import com.rvh.video.ui.components.GlobalMiniPlayer
import com.rvh.video.ui.collections.CollectionPickerDialog
import com.rvh.video.ui.collections.CollectionsScreen
import com.rvh.video.ui.collections.CollectionsViewModel
import com.rvh.video.ui.details.MediaDetailsScreen
import com.rvh.video.ui.details.MediaDetailsViewModel
import com.rvh.video.ui.home.HomeScreen
import com.rvh.video.ui.home.FolderScreen
import com.rvh.video.ui.home.HomeViewModel
import com.rvh.video.ui.home.PlaybackHistoryViewModel
import com.rvh.video.ui.movies.MoviePlayerScreen
import com.rvh.video.ui.movies.MoviesScreen
import com.rvh.video.ui.movies.MoviesViewModel
import com.rvh.video.ui.music.FloatingMiniVideoPlayer
import com.rvh.video.ui.music.MusicScreen
import com.rvh.video.ui.music.MusicVideoPlayerScreen
import com.rvh.video.ui.music.MusicViewModel
import com.rvh.video.ui.profile.ProfileScreen
import com.rvh.video.ui.shorts.ShortsScreen
import com.rvh.video.ui.shorts.ShortsViewModel
import com.rvh.video.ui.search.SearchScreen
import com.rvh.video.ui.search.SearchViewModel
import com.rvh.video.ui.theme.RvhVideoTheme
import com.rvh.video.ui.theme.Surface0
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {

    private var mediaSession: MediaSession? = null

    // Read by the Compose tree so full-screen player content (Movies,
    // Music) can hide its own chrome while the system has us in PiP —
    // isInPictureInPictureMode isn't observable from Compose on its own.
    private var isInPip by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()

        // Give Android PiP a real MediaSession so its native play/pause action
        // controls the same ExoPlayer. This is essential when the user enters
        // PiP while the video is paused: the floating window must still offer
        // a way to resume playback.
        val sessionPlayer = (application as RvhVideoApp).playerManager.getOrCreate()
        mediaSession = MediaSession.Builder(this, sessionPlayer).build()

        setContent {
            RvhVideoTheme(themeMode = AppSettings(this@MainActivity).themeMode) {
                var showDedication by remember { mutableStateOf(true) }
                if (showDedication) {
                    com.rvh.video.ui.splash.DedicationScreen(onFinished = { showDedication = false })
                } else {
                    MediaPermissionGate {
                        RvhApp(isInPip = isInPip, onThemeChanged = { recreate() })
                    }
                }
            }
        }
    }

    /**
     * Android doesn't always default a window to the display's maximum
     * refresh rate — some devices/OEM skins start at 60Hz and only switch
     * up when an app explicitly requests it. minSdk here is 30, and
     * Activity.getDisplay() (used below) was added exactly at API 30, so
     * no version branch is needed. This alone won't fix jank caused by
     * heavy per-frame work (that's the thumbnail-decoding fix above), but
     * it removes the separate possibility that the display itself was
     * capped below what the hardware supports.
     */
    private fun requestHighestRefreshRate() {
        val bestMode = display?.supportedModes?.maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = bestMode.modeId
        }
    }

    /**
     * Android 11 (API 30) has no auto-enter-PiP mechanism at all — the
     * only way to get PiP there is an explicit enterPictureInPictureMode()
     * call at the exact moment the user leaves (Home button, recents,
     * etc). API 31+ doesn't need this: PipController.configureAutoEnter
     * (called from MusicVideoPlayerScreen) already told the system to
     * handle it automatically.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val autoFloatingPlayEnabled = com.rvh.video.data.local.AppSettings(this).autoFloatingPlayEnabled
        if (PipController.isPreAutoEnterOs() && musicPlayerIsForeground && autoFloatingPlayEnabled) {
            // Guarded by musicPlayerIsForeground so leaving from Shorts/
            // Movies never triggers PiP with the wrong (or no) content,
            // and by the setting so this respects the Profile toggle same
            // as the API 31+ path in MusicVideoPlayerScreen.
            PipController.enterManually(this)
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPip = isInPictureInPictureMode
    }

    companion object {
        /** Set by RvhApp; read from onUserLeaveHint. A plain top-level var is enough for a single-Activity app. */
        var musicPlayerIsForeground: Boolean = false
    }
}

private enum class Route { Home, Search, Shorts, Movies, MoviePlayer, Music, MusicPlayer, Collections, MediaDetails, Profile, Folder }

@Composable
private fun RvhApp(isInPip: Boolean, onThemeChanged: () -> Unit) {
    var route by remember { mutableStateOf(Route.Home) }
    val routeStack = remember { mutableStateOf<List<Route>>(emptyList()) }
    fun navigateTo(target: Route) {
        if (target == route) return
        routeStack.value = routeStack.value + route
        route = target
    }
    fun goBack() {
        val stack = routeStack.value
        if (stack.isNotEmpty()) {
            route = stack.last()
            routeStack.value = stack.dropLast(1)
        } else if (route != Route.Home) {
            route = Route.Home
        }
    }
    BackHandler(enabled = route != Route.Home) { goBack() }
    var openMovieUri by remember { mutableStateOf<String?>(null) }
    var openShortUri by remember { mutableStateOf<String?>(null) }
    var openFolderName by remember { mutableStateOf<String?>(null) }
    var showFloatingMiniPlayer by remember { mutableStateOf(false) }
    var collectionTarget by remember { mutableStateOf<LocalVideoEntity?>(null) }
    var detailsUri by remember { mutableStateOf<String?>(null) }
    var raceControlTitle by remember { mutableStateOf<String?>(null) }
    var showRaceControl by remember { mutableStateOf(false) }

    LaunchedEffect(showRaceControl) {
        if (showRaceControl) {
            kotlinx.coroutines.delay(1100)
            showRaceControl = false
        }
    }

    val context = LocalContext.current
    val application = context.applicationContext as Application
    val settings = remember { AppSettings(application) }
    val playerManager = (application as RvhVideoApp).playerManager
    val playbackState by playerManager.state.collectAsState()

    // Hoisted above the per-screen composables so state survives
    // navigating to/from each section's full-screen player: two
    // independently-scoped instances would each build their own
    // PlayerManager/DB observers and lose sync (e.g. resume position
    // never actually persisting, queue position resetting).
    val homeViewModel: HomeViewModel = viewModel(factory = RvhViewModelFactory(application))
    // Starts the app-scoped playback-history observer; its state is surfaced through HomeViewModel.
    viewModel<PlaybackHistoryViewModel>(factory = RvhViewModelFactory(application))
    val moviesViewModel: MoviesViewModel = viewModel(factory = RvhViewModelFactory(application))
    val musicViewModel: MusicViewModel = viewModel(factory = RvhViewModelFactory(application))
    val collectionsViewModel: CollectionsViewModel = viewModel(factory = RvhViewModelFactory(application))
    val detailsViewModel: MediaDetailsViewModel = viewModel(factory = RvhViewModelFactory(application))
    val searchViewModel: SearchViewModel = viewModel(factory = RvhViewModelFactory(application))
    val shortsViewModel: ShortsViewModel = viewModel(factory = RvhViewModelFactory(application))
    LaunchedEffect(detailsUri) { detailsViewModel.load(detailsUri) }
    val detailsVideo by detailsViewModel.video.collectAsState()
    val movies by moviesViewModel.movies.collectAsState()

    val musicQueue by musicViewModel.queue.collectAsState()
    val musicCurrentIndex by musicViewModel.currentIndex.collectAsState()
    val musicIsPlaying by musicViewModel.isPlaying.collectAsState()
    val currentTrack = musicCurrentIndex?.let { musicQueue.getOrNull(it) }

    // Real OS PiP (for when the user actually leaves the app) should stay
    // available whether the full player or the in-app floating mini
    // player is what's currently showing the video — both count as
    // "there's a live video surface that could sensibly become a PiP
    // window if you background the app."
    MainActivity.musicPlayerIsForeground =
        (route == Route.MusicPlayer) || (showFloatingMiniPlayer && currentTrack != null)

    // Closing the floating mini player, or opening the full player again,
    // both need to turn it off — simplest to just always clear it whenever
    // the full player becomes the active route, rather than remembering
    // to reset the flag at every call site that could lead there.
    LaunchedEffect(route) {
        if (route == Route.MusicPlayer) showFloatingMiniPlayer = false
    }

    // "Auto floating play" governs whether Music Videos is allowed to keep
    // running once you leave its screens at all — off means it stops the
    // moment you navigate to a different tab (matching the toggle's
    // meaning: no floating/background playback), on means it keeps going
    // with the global mini-bar below picking up wherever you land. This is
    // also what fixes the earlier bug where a track kept playing silently
    // with literally no visible control once you switched to Shorts.
    LaunchedEffect(route) {
        val leavingMusicEntirely = route != Route.Music && route != Route.MusicPlayer
        if (leavingMusicEntirely && !settings.autoFloatingPlayEnabled) {
            musicViewModel.playerManager.pause()
            showFloatingMiniPlayer = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = androidx.compose.ui.res.painterResource(com.rvh.video.R.drawable.rvh_hero_car),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.86f
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.56f),
                    0.48f to Color.Black.copy(alpha = 0.30f),
                    1f to Color.Black.copy(alpha = 0.58f)
                )
            )
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (route) {
                    Route.Home -> HomeScreen(
                        viewModel = homeViewModel,
                        onOpenMovie = { uri -> openMovieUri = uri; navigateTo(Route.MoviePlayer) },
                        onOpenMedia = { uri, category ->
                            when (category) {
                                com.rvh.video.data.model.VideoCategory.MUSIC_VIDEO -> navigateTo(Route.Music)
                                com.rvh.video.data.model.VideoCategory.MOVIE -> { openMovieUri = uri; navigateTo(Route.MoviePlayer) }
                                com.rvh.video.data.model.VideoCategory.SHORT -> { openShortUri = uri; navigateTo(Route.Shorts) }
                            }
                        },
                        onOpenMovies = { navigateTo(Route.Movies) },
                        onOpenMusic = { navigateTo(Route.Music) },
                        onOpenShorts = { openShortUri = null; navigateTo(Route.Shorts) },
                        onOpenCollections = { navigateTo(Route.Collections) },
                        onOpenSearch = { navigateTo(Route.Search) },
                        onOpenSettings = { navigateTo(Route.Profile) },
                        onOpenFolder = { folder -> openFolderName = folder; navigateTo(Route.Folder) },
                        onToggleFavorite = { video -> homeViewModel.toggleFavorite(video.uri, !video.isFavorite) },
                        onToggleWatchLater = { video -> homeViewModel.toggleWatchLater(video.uri, !video.isWatchLater) },
                    )

                    Route.Search -> SearchScreen(
                        viewModel = searchViewModel,
                        onBack = { goBack() },
                        onOpenMedia = { video ->
                            when (video.effectiveCategory) {
                                com.rvh.video.data.model.VideoCategory.MOVIE -> { openMovieUri = video.uri; navigateTo(Route.MoviePlayer) }
                                com.rvh.video.data.model.VideoCategory.MUSIC_VIDEO -> {
                                    val index = musicQueue.indexOfFirst { it.uri == video.uri }
                                    if (index >= 0) { musicViewModel.playAt(index); navigateTo(Route.MusicPlayer) } else navigateTo(Route.Music)
                                }
                                com.rvh.video.data.model.VideoCategory.SHORT -> { openShortUri = video.uri; navigateTo(Route.Shorts) }
                            }
                        },
                        onToggleFavorite = { video -> searchViewModel.toggleFavorite(video.uri, !video.isFavorite) },
                    )

                    Route.Shorts -> ShortsScreen(
                        initialVideoUri = openShortUri,
                        viewModel = shortsViewModel,
                        onAddToCollection = { collectionTarget = it },
                        onToggleWatchLater = { uri, value -> shortsViewModel.toggleWatchLater(uri, value) },
                        onOpenDetails = { detailsUri = it; navigateTo(Route.MediaDetails) }
                    )

                    Route.Movies -> MoviesScreen(
                        viewModel = moviesViewModel,
                        onOpenMovie = { uri, _ ->
                            openMovieUri = uri
                            navigateTo(Route.MoviePlayer)
                        },
                        onToggleFavorite = { uri, favorite -> moviesViewModel.toggleFavorite(uri, favorite) },
                        onAddToCollection = { collectionTarget = it },
                        onToggleWatchLater = { uri, value -> moviesViewModel.toggleWatchLater(uri, value) },
                        onOpenDetails = { detailsUri = it; navigateTo(Route.MediaDetails) }
                    )

                    Route.MoviePlayer -> openMovieUri?.let { uri ->
                        val movie = movies.find { it.uri == uri }
                        MoviePlayerScreen(
                            uri = uri,
                            resumePositionMs = movie?.resumePositionMs ?: 0L,
                            onSavePosition = { positionMs -> moviesViewModel.saveResumePosition(uri, positionMs) },
                            onBack = {
                                raceControlTitle = movie?.displayName?.substringBeforeLast('.') ?: "RVH Media"
                                showRaceControl = true
                                goBack()
                            },
                            mediaTitle = movie?.displayName?.substringBeforeLast('.') ?: "RVH Media",
                            isInPip = isInPip,
                        )
                    }

                    Route.Music -> MusicScreen(
                        viewModel = musicViewModel,
                        onOpenFullPlayer = { navigateTo(Route.MusicPlayer) },
                        onAddToCollection = { collectionTarget = it },
                        onToggleWatchLater = { uri, value -> musicViewModel.toggleWatchLater(uri, value) },
                        onOpenDetails = { detailsUri = it; navigateTo(Route.MediaDetails) },
                    )

                    Route.MediaDetails -> MediaDetailsScreen(
                        video = detailsVideo,
                        onBack = { goBack() },
                        onPlay = {
                            val video = detailsVideo
                            if (video != null) when (video.effectiveCategory) {
                                com.rvh.video.data.model.VideoCategory.MOVIE -> { openMovieUri = video.uri; navigateTo(Route.MoviePlayer) }
                                com.rvh.video.data.model.VideoCategory.MUSIC_VIDEO -> {
                                    val index = musicQueue.indexOfFirst { it.uri == video.uri }
                                    if (index >= 0) { musicViewModel.playAt(index); navigateTo(Route.MusicPlayer) } else navigateTo(Route.Music)
                                }
                                com.rvh.video.data.model.VideoCategory.SHORT -> { openShortUri = video.uri; navigateTo(Route.Shorts) }
                            }
                        },
                        onToggleFavorite = { detailsViewModel.toggleFavorite() },
                        onToggleWatchLater = { detailsViewModel.toggleWatchLater() },
                    )

                    Route.Collections -> CollectionsScreen(
                        viewModel = collectionsViewModel,
                        onOpenMedia = { video ->
                            when (video.effectiveCategory) {
                                com.rvh.video.data.model.VideoCategory.MOVIE -> { openMovieUri = video.uri; navigateTo(Route.MoviePlayer) }
                                com.rvh.video.data.model.VideoCategory.MUSIC_VIDEO -> {
                                    val index = musicQueue.indexOfFirst { it.uri == video.uri }
                                    if (index >= 0) { musicViewModel.playAt(index); navigateTo(Route.MusicPlayer) } else navigateTo(Route.Music)
                                }
                                com.rvh.video.data.model.VideoCategory.SHORT -> { openShortUri = video.uri; navigateTo(Route.Shorts) }
                            }
                        }
                    )

                    Route.MusicPlayer -> MusicVideoPlayerScreen(
                        viewModel = musicViewModel,
                        isInPip = isInPip,
                        onMinimize = { goBack() },
                        onRequestFloatingPlayer = {
                            showFloatingMiniPlayer = true
                            goBack()
                        },
                    )

                    Route.Profile -> ProfileScreen(onThemeChanged = onThemeChanged)

                    Route.Folder -> openFolderName?.let { folder ->
                        FolderScreen(
                            folder = folder,
                            viewModel = homeViewModel,
                            onBack = { goBack() },
                            onOpenMedia = { video ->
                                when (video.effectiveCategory) {
                                    com.rvh.video.data.model.VideoCategory.MOVIE -> { openMovieUri = video.uri; navigateTo(Route.MoviePlayer) }
                                    com.rvh.video.data.model.VideoCategory.MUSIC_VIDEO -> {
                                        val index = musicQueue.indexOfFirst { it.uri == video.uri }
                                        if (index >= 0) { musicViewModel.playAt(index); navigateTo(Route.MusicPlayer) } else navigateTo(Route.Music)
                                    }
                                    com.rvh.video.data.model.VideoCategory.SHORT -> { openShortUri = video.uri; navigateTo(Route.Shorts) }
                                }
                            }
                        )
                    }
                }
            }

            // One global media surface follows the application-scoped player.
            // It survives navigation and works for both Music and Movies.
            val activeUri = playbackState.uri
            val activeMusic = activeUri?.let { uri -> musicQueue.find { it.uri == uri } }
            val activeMovie = activeUri?.let { uri -> movies.find { it.uri == uri } }
            val activeMedia = activeMusic ?: activeMovie
            val isMusic = activeMusic != null

            if (activeMedia != null && !isInPip &&
                route != Route.MusicPlayer && route != Route.MoviePlayer
            ) {
                GlobalMiniPlayer(
                    media = activeMedia,
                    playerManager = playerManager,
                    isMusic = isMusic,
                    onPlayPause = {
                        if (playbackState.isPlaying) playerManager.pause() else playerManager.resume()
                    },
                    onNext = { musicViewModel.playNext() },
                    onPrevious = { musicViewModel.playPrevious() },
                    onExpand = {
                        route = if (isMusic) Route.MusicPlayer else {
                            openMovieUri = activeMedia.uri
                            Route.MoviePlayer
                        }
                    },
                    onDismiss = {
                        playerManager.stopAndClear()
                        if (isMusic) musicViewModel.stopAndClearQueue()
                    },
                    modifier = Modifier.padding(12.dp)
                )
            }

        }

        // Overlays the ENTIRE screen (siblings to the Column above, not
        // confined to a slot inside it) so it can actually float over
        // whatever's underneath and be dragged anywhere, rather than
        // being pinned to one spot in the layout flow.
        if (showFloatingMiniPlayer && currentTrack != null && !isInPip) {
            FloatingMiniVideoPlayer(
                playerManager = musicViewModel.playerManager,
                onExpand = { navigateTo(Route.MusicPlayer) },
                onClose = {
                    showFloatingMiniPlayer = false
                    musicViewModel.stopAndClearQueue()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 16.dp) // clears the bottom nav by default
            )
        }

        if (showRaceControl) {
            RaceControlToast(
                title = raceControlTitle ?: "RVH Media",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 54.dp, end = 14.dp)
            )
        }

        collectionTarget?.let { video ->
            CollectionPickerDialog(
                video = video,
                viewModel = collectionsViewModel,
                onDismiss = { collectionTarget = null }
            )
        }
    }
}


@Composable
private fun RaceControlToast(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .glassSurface(shape = RoundedCornerShape(16.dp), blurRadius = 18.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(1.dp)) {
            Text("RACE CONTROL", style = RvhType.Meta, color = Color.White)
            Text("SESSION SAVED", style = RvhType.Meta, color = TextSecondary)
            Text(title, style = RvhType.Body, color = Color.White, maxLines = 1)
        }
    }
}
