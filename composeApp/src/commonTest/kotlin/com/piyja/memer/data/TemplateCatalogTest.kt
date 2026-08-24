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
    fun `getDefaultTemplates returns one template`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        assertEquals(1, templates.size)
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
    fun `getDefaultTemplates contains No Yes template`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        val noYes = templates.find { it.name == "No Yes" }
        assertNotNull("No Yes template should exist in defaults", noYes)
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
