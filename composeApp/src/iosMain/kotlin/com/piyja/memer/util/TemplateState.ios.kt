package com.piyja.memer.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
private fun stateFilePath(templateId: String): String? {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documentsDir = paths.firstOrNull() as? String ?: return null
    val dirPath = "$documentsDir/templates_state"
    if (!NSFileManager.defaultManager.fileExistsAtPath(dirPath)) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            dirPath,
            withIntermediateDirectories = true,
            attributes = null,
            null
        )
    }
    val safeName = templateId.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
    return "$dirPath/$safeName.txt"
}

@OptIn(ExperimentalForeignApi::class)
actual fun loadTemplateState(templateId: String): String? {
    val path = stateFilePath(templateId) ?: return null
    val data = NSData.create(contentsOfFile = path) ?: return null
    return data.toByteArray().decodeToString()
}

@OptIn(ExperimentalForeignApi::class)
actual fun saveTemplateState(templateId: String, state: String) {
    val path = stateFilePath(templateId) ?: return
    NSString.create(string = state).writeToFile(
        path,
        atomically = true,
        encoding = NSUTF8StringEncoding,
        error = null
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun clearTemplateState(templateId: String) {
    val path = stateFilePath(templateId) ?: return
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}
