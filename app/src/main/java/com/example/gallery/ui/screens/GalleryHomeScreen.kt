package com.example.gallery.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gallery.ui.components.MediaGrid
import com.example.gallery.ui.components.PermissionGate
import com.example.gallery.viewmodel.GalleryTab
import com.example.gallery.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryHomeScreen(
    viewModel: GalleryViewModel,
    onOpenAlbums: () -> Unit,
    onOpenItem: (source: String, index: Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gallery") },
                actions = {
                    IconButton(onClick = onOpenAlbums) {
                        Icon(Icons.Filled.PhotoAlbum, contentDescription = "Albums")
                    }
                }
            )
        }
    ) { padding ->
        PermissionGate(
            onGranted = { viewModel.onPermissionGranted() },
            onDenied = { viewModel.onPermissionDenied() }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                val tabs = GalleryTab.values()
                TabRow(selectedTabIndex = tabs.indexOf(state.selectedTab)) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = { Text(tab.label()) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (state.filteredMedia.isEmpty()) {
                        Text(
                            text = "No media found",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        MediaGrid(
                            items = state.filteredMedia,
                            onItemClick = { index ->
                                onOpenItem(state.selectedTab.name.lowercase(), index)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun GalleryTab.label(): String = when (this) {
    GalleryTab.ALL -> "All"
    GalleryTab.PHOTOS -> "Photos"
    GalleryTab.VIDEOS -> "Videos"
    GalleryTab.SCREENSHOTS -> "Screenshots"
}
