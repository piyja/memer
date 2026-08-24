package com.piyja.memer.util

import com.piyja.memer.util.AndroidContextHolder.appContext
import java.io.File

private fun stateDir(): File =
    File(appContext.filesDir, "templates_state").apply { mkdirs() }

private fun safeFileName(templateId: String): String =
    templateId.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")

private fun stateFile(templateId: String): File =
    File(stateDir(), "${safeFileName(templateId)}.txt")

actual fun loadTemplateState(templateId: String): String? {
    val file = stateFile(templateId)
    return if (file.exists()) file.readText() else null
}

actual fun saveTemplateState(templateId: String, state: String) {
    stateFile(templateId).writeText(state)
}

actual fun clearTemplateState(templateId: String) {
    stateFile(templateId).delete()
}
