package com.rvh.video.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rvh.video.BuildConfig
import com.rvh.video.data.classification.VideoScanner
import com.rvh.video.data.local.AppSettings
import com.rvh.video.data.local.RvhDatabase
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.Surface0
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(onThemeChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { AppSettings(context) }

    var autoFloatingPlay by remember { mutableStateOf(settings.autoFloatingPlayEnabled) }
    var scanning by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showForceRescanConfirm by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var librarySort by remember { mutableStateOf(settings.librarySortMode) }
    var gridColumns by remember { mutableStateOf(settings.movieGridColumns) }
    var autoAdvance by remember { mutableStateOf(settings.autoAdvanceEnabled) }
    var defaultSpeed by remember { mutableStateOf(settings.defaultPlaybackSpeed) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            
            .statusBarsPadding() // was rendering under the status bar icons
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text("Profile", style = RvhType.ScreenTitle, color = Color.White, modifier = Modifier.padding(vertical = 16.dp))

        SettingsSection("Library") {
            SettingsRow(
                icon = Icons.Filled.Search,
                label = if (scanning) "Scanning…" else "Scan for new videos",
                onClick = {
                    if (!scanning) {
                        scanning = true
                        scope.launch {
                            VideoScanner(context, RvhDatabase.get(context).localVideoDao()).scan()
                            scanning = false
                        }
                    }
                }
            )
            SettingsRow(
                icon = Icons.Filled.Refresh,
                label = if (scanning) "Scanning…" else "Force full rescan",
                onClick = {
                    if (!scanning) showForceRescanConfirm = true
                }
            )
        }

        SettingsSection("Library Display") {
            SettingsRow(icon = Icons.Filled.Sort, label = "Default sort: ${sortLabel(librarySort)}", onClick = { showSortDialog = true })
            SettingsRow(icon = Icons.Filled.ViewModule, label = "Movie grid: $gridColumns columns", onClick = { showGridDialog = true })
        }

        SettingsSection("Appearance") {
            SettingsRow(
                icon = Icons.Filled.DarkMode,
                label = "Theme: ${themeLabel(themeMode)}",
                onClick = { showThemeDialog = true }
            )
        }

        SettingsSection("Playback") {
            SettingsToggleRow(
                icon = Icons.Filled.PictureInPicture,
                label = "Auto floating play",
                checked = autoFloatingPlay,
                onCheckedChange = {
                    autoFloatingPlay = it
                    settings.autoFloatingPlayEnabled = it
                }
            )
            SettingsToggleRow(
                icon = Icons.Filled.PlayArrow,
                label = "Auto-play next music video",
                checked = autoAdvance,
                onCheckedChange = {
                    autoAdvance = it
                    settings.autoAdvanceEnabled = it
                }
            )
            SettingsRow(
                icon = Icons.Filled.Speed,
                label = "Default playback speed: ${speedLabel(defaultSpeed)}",
                onClick = { showSpeedDialog = true }
            )
        }

        SettingsSection("Support") {
            SettingsRow(
                icon = Icons.Filled.Feedback,
                label = "Feedback",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, "RVH Video feedback")
                    }
                    safelyStart(context, intent)
                }
            )
            SettingsRow(
                icon = Icons.Filled.ThumbUp,
                label = "Rate us",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                    safelyStart(
                        context,
                        intent,
                        fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                    )
                }
            )
            SettingsRow(
                icon = Icons.Filled.Share,
                label = "Share RVH Video",
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out RVH Video — a local video player for shorts, movies, and music videos.")
                    }
                    safelyStart(context, Intent.createChooser(intent, "Share RVH Video"))
                }
            )
            SettingsRow(
                icon = Icons.Filled.Info,
                label = "About",
                onClick = { showAbout = true }
            )
        }

        Spacer(modifier = Modifier.padding(bottom = 32.dp))
    }


    if (showSpeedDialog) {
        RvhAlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Default playback speed") },
            text = {
                Column {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = {
                            defaultSpeed = speed
                            settings.defaultPlaybackSpeed = speed
                            showSpeedDialog = false
                        }) { Text(speedLabel(speed)) }
                    }
                }
            },
            confirmButton = { TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = { showSpeedDialog = false }) { Text("Close") } }
        )
    }

    if (showSortDialog) {
        RvhAlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Default library sort") },
            text = { Column { listOf("newest" to "Newest", "oldest" to "Oldest", "name" to "Name", "longest" to "Longest").forEach { (value, label) ->
                androidx.compose.material3.TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = { librarySort = value; settings.librarySortMode = value; showSortDialog = false }) { Text(label) }
            } } },
            confirmButton = { androidx.compose.material3.TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = { showSortDialog = false }) { Text("Done") } }
        )
    }
    if (showGridDialog) {
        RvhAlertDialog(
            onDismissRequest = { showGridDialog = false },
            title = { Text("Movie grid density") },
            text = { Column { (2..4).forEach { columns ->
                androidx.compose.material3.TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = { gridColumns = columns; settings.movieGridColumns = columns; showGridDialog = false }) { Text("$columns columns") }
            } } },
            confirmButton = { androidx.compose.material3.TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = { showGridDialog = false }) { Text("Done") } }
        )
    }

    if (showThemeDialog) {
        RvhAlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose theme") },
            text = {
                Column {
                    listOf(
                        AppSettings.THEME_SYSTEM to "System default",
                        AppSettings.THEME_DARK to "Dark",
                        AppSettings.THEME_LIGHT to "Light"
                    ).forEach { (value, label) ->
                        TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = {
                            themeMode = value
                            settings.themeMode = value
                            showThemeDialog = false
                            onThemeChanged()
                        }) { Text(label) }
                    }
                }
            },
            confirmButton = { TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = { showThemeDialog = false }) { Text("Close") } }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    if (showForceRescanConfirm) {
        RvhAlertDialog(
            onDismissRequest = { showForceRescanConfirm = false },
            title = { Text("Force full rescan?") },
            text = {
                Text(
                    "Re-checks every video's category from scratch instead of just new files — " +
                        "useful after a classification fix, but takes longer than a normal scan. " +
                        "Your manual \"Move to...\" choices and movie resume positions are kept."
                )
            },
            confirmButton = {
                TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = {
                    showForceRescanConfirm = false
                    scanning = true
                    scope.launch {
                        VideoScanner(context, RvhDatabase.get(context).localVideoDao()).scan(forceReclassify = true)
                        scanning = false
                    }
                }) { Text("Rescan") }
            },
            dismissButton = {
                TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = { showForceRescanConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    RvhAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = onDismiss) { Text("Close") } },
        title = { Text("RVH Video") },
        text = {
            Text("A local video player for shorts, movies, and music videos.\n\nVersion ${BuildConfig.VERSION_NAME}")
        }
    )
}

/**
 * Rate us needs a fallback: market:// only resolves on devices with the
 * Play Store installed (not guaranteed on every Android 11-16 device, and
 * never on an emulator without Play services). Falls back to the plain
 * https Play Store URL, which any browser can open instead.
 */
private fun safelyStart(context: android.content.Context, intent: Intent, fallback: Intent? = null) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        if (fallback != null) {
            try {
                context.startActivity(fallback)
            } catch (e2: ActivityNotFoundException) {
                // No handler for either — fail quietly, nothing actionable for the user here.
            }
        }
    }
}

private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

private fun themeLabel(mode: String): String = when (mode) {
    AppSettings.THEME_LIGHT -> "Light"
    AppSettings.THEME_DARK -> "Dark"
    else -> "System"
}

// TODO: replace with a real support address before shipping.
private const val FEEDBACK_EMAIL = "feedback@rvhvideo.app"

private fun sortLabel(value: String): String = when (value) {
    AppSettings.SORT_OLDEST -> "Oldest"
    AppSettings.SORT_NAME -> "Name"
    AppSettings.SORT_LONGEST -> "Longest"
    else -> "Newest"
}
