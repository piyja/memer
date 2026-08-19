package com.piyja.memer.util

import androidx.compose.ui.graphics.ImageBitmap

expect class PlatformBitmap

expect fun loadTemplateBitmap(assetPath: String): PlatformBitmap

expect fun renderMeme(
    bitmap: PlatformBitmap,
    topText: String,
    bottomText: String
): PlatformBitmap

expect fun platformBitmapToImageBitmap(bitmap: PlatformBitmap): ImageBitmap

expect fun saveMemeImage(bitmap: PlatformBitmap): String

expect fun shareMemeImage(filePath: String)

expect fun copyMemeToClipboard(filePath: String)
