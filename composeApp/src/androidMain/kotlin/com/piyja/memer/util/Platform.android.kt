package com.piyja.memer.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.piyja.memer.util.AndroidContextHolder.appContext
import memer.composeapp.generated.resources.Res
import java.io.File
import java.io.FileOutputStream

actual typealias PlatformBitmap = Bitmap

actual suspend fun loadTemplateBitmap(assetPath: String): PlatformBitmap {
    if (assetPath.startsWith("/")) {
        return BitmapFactory.decodeFile(assetPath)
            ?: throw IllegalArgumentException("Failed to decode image at $assetPath")
    }
    val bytes = Res.readBytes("files/$assetPath")
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw IllegalArgumentException("Failed to decode image at $assetPath")
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
        } * positioned.scale

        val fillPaint = Paint().apply {
            color = positioned.color.toInt()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = if (positioned.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.textSize = textSize
        }
        val strokePaint = Paint(fillPaint).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val cx = positioned.xRatio * mutable.width
        val baselineY = positioned.yRatio * mutable.height + textSize * 0.35f

        val textWidth = fillPaint.measureText(formatted)
        val textAscent = fillPaint.ascent().toFloat()
        val blockLeft = cx - textWidth / 2f - textSize * 0.12f
        val blockTop = baselineY + textAscent - textSize * 0.25f
        val blockRight = cx + textWidth / 2f + textSize * 0.12f
        val blockBottom = baselineY - textAscent + textSize * 0.08f
        val blockPaint = Paint().apply {
            color = android.graphics.Color.argb(128, 0, 0, 0)
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            blockLeft, blockTop, blockRight, blockBottom,
            textSize * 0.12f, textSize * 0.12f,
            blockPaint
        )

        canvas.drawText(formatted, cx, baselineY, strokePaint)
        canvas.drawText(formatted, cx, baselineY, fillPaint)

        if (positioned.strike) {
            val midY = baselineY - textSize * 0.35f
            val strikePaint = Paint().apply {
                color = positioned.color.toInt()
                isAntiAlias = true
                strokeWidth = textSize * 0.08f
            }
            canvas.drawLine(cx - textWidth / 2f, midY, cx + textWidth / 2f, midY, strikePaint)
        }
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

actual fun bitmapToRgba(bitmap: PlatformBitmap): RgbaImage {
    val width = bitmap.width
    val height = bitmap.height
    val argb = IntArray(width * height)
    bitmap.getPixels(argb, 0, bitmap.width, 0, 0, width, height)
    val pixels = ByteArray(width * height * 4)
    var p = 0
    for (color in argb) {
        pixels[p++] = ((color shr 16) and 0xFF).toByte()
        pixels[p++] = ((color shr 8) and 0xFF).toByte()
        pixels[p++] = (color and 0xFF).toByte()
        pixels[p++] = ((color shr 24) and 0xFF).toByte()
    }
    return RgbaImage(width, height, pixels)
}

actual fun saveGifToAppStorage(bytes: ByteArray, id: String): String? {
    val dir = File(appContext.filesDir, "gallery").apply { mkdirs() }
    val file = File(dir, "$id.gif")
    return try {
        file.writeBytes(bytes)
        file.name
    } catch (e: Exception) {
        null
    }
}

actual fun saveGifToGallery(bytes: ByteArray): String? {
    val resolver = appContext.contentResolver
    val fileName = "memer_${System.currentTimeMillis()}.gif"

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
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
            output.write(bytes)
        } ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            MediaScannerConnection.scanFile(
                appContext,
                arrayOf(uri.toString()),
                arrayOf("image/gif"),
                null
            )
        }
        uri.toString()
    } catch (e: SecurityException) {
        resolver.delete(uri, null, null)
        null
    }
}

actual fun loadGifFromAppStorage(fileName: String): ByteArray? {
    val file = File(appContext.filesDir, "gallery/$fileName")
    return runCatching { file.readBytes() }.getOrNull()
}

actual fun stageShareableGif(bytes: ByteArray): String {
    val dir = File(appContext.cacheDir, "shared_memes").apply { mkdirs() }
    val file = File(dir, "${MemeFileNaming.generateFileName(System.currentTimeMillis())}.gif")
    file.writeBytes(bytes)
    return file.absolutePath
}

