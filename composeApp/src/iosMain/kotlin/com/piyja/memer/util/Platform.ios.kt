package com.piyja.memer.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
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
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIPasteboard
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes
import platform.posix.memcpy
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.NSURL
import kotlinx.cinterop.BetaInteropApi

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
                centerY = positioned.yRatio * height,
                color = positioned.color,
                bold = positioned.bold,
                strike = positioned.strike
            )
        }

        return UIGraphicsGetImageFromCurrentImageContext()
            ?: throw NullPointerException("Failed to render meme image")
    } finally {
        UIGraphicsEndImageContext()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun argbToUIColor(argb: Long): UIColor {
    val a = ((argb shr 24) and 0xFF) / 255.0
    val r = ((argb shr 16) and 0xFF) / 255.0
    val g = ((argb shr 8) and 0xFF) / 255.0
    val b = (argb and 0xFF) / 255.0
    return UIColor(red = r, green = g, blue = b, alpha = a)
}

@OptIn(ExperimentalForeignApi::class)
private fun drawCenteredMemeText(
    text: String,
    fontSize: Float,
    centerX: Double,
    centerY: Double,
    color: Long,
    bold: Boolean,
    strike: Boolean
) {
    val font = if (bold) {
        UIFont.boldSystemFontOfSize(fontSize.toDouble())
    } else {
        UIFont.systemFontOfSize(fontSize.toDouble())
    }
    val nsText = NSString.create(string = text)

    val measureAttrs = mapOf<Any?, Any>(platform.UIKit.NSFontAttributeName to font)
    val textSize = nsText.sizeWithAttributes(measureAttrs).useContents { Pair(width, height) }

    val attrs = mapOf<Any?, Any>(
        platform.UIKit.NSForegroundColorAttributeName to argbToUIColor(color),
        platform.UIKit.NSStrokeColorAttributeName to platform.UIKit.UIColor.blackColor,
        platform.UIKit.NSStrokeWidthAttributeName to -4.0,
        platform.UIKit.NSFontAttributeName to font
    )

    val x = centerX - (textSize.first / 2.0)
    val y = centerY - (textSize.second / 2.0)
    nsText.drawAtPoint(platform.CoreGraphics.CGPointMake(x, y), withAttributes = attrs)

    if (strike) {
        val ctx = UIGraphicsGetCurrentContext()
        if (ctx != null) {
            platform.CoreGraphics.CGContextSetStrokeColorWithColor(ctx, argbToUIColor(color).CGColor)
            platform.CoreGraphics.CGContextSetLineWidth(ctx, (fontSize * 0.08f).toDouble())
            platform.CoreGraphics.CGContextMoveToPoint(ctx, centerX - textSize.first / 2.0, centerY)
            platform.CoreGraphics.CGContextAddLineToPoint(ctx, centerX + textSize.first / 2.0, centerY)
            platform.CoreGraphics.CGContextStrokePath(ctx)
        }
    }
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

@OptIn(ExperimentalForeignApi::class)
private fun writeBytesToFile(bytes: ByteArray, filePath: String): Boolean {
    val file = fopen(filePath, "wb") ?: return false
    val written = fwrite(bytes.refTo(0), 1u, bytes.size.toULong(), file)
    fclose(file)
    return written.toLong() == bytes.size.toLong()
}

@OptIn(ExperimentalForeignApi::class)
actual fun bitmapToRgba(bitmap: PlatformBitmap): RgbaImage {
    val cgImage = bitmap.CGImage ?: throw IllegalStateException("Failed to get CGImage")
    val width = CGImageGetWidth(cgImage).toInt()
    val height = CGImageGetHeight(cgImage).toInt()
    val bytesPerPixel = 4
    val bytesPerRow = width * bytesPerPixel

    val context = CGBitmapContextCreate(
        null,
        width.toULong(),
        height.toULong(),
        8.toULong(),
        bytesPerRow.toULong(),
        CGColorSpaceCreateDeviceRGB(),
        1u // kCGImageAlphaPremultipliedLast
    ) ?: throw IllegalStateException("Failed to create bitmap context")

    CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), cgImage)

    val source = CGBitmapContextGetData(context)
        ?: throw IllegalStateException("Failed to read bitmap data")
    val buffer = ByteArray(width * height * bytesPerPixel)
    buffer.usePinned { pinned ->
        memcpy(pinned.addressOf(0), source, (width * height * bytesPerPixel).toULong())
    }
    return RgbaImage(width, height, buffer)
}

