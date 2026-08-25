package com.piyja.memer.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyja.memer.data.MemeTemplate
import com.piyja.memer.data.MemeTextBox
import com.piyja.memer.data.MemeGallery
import com.piyja.memer.data.toPositionedTexts
import com.piyja.memer.util.TemplateStateCodec
import com.piyja.memer.util.clearTemplateState
import com.piyja.memer.util.loadTemplateState
import com.piyja.memer.util.saveTemplateState
import com.piyja.memer.util.PositionedText
import com.piyja.memer.util.copyMemeToClipboard
import com.piyja.memer.util.loadTemplateBitmap
import com.piyja.memer.util.platformBitmapToImageBitmap
import com.piyja.memer.util.renderMeme
import com.piyja.memer.util.saveMemeToGallery
import com.piyja.memer.util.stageShareableImage
import com.piyja.memer.util.shareMemeImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private var nextTextBoxId = 1L

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

    val textBoxes = remember(template, galleryMemeId) {
        mutableStateListOf<MemeTextBox>().apply {
            val restored = TemplateStateCodec.decodeBoxes(initialEncodedTexts ?: loadTemplateState(template.id))
                .map { it.copy(id = nextTextBoxId++) }
            if (restored.isNotEmpty()) {
                addAll(restored)
            } else {
                add(MemeTextBox(id = nextTextBoxId++, text = "", xRatio = 0.5f, yRatio = 0.2f))
            }
        }
    }
    var selectedBoxId by remember(template, galleryMemeId) { mutableStateOf(textBoxes.first().id) }
    var imageAreaSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(textBoxes.toList(), template.id) {
        if (galleryMemeId != null) return@LaunchedEffect
        delay(500)
        withContext(Dispatchers.Default) {
            saveTemplateState(template.id, TemplateStateCodec.encodeBoxes(textBoxes))
        }
    }

    fun flushAndBack() {
        scope.launch(Dispatchers.Default) {
            if (galleryMemeId == null) {
                saveTemplateState(template.id, TemplateStateCodec.encodeBoxes(textBoxes))
            }
            withContext(Dispatchers.Main) { onBack() }
        }
    }

    fun resetLayout() {
        clearTemplateState(template.id)
        textBoxes.clear()
        val fresh = MemeTextBox(id = nextTextBoxId++, text = "", xRatio = 0.5f, yRatio = 0.2f)
        textBoxes.add(fresh)
        selectedBoxId = fresh.id
    }

    fun updateBox(id: Long, transform: (MemeTextBox) -> MemeTextBox) {
        val index = textBoxes.indexOfFirst { it.id == id }
        if (index >= 0) {
            textBoxes[index] = transform(textBoxes[index])
        }
    }

    fun addTextBox() {
        val yOffset = ((textBoxes.size % 4) * 0.18f)
        val newY = (0.25f + yOffset).coerceAtMost(0.85f)
        val box = MemeTextBox(id = nextTextBoxId++, text = "", xRatio = 0.5f, yRatio = newY)
        textBoxes.add(box)
        selectedBoxId = box.id
    }

    fun removeSelectedBox() {
        if (textBoxes.size <= 1) return
        val index = textBoxes.indexOfFirst { it.id == selectedBoxId }
        textBoxes.removeAt(if (index >= 0) index else 0)
        selectedBoxId = textBoxes.first().id
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Meme") },
                navigationIcon = {
                    IconButton(onClick = { flushAndBack() }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
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
            val img = templateImage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (img != null) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(img.width.toFloat() / img.height.toFloat())
                            .background(Color.Black.copy(alpha = 0.05f))
                            .onSizeChanged { imageAreaSize = it }
                    ) {
                        Image(
                            bitmap = img,
                            contentDescription = "Meme template",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        textBoxes.forEach { box ->
                            DraggableMemeText(
                                box = box,
                                isSelected = box.id == selectedBoxId,
                                areaSize = imageAreaSize,
                                onSelect = { selectedBoxId = box.id },
                                onMove = { dx, dy ->
                                    updateBox(box.id) {
                                        it.copy(
                                            xRatio = (it.xRatio + dx / imageAreaSize.width.coerceAtLeast(1)).coerceIn(0.02f, 0.98f),
                                            yRatio = (it.yRatio + dy / imageAreaSize.height.coerceAtLeast(1)).coerceIn(0.02f, 0.98f)
                                        )
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Text("Loading template...")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { addTextBox() }, modifier = Modifier.weight(1f)) {
                    Text("+ Add text")
                }
                OutlinedButton(
                    onClick = { removeSelectedBox() },
                    enabled = textBoxes.size > 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete selected")
                }
                OutlinedButton(onClick = { resetLayout() }, modifier = Modifier.weight(1f)) {
                    Text("Reset")
                }
            }

            textBoxes.forEachIndexed { index, box ->
                OutlinedTextField(
                    value = box.text,
                    onValueChange = { newText ->
                        updateBox(box.id) { it.copy(text = newText) }
                        if (box.text.isBlank()) selectedBoxId = box.id
                    },
                    label = { Text("Text ${index + 1}" + if (box.id == selectedBoxId) " (dragging)" else "") },
                    placeholder = { Text("Enter text") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) selectedBoxId = box.id }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val texts = textBoxes.toPositionedTexts()
                        val encoded = TemplateStateCodec.encodeBoxes(textBoxes)
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
                        val texts = textBoxes.toPositionedTexts()
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
                        val texts = textBoxes.toPositionedTexts()
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

@Composable
private fun DraggableMemeText(
    box: MemeTextBox,
    isSelected: Boolean,
    areaSize: IntSize,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    var selfSize by remember(box.id) { mutableStateOf(IntSize.Zero) }

    val fontSize = if (areaSize.width > 0) {
        with(density) { (areaSize.width * 0.11f).toSp() }
    } else {
        22.sp
    }
    val strokePx = with(density) { 6f }

    val outlineStyle = LocalTextStyle.current.copy(
        color = Color.Black,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = fontSize * 1.1f,
        drawStyle = Stroke(width = strokePx, miter = 10f)
    )
    val fillStyle = LocalTextStyle.current.copy(
        color = Color.White,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = fontSize * 1.1f
    )

    val displayText = box.text.ifBlank { "Text" }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (box.xRatio * areaSize.width).roundToInt() - selfSize.width / 2,
                    (box.yRatio * areaSize.height).roundToInt() - selfSize.height / 2
                )
            }
            .onSizeChanged { selfSize = it }
            .then(
                if (isSelected) {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.9f))
                } else {
                    Modifier
                }
            )
            .pointerInput(box.id) {
                detectDragGestures(
                    onDragStart = { onSelect() }
                ) { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            }
            .padding(2.dp)
    ) {
        Text(displayText, style = outlineStyle, modifier = Modifier.align(Alignment.Center))
        Text(displayText, style = fillStyle, modifier = Modifier.align(Alignment.Center))
    }
}
