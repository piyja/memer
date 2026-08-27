package com.piyja.memer.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piyja.memer.data.GifGallery
import com.piyja.memer.data.GifMeme
import com.piyja.memer.data.GifProject
import com.piyja.memer.data.TextSection
import com.piyja.memer.data.loadGifBytes
import com.piyja.memer.util.GifEncoder
import com.piyja.memer.util.MediaFrame
import com.piyja.memer.util.MediaInfo
import com.piyja.memer.util.RgbaImage
import com.piyja.memer.util.bitmapToRgba
import com.piyja.memer.util.copyGifToClipboard
import com.piyja.memer.util.decodeGifProject
import com.piyja.memer.util.encodeGifProject
import com.piyja.memer.util.extractFrames
import com.piyja.memer.util.getMediaInfo
import com.piyja.memer.util.platformBitmapToImageBitmap
import com.piyja.memer.util.renderMeme
import com.piyja.memer.util.shareGifFile
import com.piyja.memer.util.stageShareableGif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val GIF_TEXT_COLORS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFF0000, 0xFFFFFF00, 0xFF0000FF, 0xFF00FF00
)

private fun oneDecimal(v: Double): String {
    val r = kotlin.math.round(v * 10).toInt()
    return "${r / 10}.${kotlin.math.abs(r) % 10}"
}

private fun twoDecimal(v: Double): String {
    val r = kotlin.math.round(v * 100).toInt()
    val frac = kotlin.math.abs(r) % 100
    return "${r / 100}.${if (frac < 10) "0$frac" else frac}"
}

private fun secs(ms: Float): String = oneDecimal(ms / 1000.0)

