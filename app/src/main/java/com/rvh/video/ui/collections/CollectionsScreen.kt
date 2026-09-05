package com.rvh.video.ui.collections
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.rvh.video.data.model.LocalVideoEntity
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.RvhType
import com.rvh.video.ui.theme.Surface0
import com.rvh.video.ui.theme.Surface1
import com.rvh.video.ui.theme.TextSecondary

@Composable
fun CollectionsScreen(
    viewModel: CollectionsViewModel,
    onOpenMedia: (LocalVideoEntity) -> Unit,
) {
    val collections by viewModel.collections.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<com.rvh.video.data.model.VideoCollectionEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<com.rvh.video.data.model.VideoCollectionEntity?>(null) }

    val selected = collections.firstOrNull { it.id == selectedId }
    if (selected == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Collections", style = RvhType.ScreenTitle, color = Color.White)
                        Text("Build your own media shelves.", style = RvhType.Meta, color = TextSecondary)
                    }
                    IconButton(onClick = { showCreate = true }) { Icon(Icons.Filled.Add, "Create collection", tint = AccentTeal) }
                }
            }
            if (collections.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FolderCopy, null, tint = AccentTeal, modifier = Modifier.width(48.dp).height(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No collections yet", style = RvhType.CardTitle, color = Color.White)
                    Text("Create one, then fill it from your favorites.", style = RvhType.Meta, color = TextSecondary)
                }
            }
            items(collections, key = { it.id }) { collection ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).clickable { selectedId = collection.id }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FolderCopy, null, tint = AccentTeal)
                    Spacer(Modifier.width(14.dp))
                    Text(collection.name, style = RvhType.CardTitle, color = Color.White, modifier = Modifier.weight(1f))
                    IconButton(onClick = { renameTarget = collection }) { Icon(Icons.Filled.MoreVert, "Collection actions", tint = TextSecondary) }
                }
            }
        }
    } else {
        val items by viewModel.items(selected.id).collectAsState(initial = emptyList())
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedId = null }) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Text(selected.name, style = RvhType.ScreenTitle, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.addFavorites(selected.id) }) { Icon(Icons.Filled.Favorite, "Add favorites", tint = AccentTeal) }
                IconButton(onClick = { deleteTarget = selected }) { Icon(Icons.Filled.DeleteOutline, "Delete collection", tint = TextSecondary) }
            }
            Text("${items.size} items  •  Tap ♥ to add all current favorites", style = RvhType.Meta, color = TextSecondary, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))

            // Legendary Step 44: a unified command surface for the selected collection.
            val commandTarget = items.firstOrNull { it.resumePositionMs > 0L && it.durationMs > 0L && it.resumePositionMs < it.durationMs }
                ?: items.firstOrNull { it.isWatchLater }
                ?: items.firstOrNull { it.isFavorite }
                ?: items.firstOrNull()
            val completion = if (items.isEmpty()) 0 else ((items.count { it.durationMs > 0L && it.resumePositionMs >= it.durationMs } * 100) / items.size)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(Surface1).padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("COLLECTION COMMAND CENTER", style = RvhType.Meta, color = Color.White, modifier = Modifier.weight(1f))
                    Text(if (items.isEmpty()) "STANDBY" else "READY", style = RvhType.Meta, color = AccentTeal)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CommandCell("MEDIA", items.size.toString())
                    CommandCell("FAV", items.count { it.isFavorite }.toString())
                    CommandCell("LATER", items.count { it.isWatchLater }.toString())
                    CommandCell("DONE", "$completion%")
                }
                if (commandTarget != null) {
                    Text("NEXT RUN", style = RvhType.Meta, color = TextSecondary)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(commandTarget.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 1)
                            Text(
                                when {
                                    commandTarget.resumePositionMs > 0L -> "RESUME SESSION"
                                    commandTarget.isWatchLater -> "WATCH LATER"
                                    commandTarget.isFavorite -> "FAVORITE RUN"
                                    else -> "FRESH MEDIA"
                                },
                                style = RvhType.Meta, color = TextSecondary
                            )
                        }
                        TextButton(onClick = { onOpenMedia(commandTarget) }) { Text(if (commandTarget.resumePositionMs > 0L) "RESUME" else "LAUNCH") }
                    }
                } else {
                    Text("EMPTY SHELF • ADD MEDIA TO OPEN COMMANDS", style = RvhType.Meta, color = TextSecondary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.addFavorites(selected.id) }, modifier = Modifier.weight(1f)) { Text("ADD FAVORITES") }
                    if (commandTarget != null) {
                        TextButton(onClick = { onOpenMedia(commandTarget) }, modifier = Modifier.weight(1f)) { Text("PLAY NEXT") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Empty collection", style = RvhType.CardTitle, color = TextSecondary) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.uri }) { video ->
                        val itemIndex = items.indexOfFirst { it.uri == video.uri }
                        CollectionVideoRow(
                            video = video,
                            onOpen = { onOpenMedia(video) },
                            onMoveUp = { viewModel.move(selected.id, video.uri, -1) },
                            onMoveDown = { viewModel.move(selected.id, video.uri, 1) },
                            canMoveUp = itemIndex > 0,
                            canMoveDown = itemIndex < items.lastIndex,
                            onRemove = { viewModel.remove(selected.id, video.uri) }
                        )
                    }
                }
            }
        }
    }

    if (showCreate) NameDialog("New collection", "Create", onDismiss = { showCreate = false }) { viewModel.create(it); showCreate = false }
    renameTarget?.let { target -> NameDialog("Rename collection", "Save", initial = target.name, onDismiss = { renameTarget = null }) { viewModel.rename(target.id, it); renameTarget = null } }
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete ${target.name}?") }, text = { Text("The videos stay in your library. Only this collection is removed.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(target.id); deleteTarget = null; selectedId = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } })
    }
}

