package com.piyja.memer.data

import com.piyja.memer.util.PlatformBitmap
import com.piyja.memer.util.decodeGifProject
import com.piyja.memer.util.encodeGifProject
import com.piyja.memer.util.loadGifFromAppStorage
import com.piyja.memer.util.loadRenderedMemeImage

data class GifMeme(
    val id: String,
    val title: String,
    val gifFileName: String,
    val thumbFileName: String,
    val encodedFrames: String,
    val createdAt: Long
) {
    fun toProject(): GifProject? = decodeGifProject(encodedFrames)
}

fun GifMeme.loadGifBytes(): ByteArray? = loadGifFromAppStorage(gifFileName)

fun GifMeme.loadThumbBitmap(): PlatformBitmap? = loadRenderedMemeImage(thumbFileName)

private const val FRAMES_MARKER = "%%GIF_FRAMES%%"

fun encodeGifMeme(meme: GifMeme): String = buildString {
    appendLine(meme.id)
    appendLine(meme.title)
    appendLine(meme.gifFileName)
    appendLine(meme.thumbFileName)
    appendLine(meme.createdAt.toString())
    appendLine(FRAMES_MARKER)
    append(meme.encodedFrames)
}

fun decodeGifMeme(raw: String): GifMeme? {
    val lines = raw.split("\n")
    val markerIndex = lines.indexOf(FRAMES_MARKER)
    if (markerIndex < 4) return null
    val id = lines[0]
    val title = lines[1]
    val gifFileName = lines[2]
    val thumbFileName = lines[3]
    val createdAt = lines[4].toLongOrNull() ?: return null
    val encodedFrames = lines.drop(markerIndex + 1).joinToString("\n")
    return GifMeme(
        id = id,
        title = title,
        gifFileName = gifFileName,
        thumbFileName = thumbFileName,
        encodedFrames = encodedFrames,
        createdAt = createdAt
    )
}
