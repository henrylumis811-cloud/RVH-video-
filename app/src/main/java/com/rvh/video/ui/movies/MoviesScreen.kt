package com.rvh.video.ui.movies
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.RvhViewModelFactory
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
import com.rvh.video.ui.theme.TextSecondary
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.glassSurface

@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel = viewModel(
        factory = RvhViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    ),
    onOpenMovie: (uri: String, resumeMs: Long) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onAddToCollection: (LocalVideoEntity) -> Unit = {},
    onToggleWatchLater: (String, Boolean) -> Unit = { _, _ -> },
    onOpenDetails: (String) -> Unit = {},
) {
    val movies by viewModel.movies.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    var sortExpanded by remember { mutableStateOf(false) }
    var gridExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var performanceMode by remember { mutableStateOf(false) }
    var focusMode by remember { mutableStateOf(false) }
    var personalDriveMode by remember { mutableStateOf("SMART") }
    var showGarageIntelligence by remember { mutableStateOf(false) }

    val filtered = remember(movies, query, sortMode, filter) {
        val searched = if (query.isBlank()) movies else movies.filter { it.displayName.contains(query, ignoreCase = true) }
        val narrowed = when (filter) {
            "resume" -> searched.filter { it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }
            "favorites" -> searched.filter { it.isFavorite }
            "later" -> searched.filter { it.isWatchLater }
            else -> searched
        }
        when (sortMode) {
            "oldest" -> narrowed.sortedBy { it.dateModifiedEpochSeconds }
            "name" -> narrowed.sortedBy { it.displayName.lowercase() }
            "longest" -> narrowed.sortedByDescending { it.durationMs }
            else -> narrowed.sortedByDescending { it.dateModifiedEpochSeconds }
        }
    }

    // Deterministic strategy target: resume an active session first, then
    // Watch Later, Favorites, and finally the newest media. Keep these values
    // above every UI block that consumes them so Compose/Kotlin sees them in scope.
    val inProgress = movies.filter { it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }
    val strategyMovie = inProgress.maxByOrNull {
        it.resumePositionMs.toDouble() / it.durationMs.coerceAtLeast(1L)
    } ?: movies.firstOrNull { it.isWatchLater }
        ?: movies.firstOrNull { it.isFavorite }
        ?: movies.maxByOrNull { it.dateModifiedEpochSeconds }
    val strategyLabel = when {
        strategyMovie == null -> "GRID EMPTY"
        strategyMovie in inProgress -> "RESUME NEXT"
        strategyMovie.isWatchLater -> "PIT PLAN"
        strategyMovie.isFavorite -> "QUALIFYING PICK"
        else -> "GRID PICK"
    }
    val strategyProgress = if (strategyMovie != null && strategyMovie.durationMs > 0L) {
        (strategyMovie.resumePositionMs.coerceIn(0L, strategyMovie.durationMs) * 100L / strategyMovie.durationMs).toInt()
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding() // enableEdgeToEdge() draws content behind the status bar by design — without this, the title/search sat under the notification icons
            .padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 14.dp)) {
                Text("GARAGE", style = RvhType.ScreenTitle)
                Text("MOVIE BAY  •  ${movies.size} VEHICLES", style = RvhType.Meta, color = TextSecondary)
            }
            Box {
                IconButton(onClick = { sortExpanded = true }) { Icon(Icons.Filled.Sort, "Sort") }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    listOf("newest" to "Newest", "oldest" to "Oldest", "name" to "Name", "longest" to "Longest").forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setSortMode(value); sortExpanded = false })
                    }
                }
            }
            TextButton(onClick = { focusMode = !focusMode }) {
                Text(if (focusMode) "EXIT FOCUS" else "FOCUS", style = RvhType.Meta, color = Color.White)
            }
            Box {
                IconButton(onClick = { gridExpanded = true }) { Icon(Icons.Filled.ViewModule, "Grid size") }
                DropdownMenu(expanded = gridExpanded, onDismissRequest = { gridExpanded = false }) {
                    (2..4).forEach { columns ->
                        DropdownMenuItem(text = { Text("$columns columns") }, onClick = { viewModel.setGridColumns(columns); gridExpanded = false })
                    }
                }
            }
        }

        // Legendary Step 48: Media Focus Mode creates a distraction-free
        // command state around the same deterministic strategy target.
        if (focusMode && strategyMovie != null) {
            MediaFocusMode(
                movie = strategyMovie,
                actionLabel = towerNextForCommand(strategyLabel, strategyProgress),
                onPlay = { onOpenMovie(strategyMovie.uri, strategyMovie.resumePositionMs) },
                onFavorite = { onToggleFavorite(strategyMovie.uri, !strategyMovie.isFavorite) },
                onWatchLater = { onToggleWatchLater(strategyMovie.uri, !strategyMovie.isWatchLater) },
                onDetails = { onOpenDetails(strategyMovie.uri) },
                onExit = { focusMode = false },
            )
        }

        // Legendary Step 24: the library becomes the RVH Garage.
        // These filters are local, instant, and derived entirely from existing
        // video state — no new persistence or background work is required.
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item { FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("All") }) }
            item { FilterChip(selected = filter == "resume", onClick = { filter = "resume" }, label = { Text("In progress") }, leadingIcon = { Icon(Icons.Filled.PlayCircle, null) }) }
            item { FilterChip(selected = filter == "favorites", onClick = { filter = "favorites" }, label = { Text("Favorites") }, leadingIcon = { Icon(Icons.Filled.Favorite, null) }) }
            item { FilterChip(selected = filter == "later", onClick = { filter = "later" }, label = { Text("Watch later") }, leadingIcon = { Icon(Icons.Filled.Schedule, null) }) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GarageStat("READY", movies.count { it.resumePositionMs <= 0L }, Modifier.weight(1f))
            GarageStat("RUNNING", movies.count { it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }, Modifier.weight(1f))
            GarageStat("FAV", movies.count { it.isFavorite }, Modifier.weight(1f))
        }

        // The library is the primary experience: keep the full movie grid visible
        // immediately after the compact controls instead of burying it under the
        // Garage command surfaces.
        // Advanced Garage intelligence is still available, but it no longer
        // competes with the library for screen space.
        TextButton(
            onClick = { showGarageIntelligence = !showGarageIntelligence },
            modifier = Modifier.fillMaxWidth().glassSurface(shape = RoundedCornerShape(14.dp)).padding(horizontal = 8.dp),
        ) {
            Text(
                if (showGarageIntelligence) "HIDE GARAGE INTELLIGENCE" else "GARAGE INTELLIGENCE  •  SHOW COMMAND CENTER",
                style = RvhType.Meta,
                color = Color.White,
            )
        }

        if (showGarageIntelligence) {
        // Legendary Step 31: live telemetry turns the Garage into a real
        // command surface. Everything below is derived from the already
        // observed local movie list — no new persistence or background work.
        val inProgress = movies.filter { it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }
        val fleetProgress = if (movies.isEmpty()) 0 else {
            val playable = movies.filter { it.durationMs > 0L }
            if (playable.isEmpty()) 0 else ((playable.sumOf { it.resumePositionMs.coerceIn(0L, it.durationMs) * 100L / it.durationMs }) / playable.size).toInt()
        }
        val leadVehicle = inProgress.maxByOrNull { it.resumePositionMs.toDouble() / it.durationMs.coerceAtLeast(1L) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(14.dp), blurRadius = 14.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("LIVE TELEMETRY", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
                Text("FLEET ${fleetProgress}%", style = RvhType.Meta, color = TextSecondary)
            }
            Text(
                if (leadVehicle != null) "ACTIVE  •  ${leadVehicle.displayName.substringBeforeLast('.')}" else "STANDBY  •  No active sessions",
                style = RvhType.Body,
                color = Color.White,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50.dp))
                    .padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fleetProgress.coerceIn(0, 100) / 100f)
                        .background(Color.White, RoundedCornerShape(50.dp))
                        .padding(vertical = 2.dp)
                )
            }
        }

        // Legendary Step 32: Performance Mode is a real rendering optimization.
        // It increases library density and removes thumbnail crossfades, reducing
        // transition work while keeping all media data and playback behavior intact.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(14.dp), blurRadius = 10.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PERFORMANCE MODE", style = RvhType.Meta, color = Color.White)
                Text(
                    if (performanceMode) "HIGH THROUGHPUT  •  4-CELL GRID  •  FAST THUMBNAILS" else "CINEMATIC  •  3-CELL GRID  •  SMOOTH TRANSITIONS",
                    style = RvhType.Meta,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Switch(checked = performanceMode, onCheckedChange = { performanceMode = it })
        }

        // Legendary Step 33: Pit Wall turns the Garage telemetry into a
        // compact decision surface. It is derived from the current library
        // only, so it stays instant and fully offline.
        PitWall(
            total = movies.size,
            ready = movies.count { it.resumePositionMs <= 0L },
            running = inProgress.size,
            favorites = movies.count { it.isFavorite },
            watchLater = movies.count { it.isWatchLater },
            visible = filtered.size,
            filter = filter,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(14.dp), blurRadius = 12.dp)
                .clickable(enabled = strategyMovie != null) {
                    strategyMovie?.let { onOpenMovie(it.uri, it.resumePositionMs) }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("RACE STRATEGY", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
                Text(strategyLabel, style = RvhType.Meta, color = TextSecondary)
            }
            if (strategyMovie != null) {
                Text(
                    strategyMovie.displayName.substringBeforeLast('.'),
                    style = RvhType.CardTitle,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    if (strategyLabel == "RESUME NEXT") "${strategyProgress}% COMPLETE  •  TAP TO RESUME"
                    else "TAP TO LAUNCH  •  LOCAL RECOMMENDATION",
                    style = RvhType.Meta,
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50.dp))
                        .padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(strategyProgress.coerceIn(0, 100) / 100f)
                            .background(Color.White, RoundedCornerShape(50.dp))
                            .padding(vertical = 2.dp)
                    )
                }
            } else {
                Text("NO MEDIA AVAILABLE  •  SCAN YOUR LIBRARY TO BUILD THE GRID", style = RvhType.Meta, color = TextSecondary)
            }
        }

        // Legendary Step 57: Race Strategy AI adds a local adaptive ranking layer.
        // It uses only existing playback intent, favorites, Watch Later, and freshness.
        RaceStrategyAI(
            movies = movies,
            onOpenMovie = onOpenMovie,
        )

        // Legendary Step 62: Mission Control unifies the Garage telemetry into one
        // compact operational surface before the Personal Drive recommendations.
        MissionControl(
            movies = movies,
            target = strategyMovie,
            progressPercent = strategyProgress,
            onOpenMovie = onOpenMovie,
        )

        // Legendary Step 64: Mission Sequence 2.0 continuously reorders its staged
        // runs from the latest playback and intent state — no static queue is kept.
        MissionSequence(
            movies = movies,
            onOpenMovie = onOpenMovie,
        )

        // Legendary Step 78: Garage System Sync makes the flagship command surfaces
        // transition as one coordinated cockpit when the live mission state changes.
        val garageSyncState = when {
            strategyMovie == null -> "STANDBY"
            strategyProgress >= 75 -> "FINAL_LAP"
            strategyProgress > 0 -> "IN_MOTION"
            strategyMovie.isWatchLater -> "QUEUED"
            strategyMovie.isFavorite -> "QUALIFY"
            else -> "READY"
        }
        val missionFocusActive = strategyMovie != null && strategyProgress > 0
        val missionFocusAlpha by animateFloatAsState(
            targetValue = if (missionFocusActive) 0.72f else 1f,
            animationSpec = tween(durationMillis = 520),
            label = "missionFocusSupportAlpha"
        )

        CinematicMissionFocus(
            target = strategyMovie,
            targetProgress = strategyProgress,
            missionState = garageSyncState,
            onOpenMovie = onOpenMovie,
        )

        Crossfade(
            targetState = garageSyncState,
            animationSpec = tween(durationMillis = 420),
            label = "garageSystemSync"
        ) { _ ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Legendary Step 73: Garage Command HUD keeps the highest-value mission
                // state visible in a compact strip before the deeper command surfaces.
                GarageCommandHud(
                    target = strategyMovie,
                    targetProgress = strategyProgress,
                    personalMode = personalDriveMode,
                    activeCount = inProgress.size,
                    queueCount = movies.count { it.isWatchLater },
                    onOpenMovie = onOpenMovie,
                )

                // Legendary Step 67: Command Deck unifies the live Garage signals into
                // one glanceable target, fleet load, and direct command surface.
                Box(Modifier.graphicsLayer { alpha = missionFocusAlpha; scaleX = 0.995f + missionFocusAlpha * 0.005f; scaleY = 0.995f + missionFocusAlpha * 0.005f }) {
                CommandDeck(
                    movies = movies,
                    target = strategyMovie,
                    targetProgress = strategyProgress,
                    personalMode = personalDriveMode,
                    onOpenMovie = onOpenMovie,
                )
                }

                // Legendary Step 69: Mission Timeline connects the command target,
                // staged sequence, and Personal Drive mode into one glanceable flow.
                Box(Modifier.graphicsLayer { alpha = missionFocusAlpha; scaleX = 0.995f + missionFocusAlpha * 0.005f; scaleY = 0.995f + missionFocusAlpha * 0.005f }) {
                MissionTimeline(
                    movies = movies,
                    target = strategyMovie,
                    targetProgress = strategyProgress,
                    personalMode = personalDriveMode,
                    onOpenMovie = onOpenMovie,
                )
                }

                // Legendary Step 70: Race Dashboard turns the timeline into a single
                // glanceable session overview without introducing new state or persistence.
                Box(Modifier.graphicsLayer { alpha = missionFocusAlpha; scaleX = 0.995f + missionFocusAlpha * 0.005f; scaleY = 0.995f + missionFocusAlpha * 0.005f }) {
                RaceDashboard(
                    movies = movies,
                    target = strategyMovie,
                    targetProgress = strategyProgress,
                    personalMode = personalDriveMode,
                    onOpenMovie = onOpenMovie,
                )
                }
            }
        }

        // Legendary Step 59: Personal Drive turns the local recommendation signals
        // into a polished For You surface. No profile, network, or new persistence
        // is required; the experience adapts directly from the current library state.
        PersonalDrive(
            movies = movies,
            mode = personalDriveMode,
            onModeChange = { personalDriveMode = it },
            onOpenMovie = onOpenMovie,
        )

        // Legendary Step 35: Race Engineer turns the live Garage state into
        // an actionable call. The advice is deterministic, lightweight, and
        // derived only from media already loaded in memory.
        val engineerMovie = strategyMovie
        val engineerProgress = strategyProgress
        val engineerCall = when {
            engineerMovie == null -> "BUILD THE GRID"
            strategyLabel == "RESUME NEXT" && engineerProgress >= 75 -> "PUSH TO FINISH"
            strategyLabel == "RESUME NEXT" && engineerProgress > 0 -> "RESUME SESSION"
            strategyLabel == "PIT PLAN" -> "BOX STRATEGY: WATCH LATER"
            strategyLabel == "QUALIFYING PICK" -> "QUALIFYING RUN"
            else -> "ROLL OUT"
        }
        val engineerSignal = when {
            engineerMovie == null -> "NO MEDIA  •  SCAN LIBRARY"
            strategyLabel == "RESUME NEXT" -> "ACTIVE SESSION  •  $engineerProgress% COMPLETE"
            strategyLabel == "PIT PLAN" -> "WATCH LATER QUEUE  •  READY"
            strategyLabel == "QUALIFYING PICK" -> "FAVORITE MEDIA  •  READY"
            else -> "FRESH MEDIA  •  READY FOR LAUNCH"
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(14.dp), blurRadius = 12.dp)
                .clickable(enabled = engineerMovie != null) {
                    engineerMovie?.let { onOpenMovie(it.uri, it.resumePositionMs) }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("RACE ENGINEER", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
                Text(engineerCall, style = RvhType.Meta, color = TextSecondary)
            }
            if (engineerMovie != null) {
                Text(
                    engineerMovie.displayName.substringBeforeLast('.'),
                    style = RvhType.CardTitle,
                    color = Color.White,
                    maxLines = 1
                )
            }
            Text(engineerSignal, style = RvhType.Meta, color = TextSecondary, maxLines = 1)
        }

        // Legendary Step 36: Race Director adds a final command layer over the
        // Garage. It reads the existing fleet state and issues a deterministic
        // session directive without adding persistence, networking, or services.
        val directorStatus = when {
            movies.isEmpty() -> "GRID CLOSED"
            inProgress.size >= 2 -> "MULTI-SESSION"
            inProgress.size == 1 -> "SESSION LIVE"
            movies.any { it.isWatchLater } -> "STANDBY QUEUE"
            movies.any { it.isFavorite } -> "QUALIFYING READY"
            else -> "GRID READY"
        }
        val directorDirective = when {
            movies.isEmpty() -> "SCAN LIBRARY TO OPEN THE GRID"
            inProgress.size >= 2 -> "MANAGE ACTIVE SESSIONS  •  ${inProgress.size} RUNNING"
            inProgress.size == 1 -> "PROTECT THE ACTIVE SESSION  •  RESUME WHEN READY"
            movies.any { it.isWatchLater } -> "CLEAR THE WATCH LATER QUEUE  •  NEXT RUN READY"
            movies.any { it.isFavorite } -> "SELECT A FAVORITE  •  QUALIFYING RUN READY"
            else -> "SELECT A MEDIA ITEM  •  GRID READY FOR LAUNCH"
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(14.dp), blurRadius = 12.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("RACE DIRECTOR", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
                Text(directorStatus, style = RvhType.Meta, color = TextSecondary)
            }
            Text(
                directorDirective,
                style = RvhType.Body,
                color = Color.White,
                maxLines = 2
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("FLEET ${movies.size}", style = RvhType.Meta, color = TextSecondary, modifier = Modifier.weight(1f))
                Text("RUN ${inProgress.size}", style = RvhType.Meta, color = TextSecondary)
                Text("FILTER ${filter.uppercase()}", style = RvhType.Meta, color = TextSecondary)
            }
        }

        // Legendary Step 37: Race Strategy Board turns the Director, Engineer,
        // and Strategy signals into one visual command board. It stays fully
        // local and deterministic while giving the Garage a clear next move.
        val boardMove = when {
            strategyMovie == null -> "BUILD GRID"
            strategyLabel == "RESUME NEXT" && strategyProgress >= 75 -> "FINISH CURRENT"
            strategyLabel == "RESUME NEXT" -> "RESUME CURRENT"
            strategyLabel == "PIT PLAN" -> "OPEN PIT PLAN"
            strategyLabel == "QUALIFYING PICK" -> "QUALIFYING RUN"
            else -> "LAUNCH NEW MEDIA"
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(16.dp), blurRadius = 14.dp)
                .clickable(enabled = strategyMovie != null) {
                    strategyMovie?.let { onOpenMovie(it.uri, it.resumePositionMs) }
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("RACE STRATEGY BOARD", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
                Text("${fleetProgress}% FLEET", style = RvhType.Meta, color = TextSecondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StrategyBoardCell("PLAN", strategyLabel, Modifier.weight(1f))
                StrategyBoardCell("MOVE", boardMove, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StrategyBoardCell("ENGINEER", engineerCall, Modifier.weight(1f))
                StrategyBoardCell("DIRECTOR", directorStatus, Modifier.weight(1f))
            }
            if (strategyMovie != null) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        strategyMovie.displayName.substringBeforeLast('.'),
                        style = RvhType.Body,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (strategyProgress > 0) "$strategyProgress%" else "READY",
                        style = RvhType.Meta,
                        color = TextSecondary
                    )
                }
                Text(
                    "TAP BOARD TO ${boardMove.uppercase()}",
                    style = RvhType.Meta,
                    color = TextSecondary
                )
            } else {
                Text("NO ACTIVE PLAN  •  SCAN LIBRARY TO BUILD THE GRID", style = RvhType.Meta, color = TextSecondary)
            }
        }

        // Legendary Step 38: Race Command Deck turns the strategy board into
        // a final cockpit-style action layer. It is derived entirely from the
        // current local fleet state and keeps the existing one-tap launch path.
        val deckMode = when {
            strategyMovie == null -> "GRID BUILD"
            strategyLabel == "RESUME NEXT" -> "SESSION CONTROL"
            strategyLabel == "PIT PLAN" -> "QUEUE CONTROL"
            strategyLabel == "QUALIFYING PICK" -> "QUALIFYING CONTROL"
            else -> "LAUNCH CONTROL"
        }
        val deckAction = when {
            strategyMovie == null -> "SCAN LIBRARY"
            strategyLabel == "RESUME NEXT" -> "RESUME SESSION"
            strategyLabel == "PIT PLAN" -> "OPEN NEXT RUN"
            strategyLabel == "QUALIFYING PICK" -> "START QUALIFYING"
            else -> "LAUNCH MEDIA"
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(16.dp), blurRadius = 16.dp)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("RACE COMMAND DECK", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
                Text(deckMode, style = RvhType.Meta, color = TextSecondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommandDeckCell("STATUS", directorStatus, Modifier.weight(1f))
                CommandDeckCell("NEXT", deckAction, Modifier.weight(1f))
                CommandDeckCell("FLEET", "${movies.size}", Modifier.weight(0.55f))
            }
            if (strategyMovie != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMovie(strategyMovie.uri, strategyMovie.resumePositionMs) }
                        .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("COMMAND TARGET", style = RvhType.Meta, color = TextSecondary)
                        Text(
                            strategyMovie.displayName.substringBeforeLast('.'),
                            style = RvhType.Body,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    Text(
                        if (strategyProgress > 0) "$strategyProgress%" else "READY",
                        style = RvhType.Meta,
                        color = Color.White
                    )
                }
            } else {
                Text("NO COMMAND TARGET  •  SCAN LIBRARY TO OPEN THE DECK", style = RvhType.Meta, color = TextSecondary)
            }
        }

        // Legendary Step 47: the Global Media Command Bar keeps the selected
        // media actionable from the main Garage surface. It always targets the
        // same deterministic strategy item, so play/state/details never drift.
        if (strategyMovie != null) {
            GlobalMediaCommandBar(
                movie = strategyMovie,
                actionLabel = towerNextForCommand(strategyLabel, strategyProgress),
                onPlay = { onOpenMovie(strategyMovie.uri, strategyMovie.resumePositionMs) },
                onFavorite = { onToggleFavorite(strategyMovie.uri, !strategyMovie.isFavorite) },
                onWatchLater = { onToggleWatchLater(strategyMovie.uri, !strategyMovie.isWatchLater) },
                onDetails = { onOpenDetails(strategyMovie.uri) },
            )
        }

        // Legendary Step 50: the command center turns the selected media into
        // a single operational target with state, progress, and immediate actions.
        if (strategyMovie != null) {
            MediaCommandCenter(
                movie = strategyMovie,
                mode = strategyLabel,
                progressPercent = strategyProgress,
                onPlay = { onOpenMovie(strategyMovie.uri, strategyMovie.resumePositionMs) },
                onFavorite = { onToggleFavorite(strategyMovie.uri, !strategyMovie.isFavorite) },
                onWatchLater = { onToggleWatchLater(strategyMovie.uri, !strategyMovie.isWatchLater) },
                onDetails = { onOpenDetails(strategyMovie.uri) },
                onFocus = { focusMode = true },
            )
        }

        // Legendary Step 55: live session control provides a final operational
        // cockpit for the current target without introducing new persistence.
        if (strategyMovie != null) {
            LiveSessionControl(
                movie = strategyMovie,
                progressPercent = strategyProgress,
                activeSessions = inProgress.size,
                fleetProgress = fleetProgress,
                onPlay = { onOpenMovie(strategyMovie.uri, strategyMovie.resumePositionMs) },
                onDetails = { onOpenDetails(strategyMovie.uri) },
                onFocus = { focusMode = true },
            )
        }

        // Legendary Step 56: Pit Wall 2.0 coordinates multiple resumable sessions
        // and exposes a deterministic handoff target without adding persistence.
        if (inProgress.isNotEmpty()) {
            PitWallTwo(
                sessions = inProgress.sortedByDescending { it.resumePositionMs.toDouble() / it.durationMs.coerceAtLeast(1L) }.take(3),
                onOpen = { movie -> onOpenMovie(movie.uri, movie.resumePositionMs) },
                onDetails = { movie -> onOpenDetails(movie.uri) },
            )
        }

        // Legendary Step 39: Race Control Tower unifies the Garage command
        // signals into one operational overview. It stays fully local and
        // deterministic while exposing the most useful session controls.
        val towerStatus = when {
            strategyMovie == null -> "GRID CLOSED"
            inProgress.isNotEmpty() -> "LIVE SESSION"
            movies.any { it.isWatchLater } -> "QUEUE READY"
            movies.any { it.isFavorite } -> "QUALIFYING READY"
            else -> "GRID READY"
        }
        val towerNext = when {
            strategyMovie == null -> "SCAN LIBRARY"
            strategyLabel == "RESUME NEXT" -> if (strategyProgress >= 75) "FINISH CURRENT" else "RESUME CURRENT"
            strategyLabel == "PIT PLAN" -> "OPEN WATCH LATER"
            strategyLabel == "QUALIFYING PICK" -> "START QUALIFYING"
            else -> "LAUNCH MEDIA"
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(18.dp), blurRadius = 18.dp)
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("RACE CONTROL TOWER", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
                Text(towerStatus, style = RvhType.Meta, color = TextSecondary)
            }
            if (strategyMovie != null) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("CURRENT SESSION", style = RvhType.Meta, color = TextSecondary)
                        Text(
                            strategyMovie.displayName.substringBeforeLast('.'),
                            style = RvhType.CardTitle,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    Text(if (strategyProgress > 0) "$strategyProgress%" else "READY", style = RvhType.Meta, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TowerCommand("▶  $towerNext", Modifier.weight(1f)) {
                        onOpenMovie(strategyMovie.uri, strategyMovie.resumePositionMs)
                    }
                    TowerCommand("INFO", Modifier.weight(0.34f)) {
                        onOpenDetails(strategyMovie.uri)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FLEET ${movies.size}", style = RvhType.Meta, color = TextSecondary, modifier = Modifier.weight(1f))
                    Text("ACTIVE ${inProgress.size}", style = RvhType.Meta, color = TextSecondary)
                    Text("FLEET ${fleetProgress}%", style = RvhType.Meta, color = TextSecondary)
                }
            } else {
                Text("NO ACTIVE SESSION", style = RvhType.CardTitle, color = Color.White)
                Text("SCAN LIBRARY TO OPEN THE CONTROL TOWER", style = RvhType.Meta, color = TextSecondary)
            }
        }

        // Legendary Step 40: Smart Queue turns local playback intent into a
        // lightweight next-up queue. It is derived from resume state, Watch Later,
        // Favorites, and freshness without adding persistence or background work.
        val smartQueue = remember(movies, strategyMovie) {
            movies
                .filter { it.uri != strategyMovie?.uri }
                .sortedWith(
                    compareByDescending<com.rvh.video.data.model.LocalVideoEntity> {
                        when {
                            it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs -> 4
                            it.isWatchLater -> 3
                            it.isFavorite -> 2
                            else -> 1
                        }
                    }.thenByDescending {
                        if (it.durationMs > 0L) it.resumePositionMs.toDouble() / it.durationMs else 0.0
                    }.thenByDescending { it.dateModifiedEpochSeconds }
                )
                .take(3)
        }
        val queueMode = when {
            smartQueue.isEmpty() -> "STANDBY"
            smartQueue.any { it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs } -> "RESUME FIRST"
            smartQueue.any { it.isWatchLater } -> "WATCH LATER"
            smartQueue.any { it.isFavorite } -> "FAVORITES"
            else -> "FRESH MEDIA"
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(18.dp), blurRadius = 18.dp)
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("SMART QUEUE", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
                Text(queueMode, style = RvhType.Meta, color = TextSecondary)
            }
            Text(
                if (smartQueue.isEmpty()) "QUEUE CLEAR  •  SELECT MEDIA TO BUILD THE NEXT RUN"
                else "NEXT UP  •  ${smartQueue.size} LOCAL RUN${if (smartQueue.size == 1) "" else "S"}",
                style = RvhType.Meta,
                color = TextSecondary
            )
            smartQueue.forEachIndexed { index, movie ->
                val progress = if (movie.durationMs > 0L) {
                    (movie.resumePositionMs.coerceIn(0L, movie.durationMs) * 100L / movie.durationMs).toInt()
                } else 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMovie(movie.uri, movie.resumePositionMs) }
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}", style = RvhType.Meta, color = TextSecondary)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(movie.displayName.substringBeforeLast('.'), style = RvhType.Body, color = Color.White, maxLines = 1)
                        Text(
                            when {
                                progress > 0 -> "RESUME  •  $progress%"
                                movie.isWatchLater -> "WATCH LATER  •  READY"
                                movie.isFavorite -> "FAVORITE  •  READY"
                                else -> "FRESH  •  READY"
                            },
                            style = RvhType.Meta,
                            color = TextSecondary
                        )
                    }
                    Text("▶", style = RvhType.Meta, color = Color.White)
                }
            }
        }

        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape = RoundedCornerShape(12.dp), blurRadius = 12.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search your movie bay", style = RvhType.Body, color = TextSecondary)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (performanceMode) 4 else gridColumns),
            modifier = Modifier.heightIn(min = 420.dp, max = 760.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(filtered, key = { it.uri }) { movie ->
                MovieGridCard(
                    movie = movie,
                    onClick = { onOpenMovie(movie.uri, movie.resumePositionMs) },
                    onRecategorize = { viewModel.recategorize(movie.uri, it) },
                    onToggleFavorite = { onToggleFavorite(movie.uri, !movie.isFavorite) },
                    onAddToCollection = { onAddToCollection(movie) },
                    isWatchLater = movie.isWatchLater,
                    onToggleWatchLater = { onToggleWatchLater(movie.uri, !movie.isWatchLater) },
                    onDetails = { onOpenDetails(movie.uri) },
                    performanceMode = performanceMode,
                )
            }
        }


    }
}


