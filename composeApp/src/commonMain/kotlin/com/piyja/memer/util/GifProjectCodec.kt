package com.piyja.memer.util

import com.piyja.memer.data.GifProject
import com.piyja.memer.data.TextSection
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val MAGIC = "GIFPROJECT 1"

@OptIn(ExperimentalEncodingApi::class)
fun encodeGifProject(project: GifProject): String = buildString {
    appendLine(MAGIC)
    appendLine(project.sourcePath)
    appendLine(project.durationMs)
    appendLine(if (project.isGif) 1 else 0)
    appendLine(project.trimStartMs)
    appendLine(project.trimEndMs)
    appendLine(project.fps)
    appendLine(project.sections.size)
    for (section in project.sections) {
        appendLine(section.id)
        appendLine(section.startMs)
        appendLine(section.endMs)
        appendLine(Base64.Default.encode(section.text.encodeToByteArray()))
        appendLine(section.color.toString(16).padStart(8, '0'))
        appendLine(if (section.bold) 1 else 0)
        appendLine(section.xRatio)
        appendLine(section.yRatio)
        appendLine(section.scale)
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun decodeGifProject(raw: String): GifProject? {
    val lines = raw.split("\n")
    if (lines.firstOrNull() != MAGIC) return null

    var index = 1
    fun next(): String? {
        return lines.getOrNull(index++)
    }

    val sourcePath = next() ?: return null
    val durationMs = next()?.toLongOrNull() ?: return null
    val isGif = next()?.toIntOrNull() == 1
    val trimStartMs = next()?.toLongOrNull() ?: return null
    val trimEndMs = next()?.toLongOrNull() ?: return null
    val fps = next()?.toIntOrNull() ?: return null
    val sectionCount = next()?.toIntOrNull() ?: return null

    val sections = mutableListOf<TextSection>()
    repeat(sectionCount) {
        val id = next() ?: return null
        val startMs = next()?.toLongOrNull() ?: return null
        val endMs = next()?.toLongOrNull() ?: return null
        val text = runCatching {
            Base64.Default.decode(next() ?: return null).decodeToString()
        }.getOrNull() ?: return null
        val color = next()?.let { runCatching { it.toLong(16) }.getOrNull() } ?: return null
        val bold = next()?.toIntOrNull() == 1
        val xRatio = next()?.toFloatOrNull() ?: return null
        val yRatio = next()?.toFloatOrNull() ?: return null
        val scale = next()?.toFloatOrNull() ?: return null
        sections.add(
            TextSection(
                id = id,
                startMs = startMs,
                endMs = endMs,
                text = text,
                color = color,
                bold = bold,
                xRatio = xRatio,
                yRatio = yRatio,
                scale = scale
            )
        )
    }

    return GifProject(
        sourcePath = sourcePath,
        durationMs = durationMs,
        isGif = isGif,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        fps = fps,
        sections = sections
    )
}
