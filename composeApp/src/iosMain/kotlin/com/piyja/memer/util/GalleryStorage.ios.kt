package com.piyja.memer.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
private fun galleryDirPath(): String? {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documentsDir = paths.firstOrNull() as? String ?: return null
    val dir = "$documentsDir/gallery"
    if (!NSFileManager.defaultManager.fileExistsAtPath(dir)) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            dir,
            withIntermediateDirectories = true,
            attributes = null,
            null
        )
    }
    return dir
}

@OptIn(ExperimentalForeignApi::class)
actual fun writeGalleryEntry(id: String, content: String) {
    val dir = galleryDirPath() ?: return
    NSString.create(string = content).writeToFile(
        "$dir/$id.txt",
        atomically = true,
        encoding = platform.Foundation.NSUTF8StringEncoding,
        error = null
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun readGalleryEntry(id: String): String? {
    val dir = galleryDirPath() ?: return null
    val data = NSData.create(contentsOfFile = "$dir/$id.txt") ?: return null
    return data.toByteArray().decodeToString()
}

@OptIn(ExperimentalForeignApi::class)
actual fun listGalleryEntryIds(): List<String> {
    val dir = galleryDirPath() ?: return emptyList()
    val contents = NSFileManager.defaultManager.contentsOfDirectoryAtPath(dir, error = null)
        ?: return emptyList()
    return (contents as List<*>).mapNotNull { name ->
        (name as? String)?.takeIf { it.endsWith(".txt") }?.removeSuffix(".txt")
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun deleteGalleryEntry(id: String) {
    val dir = galleryDirPath() ?: return
    NSFileManager.defaultManager.removeItemAtPath("$dir/$id.txt", null)
    NSFileManager.defaultManager.removeItemAtPath("$dir/$id.jpg", null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun saveRenderedMemeImage(bitmap: PlatformBitmap, id: String): String? {
    val dir = galleryDirPath() ?: return null
    val path = "$dir/$id.jpg"
    val data = UIImageJPEGRepresentation(bitmap, 0.9) ?: return null
    return if (data.writeToFile(path, atomically = true)) "$id.jpg" else null
}

@OptIn(ExperimentalForeignApi::class)
actual fun loadRenderedMemeImage(fileName: String): PlatformBitmap? {
    val dir = galleryDirPath() ?: return null
    return UIImage.imageWithContentsOfFile("$dir/$fileName")
}
