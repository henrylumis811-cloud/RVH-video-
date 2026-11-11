package com.rvh.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface2
import com.rvh.video.ui.theme.TextSecondary
import androidx.compose.foundation.clickable

enum class RvhTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Shorts("Shorts", Icons.Filled.PlayCircle),
    Movies("Movies", Icons.Filled.MovieFilter),
    Music("Music", Icons.Filled.MusicNote),
    Profile("Profile", Icons.Filled.Person),
}

@Composable
fun RvhBottomNav(
    current: RvhTab,
    onSelect: (RvhTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2)
            .navigationBarsPadding() // edge-to-edge means gesture-nav devices could otherwise clip the labels under the system nav bar
            .padding(vertical = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        RvhTab.entries.forEach { tab ->
            val selected = tab == current
            val tint by animateColorAsState(
                targetValue = if (selected) AccentTeal else TextSecondary,
                label = "navTint"
            )
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.10f else 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
                label = "navScale"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                    .clickable { onSelect(tab) }
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = tab.label,
                    style = RvhType.Meta,
                    color = tint,
                )
            }
        }
    }
}