@Composable
private fun StrategyBoardCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = RvhType.Meta, color = TextSecondary, maxLines = 1)
        Text(value, style = RvhType.Meta, color = Color.White, maxLines = 2)
    }
}

@Composable
private fun TowerCommand(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.075f), RoundedCornerShape(10.dp)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, style = RvhType.Meta, color = Color.White, maxLines = 1)
    }
}


@Composable
private fun CommandDeckCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = RvhType.Meta, color = TextSecondary, maxLines = 1)
        Text(value, style = RvhType.Meta, color = Color.White, maxLines = 2)
    }
}


@Composable
private fun GarageStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .glassSurface(shape = RoundedCornerShape(14.dp), blurRadius = 10.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = RvhType.Meta, color = TextSecondary)
        Text(value.toString().padStart(2, '0'), style = RvhType.CardTitle, color = Color.White)
    }
}


private fun towerNextForCommand(label: String, progress: Int): String = when {
    label == "RESUME NEXT" -> if (progress >= 75) "FINISH" else "RESUME"
    label == "PIT PLAN" -> "WATCH LATER"
    label == "QUALIFYING PICK" -> "QUALIFY"
    else -> "PLAY NEXT"
}

@Composable
private fun MediaFocusMode(
    movie: LocalVideoEntity,
    actionLabel: String,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onWatchLater: () -> Unit,
    onDetails: () -> Unit,
    onExit: () -> Unit,
) {
    val progress = if (movie.durationMs > 0L) {
        (movie.resumePositionMs.coerceIn(0L, movie.durationMs) * 100L / movie.durationMs).toInt()
    } else 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(20.dp), blurRadius = 22.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MEDIA FOCUS MODE", style = RvhType.Meta, color = TextSecondary)
                Text(movie.displayName.substringBeforeLast('.'), style = RvhType.ScreenTitle, color = Color.White, maxLines = 2)
            }
            TextButton(onClick = onExit) { Text("EXIT", style = RvhType.Meta, color = Color.White) }
        }
        Text(
            when { progress >= 75 -> "FINAL STRETCH  •  $progress% COMPLETE"
                progress > 0 -> "SESSION LOCKED  •  $progress% COMPLETE"
                movie.isWatchLater -> "QUEUED  •  READY TO LAUNCH"
                movie.isFavorite -> "QUALIFYING  •  READY TO LAUNCH"
                else -> "READY TO LAUNCH" },
            style = RvhType.Meta, color = TextSecondary
        )
        Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50.dp)).padding(vertical = 3.dp)) {
            Box(modifier = Modifier.fillMaxWidth(progress / 100f).background(Color.White, RoundedCornerShape(50.dp)).padding(vertical = 3.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CommandBarButton("▶ $actionLabel", Modifier.weight(1.4f), onPlay)
            CommandBarButton(if (movie.isFavorite) "★ FAV" else "☆ FAV", Modifier.weight(.8f), onFavorite)
            CommandBarButton(if (movie.isWatchLater) "✓ LATER" else "＋ LATER", Modifier.weight(.9f), onWatchLater)
            CommandBarButton("INFO", Modifier.weight(.6f), onDetails)
        }
        Text("FOCUS LOCK  •  ONE TARGET  •  ONE COMMAND  •  ZERO DISTRACTIONS", style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun GlobalMediaCommandBar(
    movie: LocalVideoEntity,
    actionLabel: String,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onWatchLater: () -> Unit,
    onDetails: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(16.dp), blurRadius = 14.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("MEDIA COMMAND BAR", style = RvhType.Meta, color = Color.White)
                Text(
                    movie.displayName.substringBeforeLast('.'),
                    style = RvhType.CardTitle,
                    color = Color.White,
                    maxLines = 1
                )
            }
            Text(if (movie.resumePositionMs > 0L && movie.durationMs > 0L) "${(movie.resumePositionMs * 100L / movie.durationMs).coerceIn(0L, 100L)}%" else "READY", style = RvhType.Meta, color = TextSecondary)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CommandBarButton("▶ $actionLabel", Modifier.weight(1.35f), onPlay)
            CommandBarButton(if (movie.isFavorite) "★ FAV" else "☆ FAV", Modifier.weight(0.75f), onFavorite)
            CommandBarButton(if (movie.isWatchLater) "✓ LATER" else "＋ LATER", Modifier.weight(0.85f), onWatchLater)
            CommandBarButton("INFO", Modifier.weight(0.55f), onDetails)
        }
    }
}