@OptIn(ExperimentalForeignApi::class)
private fun writeGifBytes(bytes: ByteArray, subdirectory: String): String? {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documentsDir = paths.firstOrNull() as? String ?: return null
    val targetDir = "$documentsDir/$subdirectory"

    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(targetDir)) {
        fm.createDirectoryAtPath(targetDir, withIntermediateDirectories = true, attributes = null, null)
    }

    val fileName = MemeFileNaming.generateFileName((NSDate().timeIntervalSince1970 * 1000.0).toLong())
    val filePath = "$targetDir/$fileName.gif"
    return if (writeBytesToFile(bytes, filePath)) filePath else null
}

@OptIn(ExperimentalForeignApi::class)
actual fun saveGifToAppStorage(bytes: ByteArray, id: String): String? {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documentsDir = paths.firstOrNull() as? String ?: return null
    val targetDir = "$documentsDir/gallery"
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(targetDir)) {
        fm.createDirectoryAtPath(targetDir, withIntermediateDirectories = true, attributes = null, null)
    }
    val filePath = "$targetDir/$id.gif"
    return if (writeBytesToFile(bytes, filePath)) filePath.substringAfterLast('/') else null
}

actual fun loadGifFromAppStorage(fileName: String): ByteArray? {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documentsDir = paths.firstOrNull() as? String ?: return null
    val filePath = "$documentsDir/gallery/$fileName"
    return NSFileManager.defaultManager.contentsAtPath(filePath)?.toByteArray()
}

actual fun stageShareableGif(bytes: ByteArray): String {
    return writeGifBytes(bytes, "shared_memes")
        ?: throw IllegalStateException("Failed to stage GIF for sharing")
}

actual fun shareGifFile(filePath: String) {
    val url = platform.Foundation.NSURL.fileURLWithPath(filePath)
    val controller = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
    val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVc?.presentViewController(controller, animated = true, completion = null)
}

actual fun copyGifToClipboard(filePath: String) {
    val data = NSFileManager.defaultManager.contentsAtPath(filePath) ?: return
    UIPasteboard.generalPasteboard.setData(data, "com.compuserve.gif")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun getMediaInfo(path: String): MediaInfo {
    val isGif = path.endsWith(".gif", ignoreCase = true)
    return if (isGif) {
        MediaInfo(1000L, true)
    } else {
        val url = NSURL.fileURLWithPath(path) ?: return MediaInfo(0, false)
        val asset = AVURLAsset(url, null)
        val seconds = CMTimeGetSeconds(asset.duration)
        MediaInfo((seconds * 1000.0).toLong(), false)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun extractFrames(path: String, startMs: Long, endMs: Long, fps: Int): List<MediaFrame> {
    val isGif = path.endsWith(".gif", ignoreCase = true)
    val effectiveFps = fps.coerceAtLeast(1)
    val frameDur = 1000L / effectiveFps
    val out = mutableListOf<MediaFrame>()

    if (isGif) {
        val img = UIImage.imageWithContentsOfFile(path) ?: return emptyList()
        out.add(MediaFrame(img, 1000L))
        return out
    }

    return emptyList()
}

@OptIn(ExperimentalForeignApi::class)
actual fun loadBundledGifNames(): List<String> = emptyList()

actual fun loadBundledGifBytes(name: String): ByteArray? = null

actual fun decodeGifFrames(bytes: ByteArray): List<MediaFrame> = emptyList()

actual fun copyBundledGifToTempFile(name: String): String? = null
