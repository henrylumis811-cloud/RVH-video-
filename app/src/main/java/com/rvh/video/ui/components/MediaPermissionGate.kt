package com.rvh.video.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.rvh.video.data.classification.VideoScanner
import com.rvh.video.data.local.RvhDatabase
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
import kotlinx.coroutines.launch

/**
 * Android 13+ (API 33+) uses the granular READ_MEDIA_VIDEO permission;
 * Android 11-12 (API 30-32) only has the broader READ_EXTERNAL_STORAGE.
 * Requesting the wrong one for the OS version either does nothing or
 * shows a permission Android doesn't expect, so this branches at request
 * time rather than declaring both and hoping the OS sorts it out.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission)

    when (permissionState.status) {
        is PermissionStatus.Granted -> {
            LaunchedEffect(Unit) {
                scope.launch {
                    VideoScanner(context, RvhDatabase.get(context).localVideoDao()).scan()
                }
            }
            content()
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize().background(Surface0),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "RVH Video needs access to your videos to build Shorts, Movies, and Music Videos.",
                        style = RvhType.Body,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                    Button(onClick = { permissionState.launchPermissionRequest() }) {
                        Text("Grant access")
                    }
                }
            }
        }
    }
}
