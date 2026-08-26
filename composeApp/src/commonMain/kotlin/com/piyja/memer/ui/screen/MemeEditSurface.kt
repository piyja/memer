package com.piyja.memer.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyja.memer.data.MemeTextBox
import kotlin.math.roundToInt

private var nextTextBoxId = 1L

/**
 * Holds the editable text-box state for a single meme (or one GIF frame) and
 * exposes the mutators used by the editing UI. Shared by the static meme editor
 * and the GIF editor so both stay in sync.
 */
class MemeEditState(initialBoxes: List<MemeTextBox>) {
    val textBoxes: SnapshotStateList<MemeTextBox> = mutableStateListOf<MemeTextBox>().apply {
        addAll(initialBoxes.map { it.copy(id = nextTextBoxId++) })
    }
    var selectedBoxId by mutableStateOf(textBoxes.firstOrNull()?.id ?: 0L)
    var textEditActive by mutableStateOf(false)

    private val undoStack = mutableStateListOf<List<MemeTextBox>>()

    fun pushHistory() {
        undoStack.add(textBoxes.map { it.copy() })
        if (undoStack.size > 50) undoStack.removeAt(0)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeAt(undoStack.lastIndex)
        textBoxes.clear()
        val restored = previous.map { it.copy(id = nextTextBoxId++) }
        textBoxes.addAll(restored)
        selectedBoxId = restored.firstOrNull()?.id ?: return
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun select(id: Long) {
        selectedBoxId = id
    }

    fun updateBox(id: Long, transform: (MemeTextBox) -> MemeTextBox) {
        val index = textBoxes.indexOfFirst { it.id == id }
        if (index >= 0) textBoxes[index] = transform(textBoxes[index])
    }

    fun addTextBox() {
        pushHistory()
        val yOffset = ((textBoxes.size % 4) * 0.18f)
        val newY = (0.25f + yOffset).coerceAtMost(0.85f)
        val box = MemeTextBox(id = nextTextBoxId++, text = "", xRatio = 0.5f, yRatio = newY)
        textBoxes.add(box)
        selectedBoxId = box.id
    }

    fun removeSelectedBox() {
        if (textBoxes.size <= 1) return
        pushHistory()
        val index = textBoxes.indexOfFirst { it.id == selectedBoxId }
        textBoxes.removeAt(if (index >= 0) index else 0)
        selectedBoxId = textBoxes.first().id
    }

    fun resetLayout() {
        pushHistory()
        textBoxes.clear()
        val fresh = MemeTextBox(id = nextTextBoxId++, text = "", xRatio = 0.5f, yRatio = 0.2f)
        textBoxes.add(fresh)
        selectedBoxId = fresh.id
    }
}

@Composable
fun MemeEditSurface(
    state: MemeEditState,
    templateImage: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    var imageAreaSize by androidx.compose.runtime.remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (templateImage != null) {
                Box(
                    modifier = Modifier
                        .aspectRatio(templateImage.width.toFloat() / templateImage.height.toFloat())
                        .background(Color.Black.copy(alpha = 0.05f))
                        .onSizeChanged { imageAreaSize = it }
                ) {
                    Image(
                        bitmap = templateImage,
                        contentDescription = "Meme template",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    state.textBoxes.forEach { box ->
                        DraggableMemeText(
                            box = box,
                            isSelected = box.id == state.selectedBoxId,
                            areaSize = imageAreaSize,
                            onSelect = { state.select(box.id) },
                            onEditStart = { state.pushHistory() },
                            onMove = { dx, dy ->
                                state.updateBox(box.id) {
                                    it.copy(
                                        xRatio = (it.xRatio + dx / imageAreaSize.width.coerceAtLeast(1)).coerceIn(0.02f, 0.98f),
                                        yRatio = (it.yRatio + dy / imageAreaSize.height.coerceAtLeast(1)).coerceIn(0.02f, 0.98f)
                                    )
                                }
                            },
                            onResize = { dy ->
                                state.updateBox(box.id) {
                                    it.copy(scale = (it.scale + dy / 200f).coerceIn(0.4f, 3f))
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
            Button(onClick = { state.addTextBox() }, modifier = Modifier.weight(1f)) {
                Text("+ Add text")
            }
            OutlinedButton(
                onClick = { state.removeSelectedBox() },
                enabled = state.textBoxes.size > 1,
                modifier = Modifier.weight(1f)
            ) {
                Text("Delete selected")
            }
            OutlinedButton(onClick = { state.resetLayout() }, modifier = Modifier.weight(1f)) {
                Text("Reset")
            }
        }

        state.textBoxes.forEachIndexed { index, box ->
            OutlinedTextField(
                value = box.text,
                onValueChange = { newText ->
                    if (!state.textEditActive) {
                        state.pushHistory()
                        state.textEditActive = true
                    }
                    state.updateBox(box.id) { it.copy(text = newText) }
                    if (newText.isBlank()) state.select(box.id)
                },
                label = { Text("Text ${index + 1}" + if (box.id == state.selectedBoxId) " (dragging)" else "") },
                placeholder = { Text("Enter text") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused) state.select(box.id) else state.textEditActive = false
                    }
            )
        }

        val selectedBox = state.textBoxes.firstOrNull { it.id == state.selectedBoxId }
        if (selectedBox != null) {
            TextFormatBar(box = selectedBox, onUpdate = { state.updateBox(it.id) { it } })
        }
    }
}

@Composable
private fun TextFormatBar(
    box: MemeTextBox,
    onUpdate: (MemeTextBox) -> Unit
) {
    val palette = listOf(
        Color.White, Color.Black, Color.Red, Color.Yellow, Color.Blue, Color.Green
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Text style", style = MaterialTheme.typography.labelMedium)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            palette.forEach { color ->
                val isSelected = box.color == color.value.toLong()
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(color, RoundedCornerShape(6.dp))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onUpdate(box.copy(color = color.value.toLong())) }
                )
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
    onEditStart: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float) -> Unit
) {
    val density = LocalDensity.current
    var selfSize by androidx.compose.runtime.remember(box.id) { mutableStateOf(IntSize.Zero) }

    val baseSize = if (areaSize.width > 0) {
        with(density) { (areaSize.width * 0.11f).toSp() }
    } else {
        22.sp
    }
    val fontSize = baseSize * box.scale
    val strokePx = with(density) { 6f }

    val outlineStyle = LocalTextStyle.current.copy(
        color = Color.Black,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = fontSize * 1.1f,
        drawStyle = Stroke(width = strokePx, miter = 10f)
    )
    val fillStyle = LocalTextStyle.current.copy(
        color = Color(box.color),
        fontSize = fontSize,
        fontWeight = if (box.bold) FontWeight.ExtraBold else FontWeight.Normal,
        textDecoration = if (box.strike) TextDecoration.LineThrough else null,
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
                    onDragStart = { onSelect(); onEditStart() }
                ) { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            }
            .padding(2.dp)
    ) {
        Text(displayText, style = outlineStyle, modifier = Modifier.align(Alignment.Center))
        Text(displayText, style = fillStyle, modifier = Modifier.align(Alignment.Center))

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.7f))
                    .pointerInput(box.id) {
                        detectDragGestures(
                            onDragStart = { onEditStart() }
                        ) { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.y)
                        }
                    }
            )
        }
    }
}
