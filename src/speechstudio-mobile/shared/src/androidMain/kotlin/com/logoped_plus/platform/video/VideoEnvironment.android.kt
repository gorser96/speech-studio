package com.logoped_plus.platform.video

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException

@Composable
actual fun rememberVideoEnvironment(): VideoEnvironment {
    val context = LocalContext.current
    var pickCallback by remember { mutableStateOf<((VideoPickResult) -> Unit)?>(null) }
    var scanCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val callback = pickCallback
        pickCallback = null
        if (callback == null) return@rememberLauncherForActivityResult
        val added = mutableListOf<String>()
        var failed = 0
        uris.distinct().forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                added.add(uri.toString())
            } catch (_: SecurityException) {
                failed++
            }
        }
        callback(VideoPickResult(added, failed))
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val callback = scanCallback
        scanCallback = null
        callback?.invoke(granted)
    }

    return remember(context, pickerLauncher, permissionLauncher) {
        VideoEnvironment(
            picker = object : VideoPicker {
                override fun pickVideos(onResult: (VideoPickResult) -> Unit) {
                    pickCallback = onResult
                    try {
                        pickerLauncher.launch(arrayOf("video/*"))
                    } catch (_: ActivityNotFoundException) {
                        pickCallback = null
                        onResult(VideoPickResult(emptyList(), pickerUnavailable = true))
                    }
                }
            },
            access = AndroidVideoAccess(context),
            capabilities = VideoCapabilities(deviceScan = true),
            requestDeviceScanAccess = { onResult ->
                if (context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
                    onResult(true)
                } else {
                    scanCallback = onResult
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                }
            }
        )
    }
}

private class AndroidVideoAccess(private val context: Context) : VideoAccess {

    override suspend fun displayName(uri: String): String = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.query(
                Uri.parse(uri),
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Видео"
    }

    override suspend fun open(uri: String): VideoAccessError? {
        val accessError = withContext(Dispatchers.IO) {
            try {
                val parsed = Uri.parse(uri)
                require(parsed.scheme == "content" || parsed.scheme == "file")
                // Close before ACTION_VIEW so the viewer owns the descriptor.
                context.contentResolver.openFileDescriptor(parsed, "r")?.use { }
                    ?: return@withContext VideoAccessError.Missing
                null
            } catch (_: FileNotFoundException) {
                VideoAccessError.Missing
            } catch (_: SecurityException) {
                VideoAccessError.Permission
            } catch (_: IllegalArgumentException) {
                VideoAccessError.InvalidLink
            } catch (_: IOException) {
                VideoAccessError.Unreadable
            }
        }
        if (accessError != null) return accessError
        return withContext(Dispatchers.Main) {
            try {
                val parsed = Uri.parse(uri)
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(parsed, "video/*")
                    clipData = ClipData.newRawUri("Видео", parsed)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
                null
            } catch (_: ActivityNotFoundException) {
                VideoAccessError.NoViewer
            } catch (_: SecurityException) {
                VideoAccessError.Permission
            } catch (_: IllegalArgumentException) {
                VideoAccessError.InvalidLink
            }
        }
    }
}
