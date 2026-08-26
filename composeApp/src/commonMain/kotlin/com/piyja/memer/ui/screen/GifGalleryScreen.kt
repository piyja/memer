package com.piyja.memer.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.piyja.memer.data.GifGallery
import com.piyja.memer.data.GifMeme
import com.piyja.memer.util.loadRenderedMemeImage
import com.piyja.memer.util.platformBitmapToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifGalleryScreen(
    onGifSelected: (GifMeme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memes by GifGallery.memes

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Your GIFs") },
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
        if (memes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No GIFs yet — create one from a video or GIF!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(memes) { meme ->
                    GifGalleryCard(
                        meme = meme,
                        onClick = { onGifSelected(meme) },
                        onDelete = { GifGallery.remove(meme.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GifGalleryCard(
    meme: GifMeme,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val bitmap by produceState<ImageBitmap?>(initialValue = null, meme.thumbFileName) {
                    value = try {
                        val loaded = withContext(Dispatchers.Default) {
                            loadRenderedMemeImage(meme.thumbFileName)
                        }
                        loaded?.let { platformBitmapToImageBitmap(it) }
                    } catch (e: Exception) {
                        null
                    }
                }
                bitmap?.let { img ->
                    Image(
                        bitmap = img,
                        contentDescription = meme.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = meme.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Animated GIF",
                    style = MaterialTheme.typography.labelSmall
                )
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                    Text("Remove")
                }
            }
        }
    }
}
