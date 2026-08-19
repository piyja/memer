package com.piyja.memer.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toImageBitmap
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Foundation.atomically
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIFont
import platform.UIKit UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIPasteboard
import platform.UIKit.drawString

@OptIn(ExperimentalForeignApi::class)
actual typealias PlatformBitmap = UIImage

actual fun loadTemplateBitmap(assetPath: String): PlatformBitmap {
    val fileName = assetPath.substringAfterLast('/').substringBeforeLast('.')
    return UIImage.imageNamed(fileName)
        ?: throw NullPointerException("Template image '$fileName' not found in bundle")
}

@OptIn(ExperimentalForeignApi::class)
actual fun renderMeme(
    bitmap: PlatformBitmap,
    topText: String,
    bottomText: String
): PlatformBitmap {
    val formattedTop = MemeText.formatMemeText(topText)
    val formattedBottom = MemeText.formatMemeText(bottomText)

    val width = bitmap.size.useContents { width }
    val height = bitmap.size.useContents { height }

    UIGraphicsBeginImageContextWithOptions(bitmap.size, false, 0.0)

    try {
        bitmap.drawInRect(CGRectMake(0.0, 0.0, width, height))

        if (formattedTop.isNotEmpty()) {
            val topSize = MemeText.calculateTextSize(width.toInt(), formattedTop) { text, size ->
                val font = UIFont.boldSystemFontOfSize(size.toDouble())
                NSString.create(string = text).sizeWithFont(font).width.toFloat()
            }
            drawMemeText(formattedTop, topSize, width, 0f)
        }

        if (formattedBottom.isNotEmpty()) {
            val bottomSize = MemeText.calculateTextSize(width.toInt(), formattedBottom) { text, size ->
                val font = UIFont.boldSystemFontOfSize(size.toDouble())
                NSString.create(string = text).sizeWithFont(font).width.toFloat()
            }
            drawMemeText(formattedBottom, bottomSize, width, height.toFloat() - bottomSize - 10f)
        }

        return UIGraphicsGetImageFromCurrentImageContext()
            ?: throw NullPointerException("Failed to render meme image")
    } finally {
        UIGraphicsEndImageContext()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun drawMemeText(text: String, fontSize: Float, imageWidth: Float, y: Float) {
    val font = UIFont.boldSystemFontOfSize(fontSize.toDouble())
    val nsText = NSString.create(string = text)
    val attrs = mapOf(
        platform.UIKit.NSForegroundColorAttributeName to platform.UIKit.UIColor.whiteColor,
        platform.UIKit.NSStrokeColorAttributeName to platform.UIKit.UIColor.blackColor,
        platform.UIKit.NSStrokeWidthAttributeName to -3.0,
        platform.UIKit.NSFontAttributeName to font
    )
    val textWidth = nsText.sizeWithAttributes(attrs).width
    val x = (imageWidth.toDouble() - textWidth) / 2.0
    nsText.drawAtPoint(platform.CoreGraphics.CGPointMake(x, y.toDouble()), withAttributes = attrs)
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformBitmapToImageBitmap(bitmap: PlatformBitmap): ImageBitmap {
    return bitmap.toImageBitmap()
}

actual fun saveMemeImage(bitmap: PlatformBitmap): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    )
    val documentsDir = paths.firstOrNull() as? String ?: throw IllegalStateException("No Documents dir")
    val memesDirPath = "$documentsDir/memes"

    val nsMemesDir = platform.Foundation.NSFileManager.defaultManager
    if (!nsMemesDir.fileExistsAtPath(memesDirPath)) {
        nsMemesDir.createDirectoryAtPath(memesDirPath, withIntermediateDirectories = true, attributes = null, null)
    }

    val fileName = MemeFileNaming.generateFileName(platform.Foundation.NSDate.date().timeIntervalSince1970().toLong() * 1000)
    val filePath = "$memesDirPath/$fileName"

    val data = UIImageJPEGRepresentation(bitmap, 0.9)
        ?: throw IllegalStateException("Failed to encode image")
    data.writeToFile(filePath, atomically = true)

    return filePath
}

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
