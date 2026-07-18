package com.example.gallery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gallery.data.Album
import com.example.gallery.data.DeleteNeedsPermission
import com.example.gallery.data.MediaItem
import com.example.gallery.data.MediaRepository
import com.example.gallery.data.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GalleryTab { ALL, PHOTOS, VIDEOS, SCREENSHOTS }

data class GalleryUiState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val allMedia: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val selectedTab: GalleryTab = GalleryTab.ALL,
    val pendingDeleteIntentSender: android.content.IntentSender? = null,
    val itemPendingDelete: MediaItem? = null
) {
    val filteredMedia: List<MediaItem> get() = when (selectedTab) {
        GalleryTab.ALL -> allMedia
        GalleryTab.PHOTOS -> allMedia.filter { it.type == MediaType.IMAGE && !it.isScreenshot }
        GalleryTab.VIDEOS -> allMedia.filter { it.type == MediaType.VIDEO }
        GalleryTab.SCREENSHOTS -> allMedia.filter { it.isScreenshot }
    }
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
        refresh()
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(hasPermission = false, isLoading = false)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val media = repository.loadAllMedia()
            val albums = repository.loadAlbums()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                allMedia = media,
                albums = albums
            )
        }
    }

    fun selectTab(tab: GalleryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun requestDelete(item: MediaItem) {
        try {
            repository.deleteMedia(item)
            refresh()
        } catch (e: DeleteNeedsPermission) {
            _uiState.value = _uiState.value.copy(
                pendingDeleteIntentSender = e.intentSender,
                itemPendingDelete = item
            )
        }
    }

    fun clearPendingDelete(confirmed: Boolean) {
        _uiState.value = _uiState.value.copy(
            pendingDeleteIntentSender = null,
            itemPendingDelete = null
        )
        if (confirmed) refresh()
    }

    fun albumMedia(bucketId: String): List<MediaItem> =
        _uiState.value.allMedia.filter { it.bucketId == bucketId }
}
