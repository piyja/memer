package com.piyja.memer.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIPasteboard
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}

@OptIn(ExperimentalForeignApi::class)
actual typealias PlatformBitmap = UIImage

actual fun loadTemplateBitmap(assetPath: String): PlatformBitmap {
    if (assetPath.startsWith("/")) {
        return UIImage.imageWithContentsOfFile(assetPath)
            ?: throw NullPointerException("Template image not found at $assetPath")
    }
    val fileName = assetPath.substringAfterLast('/').substringBeforeLast('.')
    return UIImage.imageNamed(fileName)
        ?: throw NullPointerException("Template image '$fileName' not found in bundle")
}

@OptIn(ExperimentalForeignApi::class)
actual fun renderMeme(
    bitmap: PlatformBitmap,
    texts: List<PositionedText>
): PlatformBitmap {
    val width = bitmap.size.useContents { width }
    val height = bitmap.size.useContents { height }

    UIGraphicsBeginImageContextWithOptions(bitmap.size, false, 0.0)

    try {
        bitmap.drawInRect(CGRectMake(0.0, 0.0, width, height))

        texts.filter { it.text.isNotBlank() }.forEach { positioned ->
            val formatted = MemeText.formatMemeText(positioned.text)
            if (formatted.isEmpty()) return@forEach

            val fontSize = MemeText.calculateTextSize(width.toInt(), formatted) { text, size ->
                val font = UIFont.boldSystemFontOfSize(size.toDouble())
                NSString.create(string = text)
                    .sizeWithAttributes(mapOf<Any?, Any>(platform.UIKit.NSFontAttributeName to font))
                    .useContents { width }
                    .toFloat()
            } * positioned.scale
            drawCenteredMemeText(
                text = formatted,
                fontSize = fontSize,
                centerX = positioned.xRatio * width,
                centerY = positioned.yRatio * height
            )
        }

        return UIGraphicsGetImageFromCurrentImageContext()
            ?: throw NullPointerException("Failed to render meme image")
    } finally {
        UIGraphicsEndImageContext()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun drawCenteredMemeText(text: String, fontSize: Float, centerX: Double, centerY: Double) {
    val font = UIFont.boldSystemFontOfSize(fontSize.toDouble())
    val nsText = NSString.create(string = text)

    val measureAttrs = mapOf<Any?, Any>(platform.UIKit.NSFontAttributeName to font)
    val textSize = nsText.sizeWithAttributes(measureAttrs).useContents { Pair(width, height) }

    val attrs = mapOf<Any?, Any>(
        platform.UIKit.NSForegroundColorAttributeName to platform.UIKit.UIColor.whiteColor,
        platform.UIKit.NSStrokeColorAttributeName to platform.UIKit.UIColor.blackColor,
        platform.UIKit.NSStrokeWidthAttributeName to -4.0,
        platform.UIKit.NSFontAttributeName to font
    )

    val x = centerX - (textSize.first / 2.0)
    val y = centerY - (textSize.second / 2.0)
    nsText.drawAtPoint(platform.CoreGraphics.CGPointMake(x, y), withAttributes = attrs)
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformBitmapToImageBitmap(bitmap: PlatformBitmap): ImageBitmap {
    val data = UIImageJPEGRepresentation(bitmap, 0.9)
        ?: throw IllegalStateException("Failed to encode image")
    return Image.makeFromEncoded(data.toByteArray()).toComposeImageBitmap()
}

@OptIn(ExperimentalForeignApi::class)
private fun persistJpeg(bitmap: PlatformBitmap, subdirectory: String): String? {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documentsDir = paths.firstOrNull() as? String ?: return null
    val targetDir = "$documentsDir/$subdirectory"

    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(targetDir)) {
        fm.createDirectoryAtPath(targetDir, withIntermediateDirectories = true, attributes = null, null)
    }

    val fileName = MemeFileNaming.generateFileName((NSDate().timeIntervalSince1970 * 1000.0).toLong())
    val filePath = "$targetDir/$fileName"

    val data = UIImageJPEGRepresentation(bitmap, 0.9) ?: return null
    return if (data.writeToFile(filePath, atomically = true)) filePath else null
}

actual fun saveMemeToGallery(bitmap: PlatformBitmap): String? =
    persistJpeg(bitmap, "memes")

actual fun stageShareableImage(bitmap: PlatformBitmap): String =
    persistJpeg(bitmap, "shared_memes")
        ?: throw IllegalStateException("Failed to stage image for sharing")

@OptIn(ExperimentalForeignApi::class)
actual fun shareMemeImage(filePath: String) {
    val url = platform.Foundation.NSURL.fileURLWithPath(filePath)
    val controller = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)

    val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVc?.presentViewController(controller, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun copyMemeToClipboard(filePath: String) {
    val image = UIImage.imageWithContentsOfFile(filePath)
        ?: throw IllegalStateException("Failed to load image at $filePath")
    UIPasteboard.generalPasteboard.setImage(image)
}
