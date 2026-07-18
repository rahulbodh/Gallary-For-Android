package com.example.gallery.data

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,          // epoch seconds
    val dateTaken: Long,          // epoch millis, 0 if unknown
    val size: Long,               // bytes
    val width: Int,
    val height: Int,
    val mimeType: String,
    val type: MediaType,
    val duration: Long = 0L,      // ms, video only
    val bucketId: String,         // folder / album id
    val bucketName: String,       // folder / album display name
    val isScreenshot: Boolean = false,
    val relativePath: String = ""
)

data class Album(
    val bucketId: String,
    val bucketName: String,
    val coverUri: Uri,
    val itemCount: Int
)
