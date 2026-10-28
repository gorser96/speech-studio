@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package com.speech_studio.platform.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLBookmarkCreationSecurityScopeAllowOnlyReadAccess
import platform.Foundation.NSURLBookmarkCreationWithSecurityScope
import platform.Foundation.NSURLBookmarkResolutionWithSecurityScope
import platform.Foundation.NSURLBookmarkResolutionWithoutImplicitStartAccessing
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.darwin.NSObject

/**
 * iOS-адаптер выбора и просмотра видео.
 *
 * Автопоиск по медиатеке не входит в текущую версию, поэтому `deviceScan = false`.
 * Выбранные документы сохраняются как Base64 security-scoped bookmark, а не как
 * путь к файлу: ссылка продолжает открываться после перезапуска, пока доступ
 * не отозван. Файл не копируется и не удаляется.
 */
@Composable
actual fun rememberVideoEnvironment(): VideoEnvironment {
    val viewController = LocalUIViewController.current
    val picker = remember(viewController) { IosVideoPicker(viewController) }
    val access = remember(viewController) { IosVideoAccess(viewController) }
    return remember(picker, access) {
        VideoEnvironment(
            picker = picker,
            access = access,
            capabilities = VideoCapabilities(deviceScan = false),
            requestDeviceScanAccess = { onResult -> onResult(false) }
        )
    }
}

private class IosVideoPicker(
    private val host: UIViewController
) : VideoPicker {
    private var activePicker: UIDocumentPickerViewController? = null
    private var activeDelegate: UIDocumentPickerDelegateProtocol? = null

    override fun pickVideos(onResult: (VideoPickResult) -> Unit) {
        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                val urls = didPickDocumentsAtURLs.filterIsInstance<NSURL>()
                clear(controller)
                val bookmarks = mutableListOf<String>()
                var failed = 0
                urls.forEach { url ->
                    val bookmark = createBookmark(url)
                    if (bookmark != null) bookmarks.add(bookmark) else failed++
                }
                onResult(VideoPickResult(bookmarks, failed))
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                clear(controller)
                onResult(VideoPickResult(emptyList()))
            }
        }

        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeMovie),
            asCopy = false
        )
        picker.allowsMultipleSelection = true
        picker.delegate = delegate
        activePicker = picker
        activeDelegate = delegate
        host.topPresented().presentViewController(picker, animated = true, completion = null)
    }

    private fun clear(controller: UIDocumentPickerViewController) {
        if (activePicker === controller) {
            activePicker = null
            activeDelegate = null
        }
    }
}

private class IosVideoAccess(
    private val host: UIViewController
) : VideoAccess {
    private var activeController: UIDocumentInteractionController? = null
    private var activeDelegate: UIDocumentInteractionControllerDelegateProtocol? = null
    private var activeUrl: NSURL? = null
    private var accessStarted = false

    override suspend fun displayName(uri: String): String {
        val url = resolveBookmark(uri) ?: return "Видео"
        return url.lastPathComponent?.takeIf { it.isNotBlank() } ?: "Видео"
    }

    override suspend fun open(uri: String): VideoAccessError? {
        val url = resolveBookmark(uri) ?: return VideoAccessError.InvalidLink
        val path = url.path
        if (path == null || !NSFileManager.defaultManager.fileExistsAtPath(path)) {
            return VideoAccessError.Missing
        }
        if (!url.startAccessingSecurityScopedResource()) {
            return VideoAccessError.Permission
        }
        activeUrl = url
        accessStarted = true

        val controller = UIDocumentInteractionController.interactionControllerWithURL(url)
        val delegate = object : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
            override fun documentInteractionControllerViewControllerForPreview(
                controller: UIDocumentInteractionController
            ): UIViewController = host.topPresented()

            override fun documentInteractionControllerDidEndPreview(
                controller: UIDocumentInteractionController
            ) {
                finishAccess(controller)
            }

            override fun documentInteractionControllerDidDismissOpenInMenu(
                controller: UIDocumentInteractionController
            ) {
                finishAccess(controller)
            }
        }
        controller.delegate = delegate
        activeController = controller
        activeDelegate = delegate

        if (controller.presentPreviewAnimated(animated = true)) return null

        val view = host.topPresented().view
        if (controller.presentOpenInMenuFromRect(
                rect = CGRectMake(0.0, 0.0, 1.0, 1.0),
                inView = view,
                animated = true
            )
        ) {
            return null
        }

        finishAccess(controller)
        return VideoAccessError.NoViewer
    }

    private fun finishAccess(controller: UIDocumentInteractionController) {
        if (activeController !== controller) return
        if (accessStarted) activeUrl?.stopAccessingSecurityScopedResource()
        accessStarted = false
        activeUrl = null
        activeController = null
        activeDelegate = null
    }
}

private fun resolveBookmark(uri: String): NSURL? {
    val data = NSData.create(base64EncodedString = uri, options = 0uL) ?: return null
    return memScoped {
        val stale = alloc<BooleanVar>()
        val error = alloc<ObjCObjectVar<NSError?>>()
        NSURL.URLByResolvingBookmarkData(
            bookmarkData = data,
            options = NSURLBookmarkResolutionWithSecurityScope or
                NSURLBookmarkResolutionWithoutImplicitStartAccessing,
            relativeToURL = null,
            bookmarkDataIsStale = stale.ptr,
            error = error.ptr
        )
    }
}

private fun createBookmark(url: NSURL): String? {
    val started = url.startAccessingSecurityScopedResource()
    val data = url.bookmarkDataWithOptions(
        options = NSURLBookmarkCreationWithSecurityScope or
            NSURLBookmarkCreationSecurityScopeAllowOnlyReadAccess,
        includingResourceValuesForKeys = null,
        relativeToURL = null,
        error = null
    )
    if (started) url.stopAccessingSecurityScopedResource()
    return data?.base64EncodedStringWithOptions(0uL)
}

private fun UIViewController.topPresented(): UIViewController {
    var current = this
    while (true) {
        current = current.presentedViewController ?: return current
    }
}