@Composable
private fun CommandBarButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.background(Color.White.copy(alpha = 0.075f), RoundedCornerShape(9.dp)),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Text(label, style = RvhType.Meta, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun PitWall(
    total: Int,
    ready: Int,
    running: Int,
    favorites: Int,
    watchLater: Int,
    visible: Int,
    filter: String,
) {
    val filterLabel = when (filter) {
        "resume" -> "IN PROGRESS"
        "favorites" -> "FAVORITES"
        "later" -> "WATCH LATER"
        else -> "ALL MEDIA"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(14.dp), blurRadius = 12.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("PIT WALL", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
            Text("${filterLabel}  •  $visible VISIBLE", style = RvhType.Meta, color = TextSecondary)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PitWallCell("TOTAL", total)
            PitWallCell("READY", ready)
            PitWallCell("RUN", running)
            PitWallCell("FAV", favorites)
            PitWallCell("LATER", watchLater)
        }
        Text(
            if (running > 0) "TRACK STATUS  •  $running SESSION${if (running == 1) "" else "S"} IN PROGRESS"
            else "TRACK STATUS  •  STANDBY  •  READY FOR LAUNCH",
            style = RvhType.Meta,
            color = TextSecondary
        )
    }
}

@Composable
private fun RowScope.PitWallCell(label: String, value: Int) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = RvhType.Meta, color = TextSecondary, fontSize = 8.sp)
        Text(value.toString().padStart(2, '0'), style = RvhType.Meta, color = Color.White)
    }
}


