package com.piyja.memer.util

import androidx.compose.ui.graphics.ImageBitmap

expect class PlatformBitmap

data class PositionedText(
    val text: String,
    val xRatio: Float,
    val yRatio: Float
)

expect fun loadTemplateBitmap(assetPath: String): PlatformBitmap

expect fun renderMeme(
    bitmap: PlatformBitmap,
    texts: List<PositionedText>
): PlatformBitmap

expect fun platformBitmapToImageBitmap(bitmap: PlatformBitmap): ImageBitmap

expect fun saveMemeToGallery(bitmap: PlatformBitmap): String?

expect fun stageShareableImage(bitmap: PlatformBitmap): String

expect fun shareMemeImage(filePath: String)

expect fun copyMemeToClipboard(filePath: String)

expect fun loadTemplateState(templateId: String): String?

expect fun saveTemplateState(templateId: String, state: String)

expect fun clearTemplateState(templateId: String)
