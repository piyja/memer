package com.piyja.memer.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.piyja.memer.data.MemeTemplate
import com.piyja.memer.util.copyMemeToClipboard
import com.piyja.memer.util.loadTemplateBitmap
import com.piyja.memer.util.platformBitmapToImageBitmap
import com.piyja.memer.util.renderMeme
import com.piyja.memer.util.saveMemeImage
import com.piyja.memer.util.shareMemeImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeEditorScreen(
    template: MemeTemplate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var topText by remember { mutableStateOf("") }
    var bottomText by remember { mutableStateOf("") }

    val renderedBitmap by produceState<PlatformBitmapHolder?>(initialValue = null, template, topText, bottomText) {
        value = try {
            val templateBitmap = withContext(Dispatchers.Default) { loadTemplateBitmap(template.imageAssetName) }
            val rendered = withContext(Dispatchers.Default) { renderMeme(templateBitmap, topText, bottomText) }
            PlatformBitmapHolder(rendered)
        } catch (e: Exception) {
            null
        }
    }

    val previewImage: ImageBitmap? = renderedBitmap?.let { platformBitmapToImageBitmap(it.bitmap) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Edit Meme") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                val img = previewImage
                if (img != null) {
                    Image(
                        bitmap = img,
                        contentDescription = "Meme preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Loading template...")
                }
            }

            OutlinedTextField(
                value = topText,
                onValueChange = { topText = it },
                label = { Text("Top text") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bottomText,
                onValueChange = { bottomText = it },
                label = { Text("Bottom text") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val holder = renderedBitmap ?: return@Button
                        scope.launch {
                            val path = withContext(Dispatchers.Default) { saveMemeImage(holder.bitmap) }
                            snackbarHostState.showSnackbar("Meme saved to app storage")
                        }
                    },
                    enabled = renderedBitmap != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }

                Button(
                    onClick = {
                        val holder = renderedBitmap ?: return@Button
                        scope.launch {
                            val path = withContext(Dispatchers.Default) { saveMemeImage(holder.bitmap) }
                            withContext(Dispatchers.Main) { shareMemeImage(path) }
                        }
                    },
                    enabled = renderedBitmap != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Share") }

                Button(
                    onClick = {
                        val holder = renderedBitmap ?: return@Button
                        scope.launch {
                            val path = withContext(Dispatchers.Default) { saveMemeImage(holder.bitmap) }
                            withContext(Dispatchers.Main) { copyMemeToClipboard(path) }
                            snackbarHostState.showSnackbar("Meme copied to clipboard")
                        }
                    },
                    enabled = renderedBitmap != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Copy") }
            }
        }
    }
}

private class PlatformBitmapHolder(val bitmap: com.piyja.memer.util.PlatformBitmap)