@Composable
private fun RowScope.CommandCell(label: String, value: String) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 7.dp)) {
        Text(label, style = RvhType.Meta, color = TextSecondary)
        Text(value, style = RvhType.CardTitle, color = Color.White)
    }
}

@Composable
private fun NameDialog(title: String, action: String, initial: String = "", onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value, { value = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { Button(enabled = value.isNotBlank(), onClick = { onConfirm(value.trim()) }) { Text(action) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun CollectionVideoRow(
    video: LocalVideoEntity,
    onOpen: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val request = remember(video.uri) { ImageRequest.Builder(context).data(video.uri).videoFrameMillis(1000).build() }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).clickable(onClick = onOpen).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(request, video.displayName, contentScale = ContentScale.Crop, modifier = Modifier.width(104.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(video.displayName.substringBeforeLast('.'), style = RvhType.CardTitle, color = Color.White, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = canMoveUp, onClick = onMoveUp) { Icon(Icons.Filled.KeyboardArrowUp, "Move up", tint = if (canMoveUp) Color.White else TextSecondary) }
                IconButton(enabled = canMoveDown, onClick = onMoveDown) { Icon(Icons.Filled.KeyboardArrowDown, "Move down", tint = if (canMoveDown) Color.White else TextSecondary) }
            }
        }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.DeleteOutline, "Remove", tint = TextSecondary) }
    }
}

@Composable
fun CollectionPickerDialog(
    video: LocalVideoEntity,
    viewModel: CollectionsViewModel,
    onDismiss: () -> Unit,
) {
    val collections by viewModel.collections.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    if (showCreate) {
        NameDialog(
            title = "New collection",
            action = "Create",
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                viewModel.create(name)
                showCreate = false
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to collection") },
        text = {
            Column {
                Text(video.displayName.substringBeforeLast('.'), style = RvhType.Meta, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                if (collections.isEmpty()) {
                    Text("No collections yet. Create one to save this video.", style = RvhType.Body)
                } else {
                    collections.forEach { collection ->
                        TextButton(
                            onClick = { viewModel.addVideo(collection.id, video.uri); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(collection.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showCreate = true }) { Text("+ New collection") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
