package com.piyja.memer.util

import androidx.compose.ui.graphics.ImageBitmap

expect class PlatformBitmap

data class PositionedText(
    val text: String,
    val xRatio: Float,
    val yRatio: Float,
    val scale: Float = 1f,
    val color: Long = 0xFFFFFFFF,
    val bold: Boolean = true,
    val strike: Boolean = false
)

/** Raw RGBA pixels of a rendered bitmap, used to feed the GIF encoder. */
data class RgbaImage(
    val width: Int,
    val height: Int,
    val pixels: ByteArray // length = width * height * 4, RGBA order
) {
    init {
        require(pixels.size == width * height * 4) {
            "pixels size ${pixels.size} != expected ${width * height * 4}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RgbaImage) return false
        return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}

/** A single frame extracted from a picked video/GIF, with its display duration. */
data class MediaFrame(
    val bitmap: PlatformBitmap,
    val durationMs: Long
)

/** Metadata about a picked video/GIF source. */
data class MediaInfo(
    val durationMs: Long,
    val isGif: Boolean
)

/** Decode source duration and whether it is an animated GIF. */
expect fun getMediaInfo(path: String): MediaInfo

/** Extract frames from [startMs] to [endMs] (inclusive) at roughly [fps] frames/sec. */
expect fun extractFrames(path: String, startMs: Long, endMs: Long, fps: Int): List<MediaFrame>

/** Names of bundled sample GIFs shipped in the app resources. */
fun loadBundledGifNames(): List<String> = listOf("digger.gif", "sample.gif")

/** Raw bytes of a bundled sample GIF by [name]. */
expect suspend fun loadBundledGifBytes(name: String): ByteArray?

/** Decode a GIF's bytes into animatable frames. */
expect fun decodeGifFrames(bytes: ByteArray): List<MediaFrame>

/** Copy a bundled sample GIF to a temp file and return its path (for editing). */
expect suspend fun copyBundledGifToTempFile(name: String): String?

expect suspend fun loadTemplateBitmap(assetPath: String): PlatformBitmap

expect fun renderMeme(
    bitmap: PlatformBitmap,
    texts: List<PositionedText>
): PlatformBitmap

expect fun platformBitmapToImageBitmap(bitmap: PlatformBitmap): ImageBitmap

expect fun saveMemeToGallery(bitmap: PlatformBitmap): String?

expect fun stageShareableImage(bitmap: PlatformBitmap): String

expect fun shareMemeImage(filePath: String)

expect fun copyMemeToClipboard(filePath: String)

expect fun loadTemplateState(templateId: String): String?

expect fun saveTemplateState(templateId: String, state: String)

expect fun clearTemplateState(templateId: String)

// Gallery of created memes (in-app, persisted)
expect fun writeGalleryEntry(id: String, content: String)

expect fun readGalleryEntry(id: String): String?

expect fun listGalleryEntryIds(): List<String>

expect fun deleteGalleryEntry(id: String)

expect fun saveRenderedMemeImage(bitmap: PlatformBitmap, id: String): String?

expect fun loadRenderedMemeImage(fileName: String): PlatformBitmap?

// --- GIF (animated meme) support ---

/** Extract raw RGBA pixels from a rendered bitmap for the GIF encoder. */
expect fun bitmapToRgba(bitmap: PlatformBitmap): RgbaImage

/** Persist encoded GIF bytes in the app's gallery storage; returns the stored file name. */
expect fun saveGifToAppStorage(bytes: ByteArray, id: String): String?

/** Load previously stored GIF bytes by file name. */
expect fun loadGifFromAppStorage(fileName: String): ByteArray?

/** Write GIF bytes to a cache location and return its path for sharing. */
expect fun stageShareableGif(bytes: ByteArray): String

/** Share a staged GIF file. */
expect fun shareGifFile(filePath: String)

/** Copy a staged GIF file to the clipboard. */
expect fun copyGifToClipboard(filePath: String)
