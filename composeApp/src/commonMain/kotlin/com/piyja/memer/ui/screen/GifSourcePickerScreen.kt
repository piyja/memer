package com.piyja.memer.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.piyja.memer.ui.AnimatedGifImage
import com.piyja.memer.util.loadBundledGifBytes
import com.piyja.memer.util.loadBundledGifNames
import com.piyja.memer.util.rememberVideoGifPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifSourcePickerScreen(
    onPickVideoGif: (String) -> Unit,
    onOpenGallery: () -> Unit,
    onEditSample: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picker = rememberVideoGifPicker()
    var showSample by remember { mutableStateOf<String?>(null) }
    val sampleNames = remember { loadBundledGifNames() }

    if (showSample != null) {
        SampleShowWindow(
            name = showSample!!,
            onClose = { showSample = null },
            onEdit = { onEditSample(showSample!!) }
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Make a GIF") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Pick a video or GIF from your device, then trim a time range and add text frame-by-frame.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    picker.launch { path -> if (path != null) onPickVideoGif(path) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pick video or GIF")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenGallery, modifier = Modifier.fillMaxWidth()) {
                Text("Your GIFs")
            }

            if (sampleNames.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                Text(
                    "Sample GIFs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                val rows = (sampleNames.size + 1) / 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((rows * 176 + 8).dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(sampleNames) { name ->
                        val bytes = remember(name) { loadBundledGifBytes(name) }
                        Box(
                            modifier = Modifier
                                .height(160.dp)
                                .fillMaxWidth()
                                .clickable { showSample = name }
                        ) {
                            bytes?.let {
                                AnimatedGifImage(
                                    bytes = it,
                                    contentDescription = name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleShowWindow(name: String, onClose: () -> Unit, onEdit: () -> Unit) {
    val bytes = remember(name) { loadBundledGifBytes(name) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(onClick = onEdit) { Text("Edit") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                bytes?.let {
                    AnimatedGifImage(
                        bytes = it,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Text("Could not load GIF")
            }
            Text(
                "Tap Edit to trim and add frame-by-frame text",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
