package com.piyja.memer.ui.screen

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.piyja.memer.data.MemeTemplate
import com.piyja.memer.util.ClipboardHelper
import com.piyja.memer.util.ImageSaver
import com.piyja.memer.util.MemeRenderer
import com.piyja.memer.util.ShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeEditorScreen(
    template: MemeTemplate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var topText by remember { mutableStateOf("") }
    var bottomText by remember { mutableStateOf("") }
    var savedFile by remember { mutableStateOf<java.io.File?>(null) }

    val renderedBitmap by produceState<Bitmap?>(initialValue = null, template, topText, bottomText) {
        value = try {
            MemeRenderer.render(context, template, topText, bottomText)
        } catch (e: Exception) {
            null
        }
    }

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
                val bmp = renderedBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
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
                        val bmp = renderedBitmap ?: return@Button
                        scope.launch {
                            val file = ImageSaver.saveToInternalStorage(context, bmp)
                            savedFile = file
                            snackbarHostState.showSnackbar("Meme saved to app storage")
                        }
                    },
                    enabled = renderedBitmap != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }

                Button(
                    onClick = {
                        val bmp = renderedBitmap ?: return@Button
                        scope.launch {
                            val file = ImageSaver.saveToInternalStorage(context, bmp)
                            savedFile = file
                            ShareHelper.share(context, file)
                        }
                    },
                    enabled = renderedBitmap != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Share") }

                Button(
                    onClick = {
                        val bmp = renderedBitmap ?: return@Button
                        scope.launch {
                            val file = ImageSaver.saveToInternalStorage(context, bmp)
                            savedFile = file
                            ClipboardHelper.copyToClipboard(context, file)
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