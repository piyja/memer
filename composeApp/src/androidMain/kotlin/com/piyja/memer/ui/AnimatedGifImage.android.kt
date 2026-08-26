package com.piyja.memer.ui

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import com.piyja.memer.util.MediaFrame
import com.piyja.memer.util.decodeGifFrames
import com.piyja.memer.util.platformBitmapToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

@Composable
actual fun AnimatedGifImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val drawable = remember(bytes) {
            try {
                val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                val decoded = ImageDecoder.decodeDrawable(source)
                if (decoded is AnimatedImageDrawable) decoded.start()
                decoded
            } catch (e: Exception) {
                null
            }
        }
        if (drawable != null) {
            val scaleType = when (contentScale) {
                ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                else -> ImageView.ScaleType.FIT_CENTER
            }
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        setImageDrawable(drawable)
                        this.scaleType = scaleType
                    }
                },
                modifier = modifier
            )
            return
        }
    }
    LegacyGifImage(bytes, contentDescription, modifier, contentScale)
}

@Composable
private fun LegacyGifImage(
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
