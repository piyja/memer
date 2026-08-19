package com.piyja.memer.util

object MemeText {

    fun formatMemeText(text: String): String {
        return text.trim().uppercase()
    }

    fun calculateTextSize(imageWidth: Int, text: String, measureTextWidth: (String, Float) -> Float): Float {
        if (text.isEmpty()) return 0f
        val targetWidth = imageWidth * 0.8f
        var size = imageWidth * 0.12f
        while (measureTextWidth(text, size) > targetWidth && size > 12f) {
            size -= 2f
        }
        return size
    }
}
