package com.piyja.memer.data

object TemplateCatalog {

    private val defaultTemplates = listOf(
        MemeTemplate(id = "1", name = "Drake", imageAssetName = "templates/drake.jpg"),
        MemeTemplate(id = "2", name = "Distracted Boyfriend", imageAssetName = "templates/distracted-boyfriend.jpg"),
        MemeTemplate(id = "3", name = "Expanding Brain", imageAssetName = "templates/expanding-brain.jpg"),
        MemeTemplate(id = "4", name = "Mocking Spongebob", imageAssetName = "templates/mocking-spongebob.jpg"),
        MemeTemplate(id = "5", name = "Woman Yelling at Cat", imageAssetName = "templates/woman-yelling-at-cat.jpg")
    )

    fun getDefaultTemplates(): List<MemeTemplate> = defaultTemplates

    fun getTemplates(): List<MemeTemplate> = defaultTemplates
}
