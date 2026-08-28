package com.piyja.memer.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.piyja.memer.util.PlatformBitmap
import com.piyja.memer.util.deleteGalleryEntry
import com.piyja.memer.util.listGalleryEntryIds
import com.piyja.memer.util.readGalleryEntry
import com.piyja.memer.util.saveRenderedMemeImage
import com.piyja.memer.util.writeGalleryEntry
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object MemeGallery {

    private val _memes = mutableStateOf<List<CreatedMeme>>(emptyList())
    val memes: State<List<CreatedMeme>> = _memes

    fun load() {
        val loaded = mutableListOf<CreatedMeme>()
        for (id in listGalleryEntryIds()) {
            val raw = readGalleryEntry(id) ?: continue
            decodeCreatedMeme(raw)?.let { loaded.add(it) }
        }
        loaded.sortByDescending { it.createdAt }
        _memes.value = loaded
    }

    fun add(template: MemeTemplate, encodedTexts: String, rendered: PlatformBitmap): CreatedMeme? {
        val id = "meme-" + Random.nextLong().toString(16)
        val imageFileName = saveRenderedMemeImage(rendered, id) ?: return null
        val meme = CreatedMeme(
            id = id,
            templateId = template.id,
            templateName = template.name,
            templateImageAssetName = template.imageAssetName,
            imageFileName = imageFileName,
            encodedTexts = encodedTexts,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        writeGalleryEntry(id, encodeCreatedMeme(meme))
        _memes.value = (_memes.value + meme).sortedByDescending { it.createdAt }
        return meme
    }

    fun update(id: String, template: MemeTemplate, encodedTexts: String, rendered: PlatformBitmap): CreatedMeme? {
        val imageFileName = saveRenderedMemeImage(rendered, id) ?: return null
        val createdAt = _memes.value.firstOrNull { it.id == id }?.createdAt ?: Clock.System.now().toEpochMilliseconds()
        val meme = CreatedMeme(
            id = id,
            templateId = template.id,
            templateName = template.name,
            templateImageAssetName = template.imageAssetName,
            imageFileName = imageFileName,
            encodedTexts = encodedTexts,
            createdAt = createdAt
        )
        writeGalleryEntry(id, encodeCreatedMeme(meme))
        _memes.value = (_memes.value.filter { it.id != id } + meme).sortedByDescending { it.createdAt }
        return meme
    }

    fun remove(id: String) {
        deleteGalleryEntry(id)
        _memes.value = _memes.value.filter { it.id != id }
    }

    fun removeAll(ids: Set<String>) {
        ids.forEach { deleteGalleryEntry(it) }
        _memes.value = _memes.value.filter { it.id !in ids }
    }

    fun get(id: String): CreatedMeme? = _memes.value.firstOrNull { it.id == id }
}