@Composable
private fun MissionControl(
    movies: List<LocalVideoEntity>,
    target: LocalVideoEntity?,
    progressPercent: Int,
    onOpenMovie: (String, Long) -> Unit,
) {
    val active = movies.count { it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }
    val queued = movies.count { it.isWatchLater }
    val favorites = movies.count { it.isFavorite }
    val state = when {
        target == null -> "GRID EMPTY"
        progressPercent >= 75 -> "FINAL LAP"
        progressPercent > 0 -> "SESSION LIVE"
        target.isWatchLater -> "QUEUE READY"
        target.isFavorite -> "QUALIFYING"
        else -> "GRID READY"
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .glassSurface(RoundedCornerShape(16.dp), if (target != null) 16.dp else 10.dp)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MISSION CONTROL", style = RvhType.Meta, color = Color.White)
                Text("GARAGE OPERATIONS  •  $state", style = RvhType.Meta, color = TextSecondary)
            }
            Text("RVH LIVE", style = RvhType.Meta, color = Color.White)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("MEDIA" to movies.size, "ACTIVE" to active, "QUEUE" to queued, "FAV" to favorites).forEach { (label, value) ->
                Column(
                    modifier = Modifier.weight(1f)
                        .background(Color.White.copy(alpha = .06f), RoundedCornerShape(9.dp))
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, style = RvhType.Meta, color = TextSecondary, fontSize = 8.sp)
                    Text(value.toString().padStart(2, '0'), style = RvhType.Meta, color = Color.White)
                }
            }
        }

        if (target != null) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha = .075f), RoundedCornerShape(11.dp))
                    .clickable { onOpenMovie(target.uri, target.resumePositionMs) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("COMMAND TARGET", style = RvhType.Meta, color = TextSecondary)
                    Text(target.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1)
                    Text(
                        if (progressPercent > 0) "$progressPercent% COMPLETE  •  TAP TO RESUME" else "READY  •  TAP TO LAUNCH",
                        style = RvhType.Meta, color = TextSecondary
                    )
                }
                Text(if (progressPercent > 0) "$progressPercent%" else "GO", style = RvhType.CardTitle, color = Color.White)
            }
        } else {
            Text("NO COMMAND TARGET  •  SCAN YOUR LIBRARY TO ACTIVATE MISSION CONTROL", style = RvhType.Meta, color = TextSecondary)
        }

        if (target != null) {
            val rail = progressPercent.coerceIn(0, 100) / 100f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MISSION LOAD", style = RvhType.Meta, color = TextSecondary)
                    Text(if (progressPercent > 0) "$progressPercent%" else "READY", style = RvhType.Meta, color = Color.White)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(5.dp)
                        .background(Color.White.copy(alpha = .08f), RoundedCornerShape(50.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(rail)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = .72f), RoundedCornerShape(50.dp))
                    )
                }
            }
        }

        Text("LOCAL ONLY  •  LIVE TELEMETRY  •  ${if (target != null) "COMMAND READY" else "AWAITING GRID"}", style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun MissionSequence(
    movies: List<LocalVideoEntity>,
    onOpenMovie: (String, Long) -> Unit,
) {
    // Step 64: the sequence is recalculated from live media state on every
    // recomposition, so finishing/resuming/toggling intent automatically
    // changes the order without a separate queue or persistence layer.
    val queue = movies
        .asSequence()
        .filter { it.durationMs > 0L || it.isWatchLater || it.isFavorite }
        .distinctBy { it.uri }
        .sortedWith(
            compareByDescending<LocalVideoEntity> { movie ->
                when {
                    movie.durationMs > 0L && movie.resumePositionMs >= movie.durationMs -> 1
                    movie.durationMs > 0L && movie.resumePositionMs > 0L -> 5
                    movie.isWatchLater -> 4
                    movie.isFavorite -> 3
                    else -> 2
                }
            }.thenByDescending { movie ->
                if (movie.durationMs > 0L) {
                    movie.resumePositionMs.toDouble() / movie.durationMs.coerceAtLeast(1L)
                } else 0.0
            }.thenByDescending { movie -> movie.dateModifiedEpochSeconds }
        )
        .take(4)
        .toList()
    val sessions = queue.filter { it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }

    Column(
        modifier = Modifier.fillMaxWidth()
            .glassSurface(RoundedCornerShape(16.dp), 14.dp)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MISSION SEQUENCE", style = RvhType.Meta, color = Color.White)
                Text("MULTI-SESSION COMMAND  •  ${queue.size} STAGED", style = RvhType.Meta, color = TextSecondary)
            }
            Text(if (sessions.isNotEmpty()) "LIVE" else "STANDBY", style = RvhType.Meta, color = Color.White)
        }

        if (queue.isEmpty()) {
            Text("NO SESSIONS STAGED  •  ADD MEDIA TO WATCH LATER OR FAVORITES", style = RvhType.Meta, color = TextSecondary)
        } else {
            queue.forEachIndexed { index, movie ->
                val percent = if (movie.durationMs > 0L) {
                    (movie.resumePositionMs.coerceIn(0L, movie.durationMs) * 100L / movie.durationMs).toInt()
                } else 0
                val reason = when {
                    percent > 0 -> if (percent >= 75) "FINAL LAP" else "ACTIVE SESSION"
                    movie.isWatchLater -> "WATCH LATER"
                    movie.isFavorite -> "FAVORITE"
                    else -> "STAGED"
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.White.copy(alpha = if (index == 0) .09f else .055f), RoundedCornerShape(11.dp))
                        .clickable { onOpenMovie(movie.uri, movie.resumePositionMs) }
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}".padStart(2, '0'), style = RvhType.Meta, color = TextSecondary)
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(movie.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1)
                        Text(if (percent > 0) "$reason  •  $percent%" else reason, style = RvhType.Meta, color = TextSecondary)
                    }
                    Text(if (percent > 0) "RESUME" else "LAUNCH", style = RvhType.Meta, color = Color.White)
                }
            }
        }

        Text("LOCAL SEQUENCER  •  TAP ANY RUN TO HAND OFF", style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun GarageCommandHud(
    target: LocalVideoEntity?,
    targetProgress: Int,
    personalMode: String,
    activeCount: Int,
    queueCount: Int,
    onOpenMovie: (String, Long) -> Unit,
) {
    val missionState = when {
        target == null -> "STANDBY"
        targetProgress >= 75 -> "FINAL LAP"
        targetProgress > 0 -> "IN MOTION"
        target.isWatchLater -> "QUEUED"
        target.isFavorite -> "QUALIFY"
        else -> "READY"
    }
    val command = when (missionState) {
        "FINAL LAP" -> "FINISH CURRENT"
        "IN MOTION" -> "RESUME SESSION"
        "QUEUED" -> "OPEN NEXT RUN"
        "QUALIFY" -> "START QUALIFYING"
        "READY" -> "LAUNCH MEDIA"
        else -> "SCAN LIBRARY"
    }
    val progress = targetProgress.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat(),
        animationSpec = tween(durationMillis = 520),
        label = "hudProgress"
    )
    val pulse = rememberInfiniteTransition(label = "hudPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hudBeacon"
    )
    val beaconAlpha = if (target == null) 0.35f else pulse.value
    val changedMessage = when (missionState) {
        "FINAL LAP" -> "SESSION NEARLY COMPLETE"
        "IN MOTION" -> if (targetProgress > 0) "RESUME POSITION DETECTED" else "SESSION ACTIVE"
        "QUEUED" -> "WATCH LATER TARGET PROMOTED"
        "QUALIFY" -> "FAVORITE TARGET PROMOTED"
        "READY" -> "NEW TARGET READY"
        else -> "NO TARGET SELECTED"
    }
    val nextMove = when (missionState) {
        "FINAL LAP" -> "FINISH IT"
        "IN MOTION" -> "RESUME NOW"
        "QUEUED" -> "OPEN QUEUE"
        "QUALIFY" -> "START RUN"
        "READY" -> "LAUNCH"
        else -> "SCAN LIBRARY"
    }

    val commandPulse by animateFloatAsState(
        targetValue = when (missionState) {
            "FINAL LAP" -> 0.12f
            "IN MOTION" -> 0.10f
            "QUEUED" -> 0.085f
            "QUALIFY" -> 0.075f
            "READY" -> 0.065f
            else -> 0.045f
        },
        animationSpec = tween(durationMillis = 420),
        label = "hudStatePulse"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
            .glassSurface(RoundedCornerShape(16.dp), 14.dp)
            .background(Color.White.copy(alpha = commandPulse), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("GARAGE COMMAND HUD", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("●", style = RvhType.Meta, color = Color.White.copy(alpha = beaconAlpha))
                Text(missionState, style = RvhType.Meta, color = Color.White)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HudMetric("ACTIVE", activeCount)
            HudMetric("QUEUE", queueCount)
            HudMetric("MODE", personalMode)
        }
        if (target != null) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onOpenMovie(target.uri, target.resumePositionMs) }
                    .background(Color.White.copy(alpha = .07f + commandPulse * .35f), RoundedCornerShape(11.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("CURRENT COMMAND", style = RvhType.Meta, color = TextSecondary)
                    Text(target.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1)
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 2.dp).background(Color.White.copy(alpha = .08f), RoundedCornerShape(50.dp)).padding(vertical = 1.5.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(animatedProgress / 100f).background(Color.White, RoundedCornerShape(50.dp)).padding(vertical = 1.5.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$progress%", style = RvhType.CardTitle, color = Color.White)
                    Text(command, style = RvhType.Meta, color = TextSecondary)
                }
            }
        } else {
            Text("NO ACTIVE TARGET  •  READY FOR LOCAL MEDIA", style = RvhType.Meta, color = TextSecondary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
                    .background(Color.White.copy(alpha = .045f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("JUST CHANGED", style = RvhType.Meta, color = TextSecondary)
                Text(changedMessage, style = RvhType.Meta, color = Color.White, maxLines = 1)
            }
            Column(
                modifier = Modifier.weight(1f)
                    .background(Color.White.copy(alpha = .065f + commandPulse * .22f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("NEXT MOVE", style = RvhType.Meta, color = TextSecondary)
                Text(nextMove, style = RvhType.Meta, color = Color.White, maxLines = 1)
            }
        }
        Text("HUD 5.0  •  COMMAND PULSE  •  $missionState  •  LOCAL ONLY", style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun RowScope.HudMetric(label: String, value: Any) {
    Column(
        modifier = Modifier.weight(1f)
            .background(Color.White.copy(alpha = .055f), RoundedCornerShape(9.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = RvhType.Meta, color = TextSecondary)
        Text(value.toString(), style = RvhType.Meta, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun CinematicMissionFocus(
    target: LocalVideoEntity?,
    targetProgress: Int,
    missionState: String,
    onOpenMovie: (String, Long) -> Unit,
) {
    val title = target?.displayName?.substringBeforeLast('.') ?: "NO PRIMARY TARGET"
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 520),
        label = "missionFocusProgress"
    )
    val beaconAlpha by rememberInfiniteTransition(label = "missionFocusBeacon").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "missionFocusBeaconAlpha"
    )
    val focusLabel = when (missionState) {
        "FINAL_LAP" -> "PRIMARY TARGET • FINAL LAP"
        "IN_MOTION" -> "PRIMARY TARGET • IN MOTION"
        "QUEUED" -> "PRIMARY TARGET • QUEUED"
        "QUALIFY" -> "PRIMARY TARGET • QUALIFY"
        "READY" -> "PRIMARY TARGET • READY"
        else -> "PRIMARY TARGET • STANDBY"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glassSurface(shape = RoundedCornerShape(14.dp), blurRadius = 14.dp)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(Color.White.copy(alpha = beaconAlpha), RoundedCornerShape(50)))
            Spacer(Modifier.width(8.dp))
            Text("MISSION FOCUS", style = RvhType.Meta, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text(focusLabel, style = RvhType.Meta, color = TextSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = RvhType.Body, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$targetProgress% COMPLETE", style = RvhType.Meta, color = TextSecondary)
            Spacer(Modifier.weight(1f))
            if (target != null) {
                TextButton(onClick = { onOpenMovie(target.uri, target.resumePositionMs) }) {
                    Text(if (target.resumePositionMs > 0L) "RESUME" else "LAUNCH", style = RvhType.Meta, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CommandDeck(
    movies: List<LocalVideoEntity>,
    target: LocalVideoEntity?,
    targetProgress: Int,
    personalMode: String,
    onOpenMovie: (String, Long) -> Unit,
) {
    val active = movies.count { it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }
    val queue = movies.count { it.isWatchLater }
    val favorites = movies.count { it.isFavorite }
    val completed = movies.count { it.durationMs > 0L && it.resumePositionMs >= it.durationMs }
    val totalDuration = movies.sumOf { it.durationMs.coerceAtLeast(0L) }
    val consumed = movies.sumOf { it.resumePositionMs.coerceIn(0L, it.durationMs.coerceAtLeast(0L)) }
    val fleetProgress = if (totalDuration > 0L) (consumed * 100L / totalDuration).toInt().coerceIn(0, 100) else 0
    val state = when {
        target == null -> "GRID EMPTY"
        targetProgress >= 75 -> "FINAL LAP"
        targetProgress > 0 -> "SESSION LIVE"
        target.isWatchLater -> "QUEUE READY"
        target.isFavorite -> "QUALIFYING"
        else -> "GRID READY"
    }
    val directive = when {
        target == null -> "SCAN LIBRARY"
        targetProgress >= 75 -> "FINISH CURRENT"
        targetProgress > 0 -> "RESUME SESSION"
        target.isWatchLater -> "OPEN WATCH LATER"
        target.isFavorite -> "START QUALIFYING"
        else -> "LAUNCH MEDIA"
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .glassSurface(RoundedCornerShape(18.dp), 16.dp)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("RVH COMMAND DECK", style = RvhType.Meta, color = Color.White)
                Text("ONE GLANCE  •  ONE TARGET  •  ONE COMMAND", style = RvhType.Meta, color = TextSecondary)
            }
            Text(state, style = RvhType.Meta, color = Color.White)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("MEDIA" to movies.size, "ACTIVE" to active, "QUEUE" to queue, "FAV" to favorites).forEach { (label, value) ->
                Column(
                    modifier = Modifier.weight(1f).background(Color.White.copy(alpha = .06f), RoundedCornerShape(10.dp)).padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, style = RvhType.Meta, color = TextSecondary)
                    Text(value.toString(), style = RvhType.CardTitle, color = Color.White)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("FLEET LOAD", style = RvhType.Meta, color = TextSecondary)
                Text("$fleetProgress% CONSUMED  •  $completed COMPLETE", style = RvhType.Meta, color = Color.White)
            }
            Text("MODE $personalMode", style = RvhType.Meta, color = TextSecondary)
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = .07f), RoundedCornerShape(50.dp)).padding(vertical = 2.dp)) {
            Box(modifier = Modifier.fillMaxWidth(fleetProgress / 100f).background(Color.White, RoundedCornerShape(50.dp)).padding(vertical = 2.dp))
        }
        if (target != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = .085f), RoundedCornerShape(12.dp))
                    .clickable { onOpenMovie(target.uri, target.resumePositionMs) }.padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("COMMAND TARGET", style = RvhType.Meta, color = TextSecondary)
                    Text(target.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1)
                    Text("$directive  •  $targetProgress%", style = RvhType.Meta, color = TextSecondary)
                }
                Text("GO", style = RvhType.CardTitle, color = Color.White)
            }
        } else {
            Text("NO COMMAND TARGET  •  ADD MEDIA TO THE GRID", style = RvhType.Meta, color = TextSecondary)
        }
        Text("LOCAL COMMAND DECK  •  LIVE STATE  •  NO CLOUD REQUIRED", style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun MissionTimeline(
    movies: List<LocalVideoEntity>,
    target: LocalVideoEntity?,
    targetProgress: Int,
    personalMode: String,
    onOpenMovie: (String, Long) -> Unit,
) {
    val staged = movies.asSequence()
        .filter { it.durationMs > 0L || it.isWatchLater || it.isFavorite }
        .distinctBy { it.uri }
        .sortedWith(
            compareByDescending<LocalVideoEntity> { movie ->
                when {
                    movie.durationMs > 0L && movie.resumePositionMs > 0L && movie.resumePositionMs < movie.durationMs -> 5
                    movie.isWatchLater -> 4
                    movie.isFavorite -> 3
                    else -> 2
                }
            }.thenByDescending { movie ->
                if (movie.durationMs > 0L) movie.resumePositionMs.toDouble() / movie.durationMs.coerceAtLeast(1L) else 0.0
            }.thenByDescending { it.dateModifiedEpochSeconds }
        )
        .take(3)
        .toList()

    val nextRuns = staged.filter { target == null || it.uri != target.uri }.take(2)
    val flowState = when {
        target == null -> "WAITING"
        targetProgress >= 75 -> "FINAL LAP"
        targetProgress > 0 -> "IN MOTION"
        target.isWatchLater -> "QUEUED"
        target.isFavorite -> "QUALIFY"
        else -> "READY"
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .glassSurface(RoundedCornerShape(20.dp), 16.dp)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("CINEMATIC GARAGE FLOW", style = RvhType.Meta, color = Color.White)
                Text("CURRENT → NEXT → QUEUED", style = RvhType.Meta, color = TextSecondary)
            }
            Text(flowState, style = RvhType.Meta, color = Color.White)
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FlowNode("CURRENT", target != null, true)
            FlowConnector(active = target != null)
            FlowNode("NEXT", nextRuns.isNotEmpty(), false)
            FlowConnector(active = nextRuns.isNotEmpty())
            FlowNode("QUEUED", staged.size > 2, false)
        }

        if (target != null) {
            val progress = targetProgress.coerceIn(0, 100)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha = .09f), RoundedCornerShape(14.dp))
                    .clickable { onOpenMovie(target.uri, target.resumePositionMs) }
                    .padding(horizontal = 11.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("CURRENT RUN  •  $personalMode DRIVE", style = RvhType.Meta, color = TextSecondary)
                    Text(target.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1)
                    Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = .10f), RoundedCornerShape(50.dp)).padding(vertical = 2.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(progress / 100f).background(Color.White, RoundedCornerShape(50.dp)).padding(vertical = 2.dp))
                    }
                    Text(if (progress > 0) "$progress% COMPLETE  •  TAP TO RESUME" else "READY TO LAUNCH  •  TAP TO START", style = RvhType.Meta, color = TextSecondary)
                }
                Text(if (progress > 0) "$progress%" else "GO", style = RvhType.CardTitle, color = Color.White)
            }
        } else {
            Text("CURRENT RUN EMPTY  •  BUILD THE GRID", style = RvhType.Meta, color = TextSecondary)
        }

        if (nextRuns.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                nextRuns.forEachIndexed { index, movie ->
                    val progress = if (movie.durationMs > 0L) {
                        (movie.resumePositionMs.coerceIn(0L, movie.durationMs) * 100L / movie.durationMs).toInt()
                    } else 0
                    Column(
                        modifier = Modifier.weight(1f)
                            .background(Color.White.copy(alpha = if (index == 0) .07f else .045f), RoundedCornerShape(12.dp))
                            .clickable { onOpenMovie(movie.uri, movie.resumePositionMs) }
                            .padding(9.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(if (index == 0) "NEXT RUN" else "QUEUED RUN", style = RvhType.Meta, color = TextSecondary)
                        Text(movie.displayName.substringBeforeLast('.'), style = RvhType.Meta, color = Color.White, maxLines = 1)
                        Text(if (progress > 0) "$progress%  •  RESUME" else if (movie.isWatchLater) "WATCH LATER  •  READY" else "READY", style = RvhType.Meta, color = TextSecondary)
                    }
                }
            }
        }

        Text("LOCAL FLOW  •  LIVE HANDOFF  •  NO CLOUD REQUIRED", style = RvhType.Meta, color = TextSecondary)
    }
}

@Composable
private fun FlowNode(label: String, active: Boolean, primary: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = if (active) .16f else .055f), RoundedCornerShape(50.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Text(label, style = RvhType.Meta, color = if (active) Color.White else TextSecondary)
        }
        Spacer(Modifier.padding(top = if (primary && active) 1.dp else 0.dp))
    }
}

