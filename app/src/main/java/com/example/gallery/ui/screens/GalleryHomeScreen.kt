package com.example.gallery.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gallery.ui.components.MediaGrid
import com.example.gallery.ui.components.PermissionGate
import com.example.gallery.ui.theme.Spacing
import com.example.gallery.data.MediaType
import kotlinx.coroutines.launch
import com.example.gallery.viewmodel.GalleryTab
import com.example.gallery.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryHomeScreen(
    viewModel: GalleryViewModel,
    onOpenAlbums: () -> Unit,
    onOpenItem: (source: String, index: Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // UI selection state mapping
    var selectedItems by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedItems.isNotEmpty()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedItems.size} Selected", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { selectedItems = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Placeholder */ }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            } else {
                LargeTopAppBar(
                    title = { Text("Gallery", style = MaterialTheme.typography.displayLarge) },
                    actions = {
                        IconButton(onClick = { /* Placeholder */ }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onOpenAlbums) {
                            Icon(Icons.Filled.PhotoAlbum, contentDescription = "Albums")
                        }
                        IconButton(onClick = { /* Placeholder */ }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }
    ) { padding ->
        PermissionGate(
            onGranted = { viewModel.onPermissionGranted() },
            onDenied = { viewModel.onPermissionDenied() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val tabs = GalleryTab.values().toList()
                val pagerState = rememberPagerState(
                    initialPage = tabs.indexOf(state.selectedTab)
                ) { tabs.size }
                val coroutineScope = rememberCoroutineScope()

                // Pre-compute filtered lists when allMedia changes, not during pager scroll
                val mediaMap = remember(state.allMedia) {
                    mapOf(
                        GalleryTab.ALL to state.allMedia,
                        GalleryTab.PHOTOS to state.allMedia.filter { it.type == MediaType.IMAGE && !it.isScreenshot },
                        GalleryTab.VIDEOS to state.allMedia.filter { it.type == MediaType.VIDEO },
                        GalleryTab.SCREENSHOTS to state.allMedia.filter { it.isScreenshot }
                    )
                }

                // Sync pager changes to ViewModel safely
                LaunchedEffect(pagerState.currentPage) {
                    val currentTab = tabs[pagerState.currentPage]
                    if (state.selectedTab != currentTab) {
                        viewModel.selectTab(currentTab)
                        selectedItems = emptySet()
                    }
                }

                // Animated Segmented Tabs
                AnimatedVisibility(
                    visible = !isSelectionMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SegmentedTabs(
                        tabs = tabs,
                        selectedTab = tabs[pagerState.targetPage], // Visually updates instantly on swipe
                        onTabSelected = { tab ->
                            selectedItems = emptySet()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tabs.indexOf(tab))
                            }
                        },
                        modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small)
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) { page ->
                    val currentTab = tabs[page]
                    val pageMedia = mediaMap[currentTab] ?: emptyList()

                    if (state.isLoading) {
                        LoadingState()
                    } else if (pageMedia.isEmpty()) {
                        EmptyState()
                    } else {
                        MediaGrid(
                            items = pageMedia,
                            selectedItems = selectedItems,
                            isSelectionMode = isSelectionMode,
                            onItemClick = { index ->
                                onOpenItem(currentTab.name.lowercase(), index)
                            },
                            onItemLongClick = { index ->
                                selectedItems = if (selectedItems.contains(index)) {
                                    selectedItems - index
                                } else {
                                    selectedItems + index
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedTabs(
    tabs: List<GalleryTab>,
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "tab_background"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tab_content"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = Spacing.Small),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label(),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun LoadingState() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.ExtraSmall)
    ) {
        items(20) {
            Box(
                modifier = Modifier
                    .padding(Spacing.ExtraSmall)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(Spacing.Medium))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(modifier = Modifier.height(Spacing.Large))
        Text(
            text = "No Media Found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = "Your photos and videos will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun GalleryTab.label(): String = when (this) {
    GalleryTab.ALL -> "All"
    GalleryTab.PHOTOS -> "Photos"
    GalleryTab.VIDEOS -> "Videos"
    GalleryTab.SCREENSHOTS -> "Screenshots"
}
