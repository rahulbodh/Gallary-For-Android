package com.example.gallery.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thrown when a delete needs user confirmation on Android 10+ (scoped storage).
 * Contains an IntentSender the UI layer should launch to obtain permission.
 */
class DeleteNeedsPermission(val intentSender: android.content.IntentSender) : Exception()

class MediaRepository(private val context: Context) {

    /** Loads every image + video the app can see on the device, newest first. */
    suspend fun loadAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = queryMedia(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            type = MediaType.IMAGE
        )
        val videos = queryMedia(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            type = MediaType.VIDEO
        )
        (images + videos).sortedByDescending { if (it.dateTaken > 0) it.dateTaken else it.dateAdded * 1000 }
    }

    suspend fun loadAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val all = loadAllMedia()
        all.groupBy { it.bucketId }
            .map { (bucketId, items) ->
                Album(
                    bucketId = bucketId,
                    bucketName = items.first().bucketName,
                    coverUri = items.first().uri,
                    itemCount = items.size
                )
            }
            .sortedByDescending { it.itemCount }
    }

    private fun queryMedia(collection: Uri, type: MediaType): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        if (type == MediaType.VIDEO) {
            projection.add(MediaStore.Video.VideoColumns.DURATION)
        }

        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val relPathCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val durationCol = if (type == MediaType.VIDEO)
                cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val name = cursor.getString(nameCol) ?: ""
                val bucketName = cursor.getString(bucketNameCol) ?: ""
                val relPath = cursor.getString(relPathCol) ?: ""
                val isScreenshot = bucketName.contains("screenshot", ignoreCase = true) ||
                    relPath.contains("screenshot", ignoreCase = true) ||
                    name.contains("screenshot", ignoreCase = true)

                items.add(
                    MediaItem(
                        id = id,
                        uri = uri,
                        displayName = name,
                        dateAdded = cursor.getLong(dateAddedCol),
                        dateTaken = cursor.getLong(dateTakenCol),
                        size = cursor.getLong(sizeCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        mimeType = cursor.getString(mimeCol) ?: "",
                        type = type,
                        duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                        bucketId = cursor.getString(bucketIdCol) ?: "unknown",
                        bucketName = bucketName.ifBlank { "Unknown" },
                        isScreenshot = isScreenshot,
                        relativePath = relPath
                    )
                )
            }
        }
        return items
    }

    /**
     * Attempts to delete a media file. On Android 10+ this may fail with a
     * DeleteNeedsPermission that the UI must resolve via a system dialog.
     */
    fun deleteMedia(item: MediaItem) {
        try {
            context.contentResolver.delete(item.uri, null, null)
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                throw DeleteNeedsPermission(e.userAction.actionIntent.intentSender)
            } else {
                throw e
            }
        }
    }
}
