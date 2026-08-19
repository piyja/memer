package com.piyja.memer.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.FileProvider
import java.io.File

object ClipboardHelper {

    private const val AUTHORITY_SUFFIX = ".fileprovider"
    private const val CLIP_LABEL = "Meme Image"

    fun copyToClipboard(context: Context, file: File) {
        val authority = context.packageName + AUTHORITY_SUFFIX
        val uri = FileProvider.getUriForFile(context, authority, file)

        val clipData = ClipData.newUri(context.contentResolver, CLIP_LABEL, uri)
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(clipData)
    }
}