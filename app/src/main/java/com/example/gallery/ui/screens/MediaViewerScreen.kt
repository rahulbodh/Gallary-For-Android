package com.example.gallery.ui.screens

import android.content.Intent
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.gallery.data.MediaItem
import com.example.gallery.data.MediaType
import com.example.gallery.viewmodel.GalleryViewModel
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaViewerScreen(
    viewModel: GalleryViewModel,
    source: String,
    bucketId: String,
    startIndex: Int,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val mediaList: List<MediaItem> = remember(source, bucketId, state.allMedia) {
        when (source) {
            "photos" -> state.allMedia.filter { it.type == MediaType.IMAGE && !it.isScreenshot }
            "videos" -> state.allMedia.filter { it.type == MediaType.VIDEO }
            "screenshots" -> state.allMedia.filter { it.isScreenshot }
            "album" -> viewModel.albumMedia(bucketId)
            else -> state.allMedia
        }
    }

    if (mediaList.isEmpty()) {
        onBack()
        return
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, mediaList.size - 1)
    ) { mediaList.size }

    var showInfo by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.clearPendingDelete(confirmed = result.resultCode == android.app.Activity.RESULT_OK)
        if (result.resultCode == android.app.Activity.RESULT_OK) onBack()
    }

    androidx.compose.runtime.LaunchedEffect(state.pendingDeleteIntentSender) {
        state.pendingDeleteIntentSender?.let { sender ->
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    val currentItem = mediaList[pagerState.currentPage]
    var itemAwaitingConfirm by remember { mutableStateOf<MediaItem?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = mediaList[page]
            if (item.type == MediaType.VIDEO) {
                VideoPage(item = item, isActive = page == pagerState.currentPage)
            } else {
                ZoomableImage(item = item, onTap = { showControls = !showControls })
            }
        }

        if (showControls) {
            TopAppBar(
                title = { Text(currentItem.displayName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                modifier = Modifier.align(Alignment.TopCenter)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp, horizontal = 24.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { shareItem(context, currentItem) }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                }
                IconButton(onClick = { showInfo = true }) {
                    Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color.White)
                }
                IconButton(onClick = { itemAwaitingConfirm = currentItem }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }
    }

    itemAwaitingConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { itemAwaitingConfirm = null },
            title = { Text("Delete item?") },
            text = { Text("This will permanently delete \"${item.displayName}\".") },
            confirmButton = {
                TextButton(onClick = {
                    itemAwaitingConfirm = null
                    viewModel.requestDelete(item)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { itemAwaitingConfirm = null }) { Text("Cancel") }
            }
        )
    }

    if (showInfo) {
        MediaInfoDialog(item = currentItem, onDismiss = { showInfo = false })
    }
}

@Composable
private fun ZoomableImage(item: MediaItem, onTap: () -> Unit) {
    var scale by remember(item.id) { mutableStateOf(1f) }
    var offsetX by remember(item.id) { mutableStateOf(0f) }
    var offsetY by remember(item.id) { mutableStateOf(0f) }

    AsyncImage(
        model = item.uri,
        contentDescription = item.displayName,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = max(1f, min(scale * zoom, 5f))
                    offsetX = if (scale == 1f) 0f else offsetX + pan.x
                    offsetY = if (scale == 1f) 0f else offsetY + pan.y
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
    )
}

@Composable
private fun VideoPage(item: MediaItem, isActive: Boolean) {
    val context = LocalContext.current
    val exoPlayer = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            prepare()
        }
    }

    DisposableEffect(item.id) {
        onDispose { exoPlayer.release() }
    }

    androidx.compose.runtime.LaunchedEffect(isActive) {
        if (isActive) exoPlayer.playWhenReady = true else exoPlayer.pause()
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MediaInfoDialog(item: MediaItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val dateText = remember(item.id) {
        val millis = if (item.dateTaken > 0) item.dateTaken else item.dateAdded * 1000
        DateFormat.format("MMM d, yyyy h:mm a", millis).toString()
    }
    val sizeText = remember(item.id) { Formatter.formatShortFileSize(context, item.size) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Details") },
        text = {
            Column {
                InfoRow("Name", item.displayName)
                InfoRow("Date", dateText)
                InfoRow("Size", sizeText)
                InfoRow("Resolution", "${item.width} x ${item.height}")
                InfoRow("Folder", item.bucketName)
                if (item.type == MediaType.VIDEO) {
                    InfoRow("Duration", com.example.gallery.ui.components.formatDuration(item.duration))
                }
                InfoRow("Type", item.mimeType)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = "$label: ", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text(text = value)
    }
}

private fun shareItem(context: android.content.Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType.ifBlank { if (item.type == MediaType.VIDEO) "video/*" else "image/*" }
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
