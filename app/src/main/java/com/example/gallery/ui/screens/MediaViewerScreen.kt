package com.example.gallery.ui.screens

import android.content.Intent
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.example.gallery.ui.theme.Spacing
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

    LaunchedEffect(state.pendingDeleteIntentSender) {
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
                VideoPage(item = item, isActive = page == pagerState.currentPage, onTap = { showControls = !showControls })
            } else {
                ZoomableImage(item = item, onTap = { showControls = !showControls })
            }
        }

        // Top Gradient & AppBar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
            ) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }

        // Bottom Gradient & Actions
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(vertical = Spacing.Large, horizontal = Spacing.ExtraLarge)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalArrangement = Arrangement.SpaceEvenly
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
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete Media") },
            text = { Text("Are you sure you want to permanently delete \"${item.displayName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    itemAwaitingConfirm = null
                    viewModel.requestDelete(item)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { itemAwaitingConfirm = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(Spacing.Large)
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
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            }
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
private fun VideoPage(item: MediaItem, isActive: Boolean, onTap: () -> Unit) {
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

    LaunchedEffect(isActive) {
        if (isActive) exoPlayer.playWhenReady = true else exoPlayer.pause()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.id) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
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
        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
        title = { Text("Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
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
        },
        shape = RoundedCornerShape(Spacing.Large)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
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
