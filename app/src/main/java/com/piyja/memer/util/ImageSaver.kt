package com.piyja.memer.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object ImageSaver {

    private const val MEME_DIR_NAME = "memes"

    fun generateFileName(timestamp: Long): String {
        val formatted = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(timestamp)
        return "meme_$formatted.jpg"
    }

    fun getMemesDir(context: Context): File {
        val parent = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val dir = File(parent, MEME_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveToInternalStorage(context: Context, bitmap: Bitmap): File {
        val dir = getMemesDir(context)
        val file = File(dir, generateFileName(System.currentTimeMillis()))

        val output = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        output.flush()
        output.close()

        return file
    }

    fun listSavedMemes(context: Context): List<File> {
        val dir = getMemesDir(context)
        return dir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".jpg", true) || it.name.endsWith(".png", true)) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}