@Composable
private fun RowScope.FlowConnector(active: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 3.dp)
            .height(2.dp)
            .background(Color.White.copy(alpha = if (active) .28f else .08f), RoundedCornerShape(50.dp))
    )
}

@Composable
private fun RaceDashboard(
    movies: List<LocalVideoEntity>,
    target: LocalVideoEntity?,
    targetProgress: Int,
    personalMode: String,
    onOpenMovie: (String, Long) -> Unit,
) {
    val active = movies.filter { it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }
    val queue = movies.filter { it.isWatchLater }
    val completed = movies.count { it.durationMs > 0L && it.resumePositionMs >= it.durationMs }
    val fleetProgress = if (movies.isEmpty()) 0 else {
        val playable = movies.filter { it.durationMs > 0L }
        if (playable.isEmpty()) 0 else {
            (playable.sumOf { it.resumePositionMs.coerceIn(0L, it.durationMs) * 100L / it.durationMs } / playable.size).toInt()
        }
    }
    val next = active.firstOrNull { it.uri != target?.uri }
        ?: queue.firstOrNull { it.uri != target?.uri }
        ?: movies.firstOrNull { it.uri != target?.uri }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(14.dp), blurRadius = 12.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("RACE DASHBOARD", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
            Text("$personalMode DRIVE", style = RvhType.Meta, color = TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardMetric("MEDIA", movies.size)
            DashboardMetric("ACTIVE", active.size)
            DashboardMetric("QUEUE", queue.size)
            DashboardMetric("DONE", completed)
        }
        Text("FLEET ${fleetProgress}%", style = RvhType.Meta, color = TextSecondary)
        LinearProgressIndicator(progress = { fleetProgress.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth())
        if (target != null) {
            Text("CURRENT", style = RvhType.Meta, color = TextSecondary)
            Text(target.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${targetProgress.coerceIn(0, 100)}% COMPLETE", style = RvhType.Meta, color = AccentTeal)
            TextButton(onClick = { onOpenMovie(target.uri, target.resumePositionMs) }) { Text(if (target.resumePositionMs > 0L) "RESUME" else "LAUNCH") }
        }
        if (next != null) {
            Text("NEXT  •  ${next.displayName.substringBeforeLast('.')}", style = RvhType.Meta, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RowScope.DashboardMetric(label: String, value: Int) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(9.dp))
            .padding(horizontal = 7.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = RvhType.Meta, color = TextSecondary)
        Text(value.toString(), style = RvhType.Meta, color = Color.White)
    }
}


@Composable
private fun PersonalDrive(
    movies: List<LocalVideoEntity>,
    mode: String,
    onModeChange: (String) -> Unit,
    onOpenMovie: (String, Long) -> Unit,
) {
    val active = movies.filter { it.durationMs > 0L && it.resumePositionMs > 0L && it.resumePositionMs < it.durationMs }
    val source = when (mode) {
        "CONTINUE" -> active
        "QUEUE" -> movies.filter { it.isWatchLater }
        "FAVORITES" -> movies.filter { it.isFavorite }
        "FRESH" -> movies.sortedByDescending { it.dateModifiedEpochSeconds }
        else -> movies
    }
    val candidates = source.sortedWith(
        compareByDescending<LocalVideoEntity> {
            when {
                it in active -> 5
                it.isWatchLater -> 4
                it.isFavorite -> 3
                it.resumePositionMs > 0L -> 2
                else -> 1
            }
        }.thenByDescending {
            if (it.durationMs > 0L) it.resumePositionMs.toDouble() / it.durationMs else 0.0
        }.thenByDescending { it.dateModifiedEpochSeconds }
    ).distinctBy { it.uri }.take(3)

    val lead = candidates.firstOrNull()
    val leadProgress = if (lead?.durationMs ?: 0L > 0L) {
        ((lead!!.resumePositionMs.coerceIn(0L, lead.durationMs) * 100L) / lead.durationMs).toInt()
    } else 0
    val modeCaption = when (mode) {
        "CONTINUE" -> "ACTIVE SESSIONS"
        "QUEUE" -> "WATCH LATER QUEUE"
        "FAVORITES" -> "PERSONAL PICKS"
        "FRESH" -> "LATEST ARRIVALS"
        else -> "ADAPTIVE LOCAL MIX"
    }

    Column(
        modifier = Modifier.fillMaxWidth().glassSurface(RoundedCornerShape(16.dp), 14.dp).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PERSONAL DRIVE 2.1", style = RvhType.Meta, color = Color.White)
                Text(modeCaption + "  •  CHOOSE YOUR NEXT RUN", style = RvhType.Meta, color = TextSecondary)
            }
            Text("${candidates.size}/3", style = RvhType.Meta, color = TextSecondary)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("SMART", "CONTINUE", "QUEUE", "FAVORITES", "FRESH").forEach { option ->
                FilterChip(selected = mode == option, onClick = { onModeChange(option) }, label = { Text(option, style = RvhType.Meta) })
            }
        }

        if (lead != null) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha = .085f), RoundedCornerShape(12.dp))
                    .clickable { onOpenMovie(lead.uri, lead.resumePositionMs) }
                    .padding(horizontal = 11.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("NEXT RUN", style = RvhType.Meta, color = TextSecondary)
                    Text(lead.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1)
                    Text(
                        when {
                            lead in active -> "RESUME MOMENTUM  •  $leadProgress%"
                            lead.isWatchLater -> "QUEUE INTENT  •  READY"
                            lead.isFavorite -> "PERSONAL PICK  •  READY"
                            else -> "FRESHNESS  •  READY"
                        },
                        style = RvhType.Meta, color = TextSecondary, maxLines = 1
                    )
                }
                Text(if (leadProgress > 0) "$leadProgress%" else "GO", style = RvhType.CardTitle, color = Color.White)
            }
            Box(
                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = .07f), RoundedCornerShape(50.dp)).padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(leadProgress.coerceIn(0, 100) / 100f)
                        .background(Color.White, RoundedCornerShape(50.dp)).padding(vertical = 2.dp)
                )
            }
        }

        if (candidates.isEmpty()) {
            Text(
                when (mode) {
                    "CONTINUE" -> "NO ACTIVE SESSIONS  •  LAUNCH SOMETHING NEW"
                    "QUEUE" -> "WATCH LATER IS EMPTY  •  BUILD YOUR QUEUE"
                    "FAVORITES" -> "NO FAVORITES YET  •  STAR YOUR BEST MEDIA"
                    "FRESH" -> "NO MEDIA AVAILABLE  •  SCAN YOUR LIBRARY"
                    else -> "YOUR DRIVE IS EMPTY  •  SCAN YOUR LIBRARY TO START"
                },
                style = RvhType.Meta, color = TextSecondary
            )
        } else {
            candidates.drop(1).forEachIndexed { index, movie ->
                val progress = if (movie.durationMs > 0L) (movie.resumePositionMs.coerceIn(0L, movie.durationMs) * 100L / movie.durationMs).toInt() else 0
                val reason = when (mode) {
                    "CONTINUE" -> "ACTIVE SESSION  •  $progress% COMPLETE"
                    "QUEUE" -> "WATCH LATER  •  QUEUED"
                    "FAVORITES" -> "PERSONAL PICK  •  READY"
                    "FRESH" -> "RECENT MEDIA  •  READY"
                    else -> when {
                        movie in active -> "CONTINUE  •  $progress% COMPLETE"
                        movie.isWatchLater -> "WATCH LATER  •  QUEUED"
                        movie.isFavorite -> "FAVORITE  •  READY"
                        else -> "FRESH MEDIA  •  READY"
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = .045f), RoundedCornerShape(10.dp))
                        .clickable { onOpenMovie(movie.uri, movie.resumePositionMs) }.padding(horizontal = 9.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("0${index + 2}", style = RvhType.Meta, color = TextSecondary, modifier = Modifier.width(25.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(movie.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1)
                        Text(reason, style = RvhType.Meta, color = TextSecondary, maxLines = 1)
                    }
                    Text(if (progress > 0) "$progress%" else "GO", style = RvhType.Meta, color = Color.White)
                }
            }
        }
        Text("LOCAL ONLY  •  NEXT RUN  •  LIVE SIGNALS  •  TAP TO LAUNCH", style = RvhType.Meta, color = TextSecondary)
    }
}

