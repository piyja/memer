package com.piyja.memer.data

import androidx.compose.runtime.mutableStateOf
import kotlin.random.Random

object TemplateCatalog {

    private val defaultTemplates = listOf(
        MemeTemplate(id = "181913649", name = "Drake Hotline Bling", imageAssetName = "templates/181913649.jpg",
            tags = listOf("drake", "hotline", "bling", "preference", "reject", "accept", "music")),
        MemeTemplate(id = "87743020", name = "Two Buttons", imageAssetName = "templates/87743020.jpg",
            tags = listOf("buttons", "choice", "decision", "both", "dilemma")),
        MemeTemplate(id = "112126428", name = "Distracted Boyfriend", imageAssetName = "templates/112126428.jpg",
            tags = listOf("distracted", "boyfriend", "cheating", "attention", "reaction", "woman")),
        MemeTemplate(id = "222403160", name = "Bernie I Am Once Again Asking For Your Support", imageAssetName = "templates/222403160.jpg",
            tags = listOf("bernie", "sanders", "asking", "support", "politics")),
        MemeTemplate(id = "217743513", name = "UNO Draw 25 Cards", imageAssetName = "templates/217743513.jpg",
            tags = listOf("uno", "cards", "draw", "game")),
        MemeTemplate(id = "124822590", name = "Left Exit 12 Off Ramp", imageAssetName = "templates/124822590.jpg",
            tags = listOf("exit", "ramp", "highway", "wrong", "turn", "direction")),
        MemeTemplate(id = "252600902", name = "Always Has Been", imageAssetName = "templates/252600902.png",
            tags = listOf("always", "been", "earth", "flat", "space", "astronaut")),
        MemeTemplate(id = "322841258", name = "Anakin Padme 4 Panel", imageAssetName = "templates/322841258.png",
            tags = listOf("anakin", "padme", "star wars", "plan", "4 panel")),
        MemeTemplate(id = "135256802", name = "Epic Handshake", imageAssetName = "templates/135256802.jpg",
            tags = listOf("handshake", "epic", "cool", "connected", "deal")),
        MemeTemplate(id = "131087935", name = "Running Away Balloon", imageAssetName = "templates/131087935.jpg",
            tags = listOf("running", "away", "balloon", "escape", "dog")),
        MemeTemplate(id = "131940431", name = "Gru's Plan", imageAssetName = "templates/131940431.jpg",
            tags = listOf("gru", "plan", "despicable me", "steps", "fail")),
        MemeTemplate(id = "80707627", name = "Sad Pablo Escobar", imageAssetName = "templates/80707627.jpg",
            tags = listOf("pablo", "escobar", "sad", "narcos", "crying")),
        MemeTemplate(id = "4087833", name = "Waiting Skeleton", imageAssetName = "templates/4087833.jpg",
            tags = listOf("waiting", "skeleton", "impatient", "death", "time")),
        MemeTemplate(id = "129242436", name = "Change My Mind", imageAssetName = "templates/129242436.jpg",
            tags = listOf("change my mind", "opinion", "debate", "sign")),
        MemeTemplate(id = "97984", name = "Disaster Girl", imageAssetName = "templates/97984.jpg",
            tags = listOf("disaster", "girl", "fire", "smile", "chaos")),
        MemeTemplate(id = "309868304", name = "Trade Offer", imageAssetName = "templates/309868304.jpg",
            tags = listOf("trade", "offer", "deal", "exchange")),
        MemeTemplate(id = "224015000", name = "Bernie Sanders Once Again Asking", imageAssetName = "templates/224015000.png",
            tags = listOf("bernie", "sanders", "asking", "politics")),
        MemeTemplate(id = "161865971", name = "Marked Safe From", imageAssetName = "templates/161865971.jpg",
            tags = listOf("marked safe", "safe", "facebook", "status", "joke")),
        MemeTemplate(id = "101470", name = "Ancient Aliens", imageAssetName = "templates/101470.jpg",
            tags = listOf("ancient", "aliens", "history", "theory")),
        MemeTemplate(id = "124055727", name = "Y'all Got Any More Of That", imageAssetName = "templates/124055727.jpg",
            tags = listOf("more", "that", "cowboy", "want")),
        MemeTemplate(id = "438680", name = "Batman Slapping Robin", imageAssetName = "templates/438680.jpg",
            tags = listOf("batman", "robin", "slap", "argument", "fight")),
        MemeTemplate(id = "91538330", name = "X, X Everywhere", imageAssetName = "templates/91538330.jpg",
            tags = listOf("everywhere", "x", "find", "pattern")),
        MemeTemplate(id = "102156234", name = "Mocking Spongebob", imageAssetName = "templates/102156234.jpg",
            tags = listOf("mocking", "spongebob", "sarcasm")),
        MemeTemplate(id = "61579", name = "One Does Not Simply", imageAssetName = "templates/61579.jpg",
            tags = listOf("one does not simply", "boromir", "lord of the rings", "lotr", "walk"))
    )

    private val customTemplates = mutableStateOf(emptyList<MemeTemplate>())

    fun getDefaultTemplates(): List<MemeTemplate> = defaultTemplates

    fun getTemplates(): List<MemeTemplate> = defaultTemplates + customTemplates.value

    fun search(query: String): List<MemeTemplate> {
        val all = getTemplates()
        val trimmed = query.trim().lowercase()
        if (trimmed.isBlank()) return all
        val tokens = trimmed.split(Regex("\\s+"))
        return all.filter { template ->
            val haystack = (template.name + " " + template.tags.joinToString(" ")).lowercase()
            tokens.all { token -> haystack.contains(token) }
        }
    }

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
