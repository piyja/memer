package com.piyja.memer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.piyja.memer.data.MemeGallery
import com.piyja.memer.data.MemeTemplate
import com.piyja.memer.data.toPositionedTexts
import com.piyja.memer.util.TemplateStateCodec
import com.piyja.memer.util.copyMemeToClipboard
import com.piyja.memer.util.loadTemplateBitmap
import com.piyja.memer.util.loadTemplateState
import com.piyja.memer.util.platformBitmapToImageBitmap
import com.piyja.memer.util.renderMeme
import com.piyja.memer.util.saveMemeToGallery
import com.piyja.memer.util.saveTemplateState
import com.piyja.memer.util.shareMemeImage
import com.piyja.memer.util.stageShareableImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeEditorScreen(
    template: MemeTemplate,
    onBack: () -> Unit,
    initialEncodedTexts: String? = null,
    galleryMemeId: String? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state = remember(template, galleryMemeId) {
        MemeEditState(TemplateStateCodec.decodeBoxes(initialEncodedTexts ?: loadTemplateState(template.id)))
    }

    val templateImage by produceState<ImageBitmap?>(initialValue = null, template) {
        value = try {
            withContext(Dispatchers.Default) {
                platformBitmapToImageBitmap(loadTemplateBitmap(template.imageAssetName))
            }
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(state.textBoxes.toList(), template.id) {
        if (galleryMemeId != null) return@LaunchedEffect
        delay(500)
        withContext(Dispatchers.Default) {
            saveTemplateState(template.id, TemplateStateCodec.encodeBoxes(state.textBoxes))
        }
    }

    fun flushAndBack() {
        scope.launch(Dispatchers.Default) {
            if (galleryMemeId == null) {
                saveTemplateState(template.id, TemplateStateCodec.encodeBoxes(state.textBoxes))
            }
            withContext(Dispatchers.Main) { onBack() }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Meme") },
                navigationIcon = {
                    IconButton(onClick = { flushAndBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { state.undo() },
                        enabled = state.canUndo()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Undo,
                            contentDescription = "Undo"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MemeEditSurface(state = state, templateImage = templateImage, modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val texts = state.textBoxes.toPositionedTexts()
                        val encoded = TemplateStateCodec.encodeBoxes(state.textBoxes)
                        scope.launch {
                            try {
                                val savedToPhotos = withContext(Dispatchers.Default) {
                                    val base = loadTemplateBitmap(template.imageAssetName)
                                    val rendered = renderMeme(base, texts)
                                    saveMemeToGallery(rendered)
                                    if (galleryMemeId != null) {
                                        MemeGallery.update(galleryMemeId, template, encoded, rendered)
                                    } else {
                                        MemeGallery.add(template, encoded, rendered)
                                    }
                                }
                                snackbarHostState.showSnackbar(
                                    if (savedToPhotos != null) "Saved to your gallery" else "Couldn't save to gallery"
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Something went wrong")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }

                Button(
                    onClick = {
                        val texts = state.textBoxes.toPositionedTexts()
                        scope.launch {
                            try {
                                val path = withContext(Dispatchers.Default) {
                                    val base = loadTemplateBitmap(template.imageAssetName)
                                    stageShareableImage(renderMeme(base, texts))
                                }
                                withContext(Dispatchers.Main) { shareMemeImage(path) }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Sharing failed")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Share") }

                Button(
                    onClick = {
                        val texts = state.textBoxes.toPositionedTexts()
                        scope.launch {
                            try {
                                val path = withContext(Dispatchers.Default) {
                                    val base = loadTemplateBitmap(template.imageAssetName)
                                    stageShareableImage(renderMeme(base, texts))
                                }
                                withContext(Dispatchers.Main) { copyMemeToClipboard(path) }
                                snackbarHostState.showSnackbar("Meme copied to clipboard")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Copy failed")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Copy") }
            }
        }
    }
}
