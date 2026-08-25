package com.piyja.memer.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.piyja.memer.data.MemeTemplate
import com.piyja.memer.data.TemplateCatalog
import com.piyja.memer.util.GalleryImagePicker
import com.piyja.memer.util.loadTemplateBitmap
import com.piyja.memer.util.platformBitmapToImageBitmap
import com.piyja.memer.util.rememberGalleryImagePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerScreen(
    onTemplateSelected: (MemeTemplate) -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val templates = TemplateCatalog.search(query)
    val galleryPicker: GalleryImagePicker = rememberGalleryImagePicker()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Memer") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        galleryPicker.launch { path ->
                            if (path != null) {
                                TemplateCatalog.addCustomTemplate(
                                    name = path.substringAfterLast('/').substringBeforeLast('.'),
                                    imagePath = path
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Add from gallery")
                }
                Button(
                    onClick = onOpenGallery,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("My Memes")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search templates & tags…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            if (templates.isEmpty() && query.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No templates match \"$query\"")
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    TemplateCard(template = template, onClick = { onTemplateSelected(template) })
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: MemeTemplate, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = template.name.take(2),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Gray
                )
                AsyncTemplateImage(
                    imageSource = template.imageAssetName,
                    contentDescription = template.name,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AsyncTemplateImage(
    imageSource: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, imageSource) {
        value = try {
            val loaded = withContext(Dispatchers.Default) { loadTemplateBitmap(imageSource) }
            platformBitmapToImageBitmap(loaded)
        } catch (e: Exception) {
            null
        }
    }

    bitmap?.let { img ->
        Image(
            bitmap = img,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}
