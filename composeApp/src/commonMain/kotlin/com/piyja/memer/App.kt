package com.piyja.memer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.piyja.memer.data.CreatedMeme
import com.piyja.memer.data.GifGallery
import com.piyja.memer.data.MemeGallery
import com.piyja.memer.data.MemeTemplate
import com.piyja.memer.data.toTemplate
import com.piyja.memer.ui.screen.GalleryScreen
import com.piyja.memer.ui.screen.GifGalleryScreen
import com.piyja.memer.ui.screen.GifSourcePickerScreen
import com.piyja.memer.ui.screen.GifVideoEditorScreen
import com.piyja.memer.util.copyBundledGifToTempFile
import com.piyja.memer.ui.screen.MemeEditorScreen
import com.piyja.memer.ui.screen.TemplatePickerScreen

enum class TopTab { MEME, GIF }

sealed interface Screen {
    data object Picker : Screen
    data object Gallery : Screen
    data class Editor(
        val template: MemeTemplate,
        val initialEncodedTexts: String? = null,
        val galleryMemeId: String? = null
    ) : Screen

    data object GifSource : Screen
    data object GifGallery : Screen
    data class GifEditor(
        val sourcePath: String? = null,
        val projectJson: String? = null
    ) : Screen
}

@Composable
fun MemerApp() {
    var tab by remember { mutableStateOf(TopTab.MEME) }
    var memeScreen by remember { mutableStateOf<Screen>(Screen.Picker) }
    var gifScreen by remember { mutableStateOf<Screen>(Screen.GifSource) }

    LaunchedEffect(Unit) {
        MemeGallery.load()
        GifGallery.load()
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .consumeWindowInsets(WindowInsets.statusBars)
        ) {
            TabRow(selectedTabIndex = tab.ordinal) {
            Tab(
                selected = tab == TopTab.MEME,
                onClick = { tab = TopTab.MEME },
                text = { Text("Meme") }
            )
            Tab(
                selected = tab == TopTab.GIF,
                onClick = { tab = TopTab.GIF },
                text = { Text("GIF") }
            )
        }

        when (tab) {
            TopTab.MEME -> when (val current = memeScreen) {
                is Screen.Picker -> TemplatePickerScreen(
                    onTemplateSelected = { memeScreen = Screen.Editor(it) },
                    onOpenGallery = { memeScreen = Screen.Gallery },
                    modifier = Modifier.fillMaxSize()
                )
                is Screen.Gallery -> GalleryScreen(
                    onMemeSelected = { meme: CreatedMeme ->
                        memeScreen = Screen.Editor(
                            template = meme.toTemplate(),
                            initialEncodedTexts = meme.encodedTexts,
                            galleryMemeId = meme.id
                        )
                    },
                    onBack = { memeScreen = Screen.Picker },
                    modifier = Modifier.fillMaxSize()
                )
                is Screen.Editor -> MemeEditorScreen(
                    template = current.template,
                    initialEncodedTexts = current.initialEncodedTexts,
                    galleryMemeId = current.galleryMemeId,
                    onBack = {
                        memeScreen = if (current.galleryMemeId != null) Screen.Gallery else Screen.Picker
                    },
                    modifier = Modifier.fillMaxSize()
                )
                else -> {}
            }
            TopTab.GIF -> when (val current = gifScreen) {
                is Screen.GifSource -> GifSourcePickerScreen(
                    onPickVideoGif = { gifScreen = Screen.GifEditor(sourcePath = it) },
                    onOpenGallery = { gifScreen = Screen.GifGallery },
                    onEditSample = { name ->
                        val path = copyBundledGifToTempFile(name)
                        if (path != null) gifScreen = Screen.GifEditor(sourcePath = path)
                    },
                    onBack = { gifScreen = Screen.GifSource },
                    modifier = Modifier.fillMaxSize()
                )
                is Screen.GifGallery -> GifGalleryScreen(
                    onGifSelected = { gif ->
                        gifScreen = Screen.GifEditor(projectJson = gif.encodedFrames)
                    },
                    onBack = { gifScreen = Screen.GifSource },
                    modifier = Modifier.fillMaxSize()
                )
                is Screen.GifEditor -> GifVideoEditorScreen(
                    initialSourcePath = current.sourcePath,
                    initialProjectJson = current.projectJson,
                    onBack = {
                        gifScreen = if (current.projectJson != null) Screen.GifGallery else Screen.GifSource
                    },
                    modifier = Modifier.fillMaxSize()
                )
                else -> {}
            }
        }
    }
}
