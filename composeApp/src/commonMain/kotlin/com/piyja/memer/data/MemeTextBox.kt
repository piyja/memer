package com.piyja.memer.data

import com.piyja.memer.util.PositionedText

data class MemeTextBox(
    val id: Long,
    val text: String,
    val xRatio: Float,
    val yRatio: Float,
    val scale: Float = 1f
)

fun List<MemeTextBox>.toPositionedTexts(): List<PositionedText> =
    filter { it.text.isNotBlank() }
        .map { PositionedText(it.text, it.xRatio, it.yRatio, it.scale) }
