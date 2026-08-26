package com.piyja.memer.util

import com.piyja.memer.data.MemeTextBox
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object TemplateStateCodec {

    fun encodeBoxes(boxes: List<MemeTextBox>): String =
        boxes.joinToString("\n") { box ->
            val encodedText = Base64.encode(box.text.encodeToByteArray())
            "$encodedText|${box.xRatio}|${box.yRatio}|${box.scale}|${box.color}|${box.bold}|${box.strike}"
        }

    fun decodeBoxes(raw: String?): List<MemeTextBox> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split('|')
            if (parts.size < 3) return@mapNotNull null
            val text = runCatching { Base64.decode(parts[0]).decodeToString() }.getOrNull()
                ?: return@mapNotNull null
            val x = parts[1].toFloatOrNull() ?: return@mapNotNull null
            val y = parts[2].toFloatOrNull() ?: return@mapNotNull null
            val scale = if (parts.size >= 4) parts[3].toFloatOrNull() ?: 1f else 1f
            val color = if (parts.size >= 5) parts[4].toLongOrNull()?.toInt() ?: 0xFFFFFFFF.toInt() else 0xFFFFFFFF.toInt()
            val bold = if (parts.size >= 6) parts[5].toBoolean() else true
            val strike = if (parts.size >= 7) parts[6].toBoolean() else false
            MemeTextBox(
                id = 0L,
                text = text,
                xRatio = x,
                yRatio = y,
                scale = scale,
                color = color.toLong() and 0xFFFFFFFF,
                bold = bold,
                strike = strike
            )
        }
    }
}
