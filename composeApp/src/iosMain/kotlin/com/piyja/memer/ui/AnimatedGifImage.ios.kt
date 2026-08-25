package com.piyja.memer.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.piyja.memer.util.MediaFrame
import com.piyja.memer.util.decodeGifFrames
import com.piyja.memer.util.platformBitmapToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
actual fun AnimatedGifImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val frames by produceState<List<MediaFrame>>(emptyList(), bytes) {
        value = withContext(Dispatchers.Default) { decodeGifFrames(bytes) }
    }
    if (frames.isEmpty()) return
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(frames) {
        if (frames.isEmpty()) return@LaunchedEffect
        var i = 0
        while (true) {
            index = i
            delay(frames[i].durationMs.coerceAtLeast(30))
            i = (i + 1) % frames.size
        }
    }
    val bitmap = frames.getOrNull(index)?.bitmap?.let { platformBitmapToImageBitmap(it) }
    bitmap?.let {
        Image(bitmap = it, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
    }
}
