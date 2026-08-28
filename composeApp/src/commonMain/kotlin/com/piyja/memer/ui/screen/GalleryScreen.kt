package com.piyja.memer.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.piyja.memer.data.CreatedMeme
import com.piyja.memer.data.MemeGallery
import com.piyja.memer.util.loadRenderedMemeImage
import com.piyja.memer.util.platformBitmapToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onMemeSelected: (CreatedMeme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memes by MemeGallery.memes
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var deletePending by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            selectionMode -> "${selectedIds.size} selected"
                            else -> "Your Memes"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectionMode) {
                                selectedIds = emptySet()
                                selectionMode = false
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (selectionMode) "Cancel" else "Back"
                        )
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = { deletePending = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                selectionMode = true
                                selectedIds = emptySet()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SelectAll,
                                contentDescription = "Select"
                            )
                        }
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
                Text("No memes yet — create one from a template!")
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
                items(memes, key = { it.id }) { meme ->
                    GalleryCard(
                        meme = meme,
                        selected = meme.id in selectedIds,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (meme.id in selectedIds) {
                                    selectedIds - meme.id
                                } else {
                                    selectedIds + meme.id
                                }
                            } else {
                                onMemeSelected(meme)
                            }
                        },
                        onDeleteRequest = {
                            selectedIds = setOf(meme.id)
                            deletePending = true
                        }
                    )
                }
            }
        }
    }

    if (deletePending) {
        AlertDialog(
            onDismissRequest = { deletePending = false },
            title = {
                Text(if (selectedIds.size == 1) "Delete this meme?" else "Delete ${selectedIds.size} memes?")
            },
            text = {
                Text(
                    if (selectedIds.size == 1) {
                        "This meme will be removed from your gallery."
                    } else {
                        "These memes will be removed from your gallery."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        MemeGallery.removeAll(selectedIds)
                        selectedIds = emptySet()
                        selectionMode = false
                        deletePending = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletePending = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GalleryCard(
    meme: CreatedMeme,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val bitmap by produceState<ImageBitmap?>(initialValue = null, meme.imageFileName) {
                value = try {
                    val loaded = withContext(Dispatchers.Default) {
                        loadRenderedMemeImage(meme.imageFileName)
                    }
                    loaded?.let { platformBitmapToImageBitmap(it) }
                } catch (e: Exception) {
                    null
                }
            }
            bitmap?.let { img ->
                Image(
                    bitmap = img,
                    contentDescription = meme.templateName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(28.dp)
                        .shadow(2.dp, CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
            Text(
                text = meme.templateName,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .shadow(2.dp, RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
