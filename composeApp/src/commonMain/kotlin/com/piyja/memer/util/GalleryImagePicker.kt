package com.piyja.memer.util

import androidx.compose.runtime.Composable

interface GalleryImagePicker {
    fun launch(onResult: (String?) -> Unit)
}

@Composable
expect fun rememberGalleryImagePicker(): GalleryImagePicker

@Composable
expect fun rememberVideoGifPicker(): GalleryImagePicker
