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
    fun `getDefaultTemplates returns five templates`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        assertEquals(5, templates.size)
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
    fun `getDefaultTemplates contains Drake template`() {
        val templates = TemplateCatalog.getDefaultTemplates()
        val drake = templates.find { it.name == "Drake" }
        assertNotNull("Drake template should exist in defaults", drake)
    }
}
