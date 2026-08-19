package com.piyja.memer.data

import android.content.Context

object TemplateCatalog {

    private val defaultTemplates = listOf(
        MemeTemplate(id = "1", name = "Drake", imageAssetName = "templates/drake.jpg"),
        MemeTemplate(id = "2", name = "Distracted Boyfriend", imageAssetName = "templates/distracted-boyfriend.jpg"),
        MemeTemplate(id = "3", name = "Expanding Brain", imageAssetName = "templates/expanding-brain.jpg"),
        MemeTemplate(id = "4", name = "Mocking Spongebob", imageAssetName = "templates/mocking-spongebob.jpg"),
        MemeTemplate(id = "5", name = "Woman Yelling at Cat", imageAssetName = "templates/woman-yelling-at-cat.jpg")
    )

    fun getDefaultTemplates(): List<MemeTemplate> = defaultTemplates

    fun getTemplates(context: Context): List<MemeTemplate> {
        val assetTemplates = loadFromAssets(context)
        return if (assetTemplates.isNotEmpty()) assetTemplates else defaultTemplates
    }

    private fun loadFromAssets(context: Context): List<MemeTemplate> {
        return try {
            val files = context.assets.list("templates") ?: emptyArray()
            files
                .filter { it.endsWith(".jpg", true) || it.endsWith(".png", true) }
                .mapIndexed { index, fileName ->
                    MemeTemplate(
                        id = (index + 1).toString(),
                        name = fileName.substringBeforeLast('.').replace('_', ' '),
                        imageAssetName = "templates/$fileName"
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}