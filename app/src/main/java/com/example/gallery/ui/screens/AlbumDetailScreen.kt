package com.example.gallery.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.gallery.ui.components.MediaGrid
import com.example.gallery.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: GalleryViewModel,
    bucketId: String,
    onBack: () -> Unit,
    onOpenItem: (Int) -> Unit
) {
    val media = remember(bucketId) { viewModel.albumMedia(bucketId) }
    val title = media.firstOrNull()?.bucketName ?: "Album"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        MediaGrid(
            items = media,
            modifier = Modifier.padding(padding).fillMaxSize(),
            onItemClick = { index -> onOpenItem(index) }
        )
    }
}
