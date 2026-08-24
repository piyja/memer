package com.piyja.memer.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.piyja.memer.util.AndroidContextHolder.appContext
import java.io.File
import java.io.FileOutputStream

actual typealias PlatformBitmap = Bitmap

actual fun loadTemplateBitmap(assetPath: String): PlatformBitmap {
    if (assetPath.startsWith("/")) {
        return BitmapFactory.decodeFile(assetPath)
            ?: throw IllegalArgumentException("Failed to decode image at $assetPath")
    }
    val input = appContext.assets.open(assetPath)
    return BitmapFactory.decodeStream(input)
}

actual fun renderMeme(
    bitmap: PlatformBitmap,
    texts: List<PositionedText>
): PlatformBitmap {
    val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutable)

    texts.filter { it.text.isNotBlank() }.forEach { positioned ->
        val formatted = MemeText.formatMemeText(positioned.text)
        if (formatted.isEmpty()) return@forEach

        val textSize = MemeText.calculateTextSize(mutable.width, formatted) { text, size ->
            Paint().apply { textSize = size }.measureText(text)
        }

        val fillPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            this.textSize = textSize
        }
        val strokePaint = Paint(fillPaint).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val cx = positioned.xRatio * mutable.width
        val baselineY = positioned.yRatio * mutable.height + textSize * 0.35f
        canvas.drawText(formatted, cx, baselineY, strokePaint)
        canvas.drawText(formatted, cx, baselineY, fillPaint)
    }

    return mutable
}

actual fun platformBitmapToImageBitmap(bitmap: PlatformBitmap): ImageBitmap {
    return bitmap.asImageBitmap()
}

actual fun saveMemeToGallery(bitmap: PlatformBitmap): String? {
    val resolver = appContext.contentResolver
    val fileName = MemeFileNaming.generateFileName(System.currentTimeMillis())

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/Memer"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri: Uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return null

    return try {
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        } ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            MediaScannerConnection.scanFile(appContext, arrayOf(uri.toString()), arrayOf("image/jpeg"), null)
        }
        uri.toString()
    } catch (e: SecurityException) {
        resolver.delete(uri, null, null)
        null
    }
}

actual fun stageShareableImage(bitmap: PlatformBitmap): String {
    val dir = File(appContext.cacheDir, "shared_memes").apply { mkdirs() }
    val file = File(dir, MemeFileNaming.generateFileName(System.currentTimeMillis()))
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
    }
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

    val chooser = Intent.createChooser(intent, "Share meme via").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContext.startActivity(chooser)
}

actual fun copyMemeToClipboard(filePath: String) {
    val file = File(filePath)
    val authority = appContext.packageName + ".fileprovider"
    val uri = FileProvider.getUriForFile(appContext, authority, file)

    val clipData = ClipData.newUri(appContext.contentResolver, "Meme Image", uri)
    val clipboard = appContext.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(clipData)
}
