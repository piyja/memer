package com.piyja.memer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.piyja.memer.data.CreatedMeme
import com.piyja.memer.data.MemeGallery
import com.piyja.memer.data.MemeTemplate
import com.piyja.memer.data.toTemplate
import com.piyja.memer.ui.screen.GalleryScreen
import com.piyja.memer.ui.screen.MemeEditorScreen
import com.piyja.memer.ui.screen.TemplatePickerScreen

sealed interface Screen {
    data object Picker : Screen
    data object Gallery : Screen
    data class Editor(
        val template: MemeTemplate,
        val initialEncodedTexts: String? = null,
        val galleryMemeId: String? = null
    ) : Screen
}

@Composable
fun MemerApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Picker) }

    LaunchedEffect(Unit) { MemeGallery.load() }

    when (val current = screen) {
        is Screen.Picker -> TemplatePickerScreen(
            onTemplateSelected = { template ->
                screen = Screen.Editor(template)
            },
            onOpenGallery = { screen = Screen.Gallery },
            modifier = Modifier.fillMaxSize()
        )
        is Screen.Gallery -> GalleryScreen(
            onMemeSelected = { meme: CreatedMeme ->
                screen = Screen.Editor(
                    template = meme.toTemplate(),
                    initialEncodedTexts = meme.encodedTexts,
                    galleryMemeId = meme.id
                )
            },
            onBack = { screen = Screen.Picker },
            modifier = Modifier.fillMaxSize()
        )
        is Screen.Editor -> MemeEditorScreen(
            template = current.template,
            initialEncodedTexts = current.initialEncodedTexts,
            galleryMemeId = current.galleryMemeId,
            onBack = {
                screen = if (current.galleryMemeId != null) Screen.Gallery else Screen.Picker
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
