package com.piyja.memer.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.piyja.memer.util.AndroidContextHolder.appContext
import java.io.File
import java.io.FileOutputStream

actual typealias PlatformBitmap = Bitmap

actual fun loadTemplateBitmap(assetPath: String): PlatformBitmap {
    val input = appContext.assets.open(assetPath)
    return BitmapFactory.decodeStream(input)
}

actual fun renderMeme(
    bitmap: PlatformBitmap,
    topText: String,
    bottomText: String
): PlatformBitmap {
    val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutable)

    val fillPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    val strokePaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    val formattedTop = MemeText.formatMemeText(topText)
    val formattedBottom = MemeText.formatMemeText(bottomText)

    if (formattedTop.isNotEmpty()) {
        val topSize = MemeText.calculateTextSize(mutable.width, formattedTop) { text, size ->
            Paint().apply { textSize = size }.measureText(text)
        }
        fillPaint.textSize = topSize
        strokePaint.textSize = topSize
        val topY = topSize + 10f
        canvas.drawText(formattedTop, mutable.width / 2f, topY, strokePaint)
        canvas.drawText(formattedTop, mutable.width / 2f, topY, fillPaint)
    }

    if (formattedBottom.isNotEmpty()) {
        val bottomSize = MemeText.calculateTextSize(mutable.width, formattedBottom) { text, size ->
            Paint().apply { textSize = size }.measureText(text)
        }
        fillPaint.textSize = bottomSize
        strokePaint.textSize = bottomSize
        val bottomY = mutable.height - 15f
        canvas.drawText(formattedBottom, mutable.width / 2f, bottomY, strokePaint)
        canvas.drawText(formattedBottom, mutable.width / 2f, bottomY, fillPaint)
    }

    return mutable
}

actual fun platformBitmapToImageBitmap(bitmap: PlatformBitmap): ImageBitmap {
    return bitmap.asImageBitmap()
}

private fun getMemesDir(): File {
    val parent = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val dir = File(parent, "memes")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

actual fun saveMemeImage(bitmap: PlatformBitmap): String {
    val dir = getMemesDir()
    val file = File(dir, MemeFileNaming.generateFileName(System.currentTimeMillis()))

    val output = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
    output.flush()
    output.close()

    return file.absolutePath
}

actual fun shareMemeImage(filePath: String) {
    val file = File(filePath)
    val authority = appContext.packageName + ".fileprovider"
    val uri = FileProvider.getUriForFile(appContext, authority, file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        type = "image/jpeg"
    }

    appContext.startActivity(Intent.createChooser(intent, "Share meme via"))
}

actual fun copyMemeToClipboard(filePath: String) {
    val file = File(filePath)
    val authority = appContext.packageName + ".fileprovider"
    val uri = FileProvider.getUriForFile(appContext, authority, file)

    val clipData = ClipData.newUri(appContext.contentResolver, "Meme Image", uri)
    val clipboard = appContext.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(clipData)
}
