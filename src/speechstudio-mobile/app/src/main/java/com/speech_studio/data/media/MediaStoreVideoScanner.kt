@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.speech_studio.data.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.speech_studio.domain.model.DeviceVideo
import com.speech_studio.domain.repository.VideoLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class MediaStoreVideoScanner(private val context: Context) : VideoLibrary {

    override suspend fun findVideos(from: Instant, to: Instant): List<DeviceVideo> = withContext(Dispatchers.IO) {
        val fromMillis = from.toEpochMilliseconds()
        val toMillis = to.toEpochMilliseconds()
        val found = mutableListOf<DeviceVideo>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DATE_ADDED
            ),
            null, null, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val takenIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
            val addedIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val taken = if (takenIndex >= 0) cursor.getLong(takenIndex) else 0L
                val added = if (addedIndex >= 0) cursor.getLong(addedIndex) else 0L
                val createdMillis = mediaStoreTimeToMillis(if (taken > 0) taken else added)
                if (createdMillis >= fromMillis && createdMillis < toMillis) {
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idIndex)).toString()
                    val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    found += DeviceVideo(uri, displayName, createdMillis)
                }
            }
        }
        found
    }
}

// MediaStore contract says seconds, but some ROMs (e.g. OnePlus OxygenOS) store
// DATE_TAKEN in milliseconds. Values >= 1e11 are necessarily milliseconds
// (milliseconds before 1973 are smaller); seconds only reach 1e10 after 2286.
private const val MEDIA_STORE_MILLIS_THRESHOLD = 100_000_000_000L

internal fun mediaStoreTimeToMillis(value: Long): Long =
    if (value >= MEDIA_STORE_MILLIS_THRESHOLD) value else value * 1000
