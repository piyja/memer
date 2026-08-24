package com.piyja.memer.data

import androidx.compose.runtime.mutableStateOf
import kotlin.random.Random

object TemplateCatalog {

    private val defaultTemplates = listOf(
        MemeTemplate(id = "1", name = "No Yes", imageAssetName = "templates/noYes.png")
    )

    private val customTemplates = mutableStateOf(emptyList<MemeTemplate>())

    fun getDefaultTemplates(): List<MemeTemplate> = defaultTemplates

    fun getTemplates(): List<MemeTemplate> = defaultTemplates + customTemplates.value

    fun addCustomTemplate(name: String, imagePath: String): MemeTemplate {
        val template = MemeTemplate(
            id = "gallery-" + Random.nextLong().toString(16),
            name = name,
            imageAssetName = imagePath
        )
        customTemplates.value += template
        return template
    }
}
