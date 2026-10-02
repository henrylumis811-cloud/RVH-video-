package com.rvh.video.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.TextSecondary

private data class TrackOption(
    val type: Int,
    val group: Tracks.Group,
    val trackIndex: Int,
    val title: String,
    val detail: String,
    val selected: Boolean,
)

@Composable
fun TrackSelectionSheet(
    player: Player?,
    onDismiss: () -> Unit,
) {
    val current = player?.currentTracks
    val options = current?.groups?.flatMap { group ->
        if (group.type != C.TRACK_TYPE_AUDIO && group.type != C.TRACK_TYPE_TEXT) return@flatMap emptyList()
        (0 until group.length).mapNotNull { index ->
            if (!group.isTrackSupported(index)) return@mapNotNull null
            val format = group.getTrackFormat(index)
            TrackOption(
                type = group.type,
                group = group,
                trackIndex = index,
                title = trackTitle(format, group.type),
                detail = trackDetail(format, group.type),
                selected = group.isTrackSelected(index),
            )
        }
    }.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio & subtitles") },
        text = {
            if (options.isEmpty()) {
                Text("This video does not expose selectable audio or subtitle tracks.", color = TextSecondary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Text("AUDIO", style = RvhType.Meta, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                    items(options.filter { it.type == C.TRACK_TYPE_AUDIO }, key = { "a-${it.group.hashCode()}-${it.trackIndex}" }) { option ->
                        TrackRow(option, player, onDismiss)
                    }
                    item {
                        Text("SUBTITLES", style = RvhType.Meta, color = TextSecondary, modifier = Modifier.padding(top = 10.dp))
                    }
                    item {
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal),
                            onClick = {
                                setTrackTypeDisabled(player, C.TRACK_TYPE_TEXT)
                                onDismiss()
                            },
                        ) { Text("Off") }
                    }
                    items(options.filter { it.type == C.TRACK_TYPE_TEXT }, key = { "t-${it.group.hashCode()}-${it.trackIndex}" }) { option ->
                        TrackRow(option, player, onDismiss)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal), onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun TrackRow(option: TrackOption, player: Player?, onDismiss: () -> Unit) {
    TextButton(
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(contentColor = if (option.selected) AccentTeal else Color.White),
        onClick = {
            selectTrack(player, option)
            onDismiss()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(option.title, style = RvhType.CardTitle, modifier = Modifier.weight(1f))
                if (option.selected) Text("ACTIVE", style = RvhType.Meta, color = AccentTeal)
            }
            if (option.detail.isNotBlank()) Text(option.detail, style = RvhType.Meta, color = TextSecondary)
        }
    }
}

private fun selectTrack(player: Player?, option: TrackOption) {
    player ?: return
    if (!player.availableCommands.contains(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return
    val parameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(option.type, false)
        .clearOverridesOfType(option.type)
        .addOverride(TrackSelectionOverride(option.group.mediaTrackGroup, option.trackIndex))
        .build()
    player.setTrackSelectionParameters(parameters)
}

private fun setTrackTypeDisabled(player: Player?, type: Int) {
    player ?: return
    if (!player.availableCommands.contains(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return
    val parameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(type, true)
        .clearOverridesOfType(type)
        .build()
    player.setTrackSelectionParameters(parameters)
}

private fun trackTitle(format: Format, type: Int): String {
    return format.label?.takeIf { it.isNotBlank() }
        ?: format.language?.takeIf { it.isNotBlank() }?.let { language ->
            if (type == C.TRACK_TYPE_TEXT) language.uppercase() else language.uppercase()
        }
        ?: if (type == C.TRACK_TYPE_AUDIO) "Audio track" else "Subtitle track"
}

private fun trackDetail(format: Format, type: Int): String {
    val parts = buildList {
        format.language?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        format.sampleMimeType?.substringAfterLast('/')?.let { add(it.uppercase()) }
        if (type == C.TRACK_TYPE_AUDIO) format.channelCount.takeIf { it > 0 }?.let { add("${it}ch") }
        format.sampleRate.takeIf { it > 0 }?.let { add("${it / 1000}kHz") }
    }
    return parts.distinct().joinToString(" • ")
}
