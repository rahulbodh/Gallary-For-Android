package com.example.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.gallery.data.MediaItem
import com.example.gallery.data.MediaType
import java.util.concurrent.TimeUnit

@Composable
fun MediaGrid(
    items: List<MediaItem>,
    modifier: Modifier = Modifier,
    onItemClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp)
    ) {
        items(items.size) { index ->
            MediaThumbnail(item = items[index], onClick = { onItemClick(index) })
        }
    }
}

@Composable
fun MediaThumbnail(item: MediaItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
    ) {
        val request = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(item.uri)
            .apply { if (item.type == MediaType.VIDEO) videoFrameMillis(1000) }
            .crossfade(true)
            .build()

        AsyncImage(
            model = request,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.type == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(4.dp)
            )
            Text(
                text = formatDuration(item.duration),
                color = Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
