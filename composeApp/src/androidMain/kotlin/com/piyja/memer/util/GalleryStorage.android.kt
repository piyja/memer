package com.piyja.memer.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.piyja.memer.util.AndroidContextHolder.appContext
import java.io.File
import java.io.FileOutputStream

private fun galleryDir(): File =
    File(appContext.filesDir, "gallery").apply { mkdirs() }

actual fun writeGalleryEntry(id: String, content: String) {
    File(galleryDir(), "$id.txt").writeText(content)
}

actual fun readGalleryEntry(id: String): String? {
    val file = File(galleryDir(), "$id.txt")
    return if (file.exists()) file.readText() else null
}

actual fun listGalleryEntryIds(): List<String> {
    return galleryDir().listFiles { file -> file.name.endsWith(".txt") }
        ?.map { it.name.removeSuffix(".txt") }
        ?: emptyList()
}

actual fun deleteGalleryEntry(id: String) {
    File(galleryDir(), "$id.txt").delete()
    File(galleryDir(), "$id.jpg").delete()
}

actual fun saveRenderedMemeImage(bitmap: PlatformBitmap, id: String): String? {
    val file = File(galleryDir(), "$id.jpg")
    return try {
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        "$id.jpg"
    } catch (e: Exception) {
        null
    }
}

actual fun loadRenderedMemeImage(fileName: String): PlatformBitmap? {
    val file = File(galleryDir(), fileName)
    if (!file.exists()) return null
    return BitmapFactory.decodeFile(file.absolutePath)
}
