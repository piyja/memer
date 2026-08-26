package com.piyja.memer.data

data class CreatedMeme(
    val id: String,
    val templateId: String,
    val templateName: String,
    val templateImageAssetName: String,
    val imageFileName: String,
    val encodedTexts: String,
    val createdAt: Long
)

fun CreatedMeme.toTemplate(): MemeTemplate =
    MemeTemplate(
        id = templateId,
        name = templateName,
        imageAssetName = templateImageAssetName
    )

private const val TEXTS_MARKER = "%%MEME_TEXTS%%"

fun encodeCreatedMeme(meme: CreatedMeme): String = buildString {
    appendLine(meme.id)
    appendLine(meme.templateId)
    appendLine(meme.templateName)
    appendLine(meme.templateImageAssetName)
    appendLine(meme.imageFileName)
    appendLine(meme.createdAt.toString())
    appendLine(TEXTS_MARKER)
    append(meme.encodedTexts)
}

fun decodeCreatedMeme(raw: String): CreatedMeme? {
    val lines = raw.split("\n")
    val markerIndex = lines.indexOf(TEXTS_MARKER)
    if (markerIndex < 6) return null
    val id = lines[0]
    val templateId = lines[1]
    val templateName = lines[2]
    val templateImageAssetName = lines[3]
    val imageFileName = lines[4]
    val createdAt = lines[5].toLongOrNull() ?: return null
    val encodedTexts = lines.drop(markerIndex + 1).joinToString("\n")
    return CreatedMeme(
        id = id,
        templateId = templateId,
        templateName = templateName,
        templateImageAssetName = templateImageAssetName,
        imageFileName = imageFileName,
        encodedTexts = encodedTexts,
        createdAt = createdAt
    )
}
