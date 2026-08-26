package com.piyja.memer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIImageJPEGRepresentation
import platform.UniformTypeIdentifiers.UTTypeGIF
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.Foundation.NSURL
import platform.darwin.NSObject

private object IOSGalleryState {
    var activeDelegate: GalleryDelegate? = null
}

private class GalleryDelegate : NSObject(), UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {

    var onResult: ((String?) -> Unit)? = null

    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        IOSGalleryState.activeDelegate = null
        onResult?.invoke(image?.let { persist(it) })
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        IOSGalleryState.activeDelegate = null
        onResult?.invoke(null)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun persist(image: UIImage): String? {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsDir = paths.firstOrNull() as? String ?: return null
        val templatesDir = "$documentsDir/user_templates"
        val fm = NSFileManager.defaultManager
        if (!fm.fileExistsAtPath(templatesDir)) {
            fm.createDirectoryAtPath(templatesDir, withIntermediateDirectories = true, attributes = null, null)
        }
        val millis = (NSDate().timeIntervalSince1970() * 1000.0).toLong()
        val filePath = "$templatesDir/gallery-$millis.jpg"
        val data = UIImageJPEGRepresentation(image, 0.9) ?: return null
        return if (data.writeToFile(filePath, atomically = true)) filePath else null
    }
}

@Composable
actual fun rememberGalleryImagePicker(): GalleryImagePicker {
    return remember {
        object : GalleryImagePicker {
            override fun launch(onResult: (String?) -> Unit) {
                val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
                if (rootVc == null) {
                    onResult(null)
                    return
                }
                val delegate = GalleryDelegate().apply { this.onResult = onResult }
                IOSGalleryState.activeDelegate = delegate

                val picker = UIImagePickerController()
                picker.delegate = delegate
                rootVc.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

private object IOSVideoGifState {
    var activeDelegate: VideoGifDelegate? = null
}

private class VideoGifDelegate : NSObject(), UIDocumentPickerDelegateProtocol {

    var onResult: ((String?) -> Unit)? = null

    override fun documentPicker(
        picker: UIDocumentPickerViewController,
        didPickDocumentAtURL: NSURL
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        IOSVideoGifState.activeDelegate = null
        val path = didPickDocumentAtURL.path
        onResult?.invoke(path)
    }

    override fun documentPickerWasCancelled(picker: UIDocumentPickerViewController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        IOSVideoGifState.activeDelegate = null
        onResult?.invoke(null)
    }
}

@Composable
actual fun rememberVideoGifPicker(): GalleryImagePicker {
    return remember {
        object : GalleryImagePicker {
            override fun launch(onResult: (String?) -> Unit) {
                val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
                if (rootVc == null) {
                    onResult(null)
                    return
                }
                val delegate = VideoGifDelegate().apply { this.onResult = onResult }
                IOSVideoGifState.activeDelegate = delegate

                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypeMovie, UTTypeGIF)
                )
                picker.delegate = delegate
                rootVc.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}
