package com.piyja.memer.data

import com.piyja.memer.util.PositionedText

/**
 * A text overlay that applies to a time span of the trimmed clip.
 * Times are relative to the start of the trimmed clip (0 .. clipDurationMs).
 */
data class TextSection(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val color: Long,
    val bold: Boolean,
    val xRatio: Float,
    val yRatio: Float,
    val scale: Float
) {
    fun positionedText(): PositionedText? =
        if (text.isBlank()) {
            null
        } else {
            PositionedText(
                text = text,
                xRatio = xRatio,
                yRatio = yRatio,
                scale = scale,
                color = color,
                bold = bold,
                strike = false
            )
        }
}

/** Editable state for a GIF built from a picked video/GIF. */
data class GifProject(
    val sourcePath: String,
    val durationMs: Long,
    val isGif: Boolean,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val fps: Int,
    val sections: List<TextSection>
)