private fun makeSection(startMs: Long, endMs: Long): TextSection = TextSection(
    id = "s-" + Random.nextLong().toString(16),
    startMs = startMs,
    endMs = endMs,
    text = "",
    color = 0xFFFFFFFF,
    bold = true,
    xRatio = 0.5f,
    yRatio = 0.5f,
    scale = 1f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifVideoEditorScreen(
    initialSourcePath: String? = null,
    initialProjectJson: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sourcePath by remember { mutableStateOf(initialSourcePath) }
    var mediaInfo by remember { mutableStateOf<MediaInfo?>(null) }
    var trimStartMs by remember { mutableStateOf(0f) }
    var trimEndMs by remember { mutableStateOf(0f) }
    var fps by remember { mutableStateOf(10f) }
    var sections by remember { mutableStateOf<List<TextSection>>(emptyList()) }
    var frames by remember { mutableStateOf<List<MediaFrame>>(emptyList()) }
    var isExtracting by remember { mutableStateOf(false) }
    var previewPlaying by remember { mutableStateOf(false) }
    var previewIndex by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val clipDurationMs = (trimEndMs - trimStartMs).toLong().coerceAtLeast(0L)

    LaunchedEffect(Unit) {
        if (initialProjectJson != null) {
            val p = decodeGifProject(initialProjectJson)
            if (p != null) {
                sourcePath = p.sourcePath
                mediaInfo = MediaInfo(p.durationMs, p.isGif)
                trimStartMs = p.trimStartMs.toFloat()
                trimEndMs = p.trimEndMs.toFloat()
                fps = p.fps.toFloat()
                sections = p.sections
                return@LaunchedEffect
            }
        }
        if (sourcePath != null) {
            val info = getMediaInfo(sourcePath!!)
            mediaInfo = info
            trimStartMs = 0f
            trimEndMs = info.durationMs.toFloat().coerceAtLeast(1f)
            fps = 10f
            sections = listOf(makeSection(0L, info.durationMs))
        }
    }

    LaunchedEffect(trimStartMs, trimEndMs, fps, sourcePath) {
        val path = sourcePath ?: return@LaunchedEffect
        val info = mediaInfo ?: return@LaunchedEffect
        if (trimEndMs <= trimStartMs) return@LaunchedEffect
        delay(400)
        isExtracting = true
        val result = withContext(Dispatchers.Default) {
            extractFrames(path, trimStartMs.toLong(), trimEndMs.toLong(), fps.toInt())
        }
        frames = result
        previewIndex = 0
        isExtracting = false
    }

    LaunchedEffect(previewPlaying) {
        if (!previewPlaying || frames.isEmpty()) return@LaunchedEffect
        var i = 0
        while (true) {
            if (i >= frames.size) i = 0
            previewIndex = i
            delay(frames[i].durationMs.coerceAtLeast(30))
            i = (i + 1) % frames.size
        }
    }

    fun generate(): GifMeme? {
        val path = sourcePath ?: return null
        if (frames.isEmpty()) return null
        val rgbaFrames = mutableListOf<RgbaImage>()
        val durations = mutableListOf<Int>()
        var cum = 0L
        for (frame in frames) {
            val rel = cum
            val section = sections.firstOrNull { rel >= it.startMs && rel < it.endMs }
            val base = section?.positionedText()?.let { pt -> renderMeme(frame.bitmap, listOf(pt)) }
                ?: frame.bitmap
            rgbaFrames.add(bitmapToRgba(base))
            durations.add(frame.durationMs.toInt())
            cum += frame.durationMs
        }
        val bytes = GifEncoder.encodeAnimatedGif(rgbaFrames, durations)
        val json = encodeGifProject(
            GifProject(
                sourcePath = path,
                durationMs = mediaInfo?.durationMs ?: 0L,
                isGif = mediaInfo?.isGif ?: false,
                trimStartMs = trimStartMs.toLong(),
                trimEndMs = trimEndMs.toLong(),
                fps = fps.toInt(),
                sections = sections
            )
        )
        return GifGallery.add(
            title = "Video GIF",
            gifBytes = bytes,
            thumb = frames.first().bitmap,
            projectJson = json
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    if (sourcePath != null && mediaInfo != null) {
                        val durationSecs = oneDecimal(mediaInfo!!.durationMs / 1000.0)
                        val totalMs = mediaInfo!!.durationMs.toFloat().coerceAtLeast(1f)

                        SettingsCard(title = "Trim") {
                            Text("${secs(trimStartMs)}s \u2013 ${secs(trimEndMs)}s", style = MaterialTheme.typography.bodySmall)
                            RangeSlider(
                                value = trimStartMs..trimEndMs,
                                onValueChange = { range ->
                                    trimStartMs = range.start.coerceAtMost(range.endInclusive - 100f)
                                    trimEndMs = range.endInclusive.coerceAtLeast(range.start + 100f)
                                },
                                valueRange = 0f..totalMs
                            )
                        }

                        SettingsCard(title = "Playback") {
                            Text("${fps.toInt()} fps", style = MaterialTheme.typography.bodySmall)
                            Slider(value = fps, onValueChange = { fps = it }, valueRange = 2f..30f, steps = 28)
                            if (isExtracting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Extracting frames...", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text("Clip: ${secs(clipDurationMs.toFloat())}s \u2022 ${frames.size} frames", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Text Sections",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        sections.forEachIndexed { idx, sec ->
                            SectionEditor(
                                section = sec,
                                clipDurationMs = clipDurationMs,
                                onUpdate = { updated ->
                                    sections = sections.toMutableList().also { it[idx] = updated }
                                },
                                onRemove = { sections = sections.filter { it.id != sec.id } }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val start = (clipDurationMs / (sections.size + 1))
                                sections = sections + makeSection(start, clipDurationMs)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Add Section") }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text("Edit GIF") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, "Settings")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val saved = generate()
                                val bytes = saved?.let { GifGallery.get(it.id)?.loadGifBytes() }
                                if (bytes != null) shareGifFile(stageShareableGif(bytes))
                            }
                        }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                        IconButton(onClick = {
                            val saved = generate()
                            val bytes = saved?.let { GifGallery.get(it.id)?.loadGifBytes() }
                            if (bytes != null) copyGifToClipboard(stageShareableGif(bytes))
                            status = "Copied to clipboard"
                        }) {
                            Icon(Icons.Default.ContentCopy, "Copy")
                        }
                        Button(onClick = {
                            status = if (generate() != null) "Saved to Your GIFs" else "Failed"
                        }) {
                            Text("Save")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (sourcePath == null || mediaInfo == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading media\u2026")
                    }
                    return@Scaffold
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val img: ImageBitmap? =
                        frames.getOrNull(if (previewPlaying) previewIndex else 0)
                            ?.bitmap?.let { platformBitmapToImageBitmap(it) }
                    img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }

                    val activeSection = remember(previewIndex, previewPlaying, sections, frames) {
                        val idx = if (previewPlaying) previewIndex else 0
                        var cum = 0L
                        for (j in 0 until idx) cum += frames[j].durationMs
                        sections.firstOrNull { cum >= it.startMs && cum < it.endMs }
                    }
                    activeSection?.positionedText()?.let { pt ->
                        Text(
                            text = pt.text.trim().uppercase(),
                            color = Color(pt.color),
                            fontSize = (16 + 16 * pt.scale).sp,
                            fontWeight = if (pt.bold) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                .absoluteOffset(
                                    x = ((pt.xRatio - 0.5f) * 300).dp,
                                    y = ((pt.yRatio - 0.5f) * 160).dp
                                )
                        )
                    }

                    IconButton(
                        onClick = { previewPlaying = !previewPlaying },
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                    ) {
                        Text(
                            if (previewPlaying) "\u23F9" else "\u25B6",
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        "${if (frames.isNotEmpty()) (if (previewPlaying) previewIndex else 0) + 1 else 0}/${frames.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    )
                }

                if (frames.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.8f)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(frames.size) { i ->
                            val img = platformBitmapToImageBitmap(frames[i].bitmap)
                            Image(
                                bitmap = img,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable { previewPlaying = false; previewIndex = i }
                                    .border(
                                        if (i == previewIndex && !previewPlaying) 2.dp else 0.dp,
                                        Color.White
                                    )
                            )
                        }
                    }
                }

                status?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun SectionEditor(
    section: TextSection,
    clipDurationMs: Long,
    onUpdate: (TextSection) -> Unit,
    onRemove: () -> Unit
) {
    var text by remember(section.id) { mutableStateOf(section.text) }
    var expanded by remember(section.id) { mutableStateOf(false) }
    val maxMs = clipDurationMs.toFloat().coerceAtLeast(1f)

    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text.ifEmpty { "New Section" },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                    Text(
                        "${secs(section.startMs.toFloat())}s \u2013 ${secs(section.endMs.toFloat())}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, "Remove section", Modifier.size(20.dp))
                }
            }

            if (expanded) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            onUpdate(section.copy(text = it))
                        },
                        label = { Text("Text") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Start: ${secs(section.startMs.toFloat())}s", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = section.startMs.toFloat(),
                        onValueChange = { onUpdate(section.copy(startMs = it.toLong().coerceAtMost(section.endMs - 50))) },
                        valueRange = 0f..maxMs
                    )
                    Text("End: ${secs(section.endMs.toFloat())}s", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = section.endMs.toFloat(),
                        onValueChange = { onUpdate(section.copy(endMs = it.toLong().coerceAtLeast(section.startMs + 50))) },
                        valueRange = 0f..maxMs
                    )

                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GIF_TEXT_COLORS.forEach { c ->
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(Color(c))
                                    .clickable { onUpdate(section.copy(color = c)) }
                                    .border(if (section.color == c) 2.dp else 0.dp, Color.White)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = section.bold, onCheckedChange = { onUpdate(section.copy(bold = it)) })
                        Text("Bold")
                    }

                    Text("X: " + twoDecimal(section.xRatio.toDouble()), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = section.xRatio,
                        onValueChange = { onUpdate(section.copy(xRatio = it)) },
                        valueRange = 0f..1f
                    )
                    Text("Y: " + twoDecimal(section.yRatio.toDouble()), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = section.yRatio,
                        onValueChange = { onUpdate(section.copy(yRatio = it)) },
                        valueRange = 0f..1f
                    )
                    Text("Size: " + twoDecimal(section.scale.toDouble()), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = section.scale,
                        onValueChange = { onUpdate(section.copy(scale = it)) },
                        valueRange = 0.5f..3f
                    )
                }
            }
        }
    }
}
