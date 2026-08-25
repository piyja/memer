package com.piyja.memer.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.piyja.memer.util.PlatformBitmap
import com.piyja.memer.util.deleteGalleryEntry
import com.piyja.memer.util.listGalleryEntryIds
import com.piyja.memer.util.readGalleryEntry
import com.piyja.memer.util.saveGifToAppStorage
import com.piyja.memer.util.saveRenderedMemeImage
import com.piyja.memer.util.writeGalleryEntry
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object GifGallery {

    private val _memes = mutableStateOf<List<GifMeme>>(emptyList())
    val memes: State<List<GifMeme>> = _memes

    fun load() {
        val loaded = mutableListOf<GifMeme>()
        for (id in listGalleryEntryIds()) {
            val raw = readGalleryEntry(id) ?: continue
            decodeGifMeme(raw)?.let { loaded.add(it) }
        }
        loaded.sortByDescending { it.createdAt }
        _memes.value = loaded
    }

    fun add(
        title: String,
        gifBytes: ByteArray,
        thumb: PlatformBitmap,
        projectJson: String
    ): GifMeme? {
        val id = "gif-" + Random.nextLong().toString(16)
        val gifFileName = saveGifToAppStorage(gifBytes, id) ?: return null
        val thumbFileName = saveRenderedMemeImage(thumb, id) ?: return null
        val meme = GifMeme(
            id = id,
            title = title,
            gifFileName = gifFileName,
            thumbFileName = thumbFileName,
            encodedFrames = projectJson,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        writeGalleryEntry(id, encodeGifMeme(meme))
        _memes.value = (_memes.value + meme).sortedByDescending { it.createdAt }
        return meme
    }

    fun remove(id: String) {
        deleteGalleryEntry(id)
        _memes.value = _memes.value.filter { it.id != id }
    }

    fun get(id: String): GifMeme? = _memes.value.firstOrNull { it.id == id }
}