actual fun shareGifFile(filePath: String) {
    val file = File(filePath)
    val authority = appContext.packageName + ".fileprovider"
    val uri = FileProvider.getUriForFile(appContext, authority, file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        type = "image/gif"
    }

    val chooser = Intent.createChooser(intent, "Share GIF via").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContext.startActivity(chooser)
}

actual fun copyGifToClipboard(filePath: String) {
    val file = File(filePath)
    val authority = appContext.packageName + ".fileprovider"
    val uri = FileProvider.getUriForFile(appContext, authority, file)

    val clipData = ClipData.newUri(appContext.contentResolver, "GIF", uri)
    val clipboard = appContext.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(clipData)
}

private const val MAX_FRAME_DIM = 480

private fun scaleBitmap(source: Bitmap): Bitmap {
    val maxDim = maxOf(source.width, source.height)
    if (maxDim <= MAX_FRAME_DIM) return source
    val ratio = MAX_FRAME_DIM.toFloat() / maxDim
    val w = (source.width * ratio).toInt()
    val h = (source.height * ratio).toInt()
    return Bitmap.createScaledBitmap(source, w, h, true)
}

actual fun getMediaInfo(path: String): MediaInfo {
    val isGif = path.endsWith(".gif", ignoreCase = true)
    return if (isGif) {
        val movie = Movie.decodeFile(path)
        MediaInfo((movie?.duration() ?: 0).toLong(), true)
    } else {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            MediaInfo(dur, false)
        } finally {
            retriever.release()
        }
    }
}

actual fun extractFrames(path: String, startMs: Long, endMs: Long, fps: Int): List<MediaFrame> {
    val isGif = path.endsWith(".gif", ignoreCase = true)
    val effectiveFps = fps.coerceAtLeast(1)
    val frameDur = 1000L / effectiveFps
    val out = mutableListOf<MediaFrame>()

    if (isGif) {
        val movie = Movie.decodeFile(path) ?: return emptyList()
        val total = (movie.duration() ?: 0).toLong()
        val gifFrameDur = 100L
        var t = startMs
        while (t < endMs && t <= total) {
            movie.setTime(t.toInt())
            val bmp = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
            movie.draw(Canvas(bmp), 0f, 0f)
            out.add(MediaFrame(scaleBitmap(bmp), gifFrameDur))
            t += gifFrameDur
        }
    } else {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            var t = startMs
            while (t < endMs) {
                val bmp = retriever.getFrameAtTime(
                    t * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (bmp != null) out.add(MediaFrame(scaleBitmap(bmp), frameDur))
                t += frameDur
            }
        } finally {
            retriever.release()
        }
    }

    if (out.isEmpty() && endMs > startMs) {
        val fallback = if (isGif) {
            val m = Movie.decodeFile(path)
            m?.let {
                m.setTime(startMs.toInt())
                val b = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888)
                m.draw(Canvas(b), 0f, 0f)
                MediaFrame(scaleBitmap(b), frameDur)
            }
        } else {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(path)
                r.getFrameAtTime(startMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.let { MediaFrame(scaleBitmap(it), frameDur) }
            } finally {
                r.release()
            }
        }
        fallback?.let { out.add(it) }
    }

    return out
}

actual suspend fun loadBundledGifBytes(name: String): ByteArray? {
    return try {
        Res.readBytes("files/gifs/$name")
    } catch (e: Exception) {
        null
    }
}

actual fun decodeGifFrames(bytes: ByteArray): List<MediaFrame> {
    val movie = Movie.decodeByteArray(bytes, 0, bytes.size) ?: return emptyList()
    val duration = (movie.duration() ?: 0).toLong().coerceAtLeast(1)
    val frameDur = 100L
    val out = mutableListOf<MediaFrame>()
    var t = 0L
    while (t < duration && out.size < 400) {
        movie.setTime(t.toInt())
        val bmp = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
        movie.draw(Canvas(bmp), 0f, 0f)
        out.add(MediaFrame(bmp, frameDur))
        t += frameDur
    }
    if (out.isEmpty()) {
        movie.setTime(0)
        val bmp = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
        movie.draw(Canvas(bmp), 0f, 0f)
        out.add(MediaFrame(bmp, duration.coerceAtLeast(100)))
    }
    return out
}

actual suspend fun copyBundledGifToTempFile(name: String): String? {
    return try {
        val bytes = loadBundledGifBytes(name) ?: return null
        val dir = File(appContext.cacheDir, "bundled_gifs").apply { mkdirs() }
        val file = File(dir, name)
        file.writeBytes(bytes)
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
