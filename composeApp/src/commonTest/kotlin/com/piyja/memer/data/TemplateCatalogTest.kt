package com.piyja.memer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateCatalogTest {

    @Test
    fun `getDefaultTemplates returns non-empty list`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        assertTrue("Default template list should not be empty", templates.isNotEmpty())
    }

    @Test
    fun `getDefaultTemplates returns the bundled templates`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        assertTrue("Default template list should have multiple bundled templates", templates.size > 1)
        val names = templates.map { it.name }
        assertTrue("Should include a standard meme template", "Drake Hotline Bling" in names)
    }

    @Test
    fun `all templates have unique ids`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        val ids = templates.map { it.id }
        assertEquals("IDs should be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `all templates have non-blank names`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        templates.forEach { template ->
            assertFalse(
                "Template ${template.id} should have a non-blank name",
                template.name.isBlank()
            )
        }
    }

    @Test
    fun `all templates have non-blank asset names`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        templates.forEach { template ->
            assertFalse(
                "Template ${template.id} should have a non-blank asset name",
                template.imageAssetName.isBlank()
            )
        }
    }

    @Test
    fun `all template asset names end with jpg or png`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        templates.forEach { template ->
            val lower = template.imageAssetName.lowercase()
            assertTrue(
                "Template ${template.id} asset should be jpg or png, got ${template.imageAssetName}",
                lower.endsWith(".jpg") || lower.endsWith(".png")
            )
        }
    }

    @Test
    fun `getDefaultTemplates does not contain removed No Yes template`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        val noYes = templates.find { it.name == "No Yes" }
        assertEquals("No Yes template should be removed from defaults", null, noYes)
    }

    @Test
    fun `search returns all templates for blank query`() {
        val results = TemplateCatalog.search("")
        assertEquals(TemplateCatalog.getTemplates().size, results.size)
        assertEquals(TemplateCatalog.search("   ").size, results.size)
    }

    @Test
    fun `search matches by name`() {
        val results = TemplateCatalog.search("drake")
        assertTrue("Should match Drake Hotline Bling by name", results.any { it.name == "Drake Hotline Bling" })
    }

    @Test
    fun `search matches by tag`() {
        val results = TemplateCatalog.search("cheating")
        assertTrue("Should match Distracted Boyfriend via tag", results.any { it.name == "Distracted Boyfriend" })
    }

    @Test
    fun `search narrows with multiple tokens`() {
        val single = TemplateCatalog.search("bernie").size
        val multi = TemplateCatalog.search("bernie asking").size
        assertTrue("Multi-token query should be narrower or equal", multi <= single)
        assertTrue("Should still find a Bernie template", multi > 0)
    }

    @Test
    fun `search is case insensitive`() {
        assertEquals(TemplateCatalog.search("DRAKE").size, TemplateCatalog.search("drake").size)
    }

    @Test
    fun `addCustomTemplate appends to getTemplates`() {
        val before = TemplateCatalog.getTemplates().size
        val added = TemplateCatalog.addCustomTemplate("My Pic", "/data/user/0/pic1.jpg")

        assertTrue(
            "getTemplates should contain the custom template",
            TemplateCatalog.getTemplates().contains(added)
        )
        assertEquals(before + 1, TemplateCatalog.getTemplates().size)
        assertEquals("My Pic", added.name)
        assertEquals("/data/user/0/pic1.jpg", added.imageAssetName)
        assertFalse(
            "defaults must stay untouched",
            TemplateCatalog.getDefaultTemplates().contains(added)
        )
    }
}
