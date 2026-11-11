package com.rvh.video.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.ImageDecoder
import android.os.Build
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Composable
fun ProfileScreen(
    onThemeChanged: () -> Unit = {},
    onAutoFloatingPlayChanged: (Boolean) -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onWallpaperChanged: (String?) -> Unit = {},
) {
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
    var wallpaperUri by remember { mutableStateOf(settings.wallpaperUri) }
    var pendingWallpaperUri by remember { mutableStateOf<Uri?>(null) }
    var wallpaperSaving by remember { mutableStateOf(false) }
    var wallpaperError by remember { mutableStateOf<String?>(null) }
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingWallpaperUri = uri
    }

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
                            try {
                                VideoScanner(context, RvhDatabase.get(context).localVideoDao()).scan()
                            } finally {
                                scanning = false
                            }
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
                icon = Icons.Filled.Wallpaper,
                label = if (wallpaperUri == null) "App wallpaper: Default" else "App wallpaper: Gallery image",
                onClick = { wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
            if (wallpaperUri != null) {
                SettingsRow(icon = Icons.Filled.Refresh, label = "Reset app wallpaper", onClick = {
                    deleteStoredWallpaper(context)
                    wallpaperUri = null
                    settings.wallpaperUri = null
                    onWallpaperChanged(null)
                })
            }
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
                    onAutoFloatingPlayChanged(it)
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
                label = "Support & Feedback",
                onClick = onOpenSupport
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


    pendingWallpaperUri?.let { candidate ->
        RvhAlertDialog(
            onDismissRequest = { if (!wallpaperSaving) pendingWallpaperUri = null },
            title = { Text(if (wallpaperSaving) "Saving wallpaper…" else "Preview app wallpaper") },
            text = {
                Column {
                    coil.compose.AsyncImage(
                        model = candidate,
                        contentDescription = "Selected wallpaper preview",
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                        contentScale = ContentScale.Crop
                    )
                    Text(if (wallpaperSaving) "Preparing an optimized copy for your device…" else "This image will appear behind the app interface. Player screens remain unchanged.", modifier = Modifier.padding(top = 12.dp))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !wallpaperSaving,
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal),
                    onClick = {
                        if (!wallpaperSaving) {
                            wallpaperSaving = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { copyWallpaperIntoPrivateStorage(context, candidate) }
                                }
                                result.onSuccess { storedFile ->
                                    val storedUri = Uri.fromFile(storedFile).toString()
                                    settings.wallpaperUri = storedUri
                                    wallpaperUri = storedUri
                                    onWallpaperChanged(storedUri)
                                    deleteStoredWallpaper(context, keep = storedFile)
                                    pendingWallpaperUri = null
                                    wallpaperError = null
                                }.onFailure {
                                    wallpaperError = when (it) {
                                        is IOException -> it.message ?: "RVH Video couldn't save that image. Please choose another image and try again."
                                        else -> "RVH Video couldn't save that image. Please choose another image and try again."
                                    }
                                }
                                wallpaperSaving = false
                            }
                        }
                    }
                ) { Text(if (wallpaperSaving) "Saving…" else "Apply") }
            },
            dismissButton = {
                TextButton(enabled = !wallpaperSaving, colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = { pendingWallpaperUri = null }) { Text("Cancel") }
            }
        )
    }

    wallpaperError?.let { message ->
        RvhAlertDialog(
            onDismissRequest = { wallpaperError = null },
            title = { Text("Couldn't save wallpaper") },
            text = { Text(message) },
            confirmButton = {
                TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = { wallpaperError = null }) {
                    Text("OK")
                }
            }
        )
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
                        try {
                            VideoScanner(context, RvhDatabase.get(context).localVideoDao()).scan(forceReclassify = true)
                        } finally {
                            scanning = false
                        }
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
 * Play Store installed (not guaranteed on every Android 8-17 device, and
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


private fun sortLabel(value: String): String = when (value) {
    AppSettings.SORT_OLDEST -> "Oldest"
    AppSettings.SORT_NAME -> "Name"
    AppSettings.SORT_LONGEST -> "Longest"
    else -> "Newest"
}


/**
 * Copies the selected gallery image into RVH Video's private files directory.
 *
 * Some gallery/photo-picker providers expose a URI whose stream cannot safely be
 * reopened several times. Stage it from one resolver stream first, then perform
 * bounds, EXIF and bitmap decoding against that app-owned temporary file.
 */
private fun copyWallpaperIntoPrivateStorage(context: Context, source: Uri): File {
    val maxInputBytes = 40L * 1024L * 1024L
    val stagingFile = File.createTempFile("wallpaper-source-", ".img", context.cacheDir)
    try {
        val input = context.contentResolver.openInputStream(source)
            ?: throw IOException("Android couldn't provide a readable stream for the selected image. Please choose it again.")
        input.use { stream ->
            FileOutputStream(stagingFile).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxInputBytes) {
                        throw IOException("That image is larger than the 40 MB wallpaper limit.")
                    }
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
        }
        if (stagingFile.length() == 0L) {
            throw IOException("The selected image is empty. Please choose another image.")
        }

        val wallpaperDir = File(context.filesDir, "wallpapers").apply {
            if (!exists() && !mkdirs()) throw IOException("Couldn't create wallpaper storage.")
        }
        val maxDimension = 2560
        // ImageDecoder supports modern gallery formats (including HEIF/AVIF) on
        // Android 9+, and applies embedded orientation while decoding.
        val orientation = if (Build.VERSION.SDK_INT < 28) runCatching {
            ExifInterface(stagingFile.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL) else ExifInterface.ORIENTATION_NORMAL

        val decoded = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(stagingFile)) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val scale = minOf(1f, maxDimension.toFloat() / maxOf(width, height))
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(stagingFile.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("The selected file isn't a supported image or couldn't be decoded.")
            }
            var sampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(stagingFile.absolutePath, options)
                ?: throw IOException("The selected image couldn't be decoded. Please choose another image.")
        }

        var outputBitmap: Bitmap? = decoded
        try {
            val orientationMatrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
                }
            }
            if (!orientationMatrix.isIdentity) {
                outputBitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, orientationMatrix, true)
                if (outputBitmap !== decoded) decoded.recycle()
            }
            val oriented = outputBitmap ?: throw IOException("That image couldn't be prepared.")
            val scale = minOf(1f, maxDimension.toFloat() / maxOf(oriented.width, oriented.height))
            if (scale < 1f) {
                val scaled = Bitmap.createScaledBitmap(oriented, (oriented.width * scale).toInt().coerceAtLeast(1), (oriented.height * scale).toInt().coerceAtLeast(1), true)
                if (scaled !== oriented) oriented.recycle()
                outputBitmap = scaled
            }
            val bitmapToWrite = outputBitmap ?: throw IOException("That image couldn't be prepared.")
            val preserveTransparency = bitmapToWrite.hasAlpha()
            val extension = if (preserveTransparency) "png" else "jpg"
            val format = if (preserveTransparency) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val destination = File(wallpaperDir, "custom-wallpaper-${System.currentTimeMillis()}.$extension")
            val temporary = File(wallpaperDir, "wallpaper-copy.tmp")
            try {
                FileOutputStream(temporary).use { output ->
                    if (!bitmapToWrite.compress(format, if (preserveTransparency) 100 else 88, output)) {
                        throw IOException("RVH Video couldn't optimize that image. Please choose another image.")
                    }
                    output.fd.sync()
                }
                if (!temporary.renameTo(destination)) {
                    temporary.copyTo(destination, overwrite = true)
                    temporary.delete()
                }
                return destination
            } catch (error: Throwable) {
                temporary.delete()
                throw error
            }
        } finally {
            outputBitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    } finally {
        stagingFile.delete()
    }
}

/** Removes obsolete app-owned wallpaper copies, retaining the active file when supplied. */
private fun deleteStoredWallpaper(context: Context, keep: File? = null) {
    val directory = File(context.filesDir, "wallpapers")
    directory.listFiles()?.forEach { file ->
        if (keep == null || file.absolutePath != keep.absolutePath) file.delete()
    }
    if (keep == null) directory.delete()
}
