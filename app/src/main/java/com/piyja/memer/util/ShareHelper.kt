package com.piyja.memer.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun share(context: Context, file: File) {
        val authority = context.packageName + AUTHORITY_SUFFIX
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "image/jpeg"
        }

        context.startActivity(Intent.createChooser(intent, "Share meme via"))
    }
}