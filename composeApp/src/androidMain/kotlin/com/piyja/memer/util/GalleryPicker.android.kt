package com.piyja.memer.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private var galleryCallback: ((String?) -> Unit)? = null

@Composable
actual fun rememberGalleryImagePicker(): GalleryImagePicker {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            galleryCallback?.invoke(null)
        } else {
            GlobalScope.launch {
                val path = withContext(Dispatchers.IO) { copyToPrivateStorage(context, uri) }
                galleryCallback?.invoke(path)
            }
        }
    }

    return remember {
        object : GalleryImagePicker {
            override fun launch(onResult: (String?) -> Unit) {
                galleryCallback = onResult
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

private fun copyToPrivateStorage(context: Context, uri: Uri): String? {
    return try {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "user_templates"
        ).apply { mkdirs() }

        val rawName = queryDisplayName(context, uri) ?: "gallery-${System.currentTimeMillis()}.jpg"
        var target = File(dir, rawName.substringAfterLast('/'))
        if (target.exists()) {
            val base = target.nameWithoutExtension
            val ext = target.extension.ifBlank { "jpg" }
            target = File(dir, "$base-${System.currentTimeMillis()}.$ext")
        }

        val input = context.contentResolver.openInputStream(uri)
            ?: return null
        input.use { stream ->
            FileOutputStream(target).use { output -> stream.copyTo(output) }
        }

        target.absolutePath
    } catch (e: Exception) {
        null
    }
}
