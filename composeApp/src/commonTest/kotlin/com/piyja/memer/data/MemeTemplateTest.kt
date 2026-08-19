package com.piyja.memer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MemeTemplateTest {

    @Test
    fun `template stores all properties correctly`() {
        val template = MemeTemplate(
            id = "42",
            name = "Distracted Boyfriend",
            imageAssetName = "templates/distracted-boyfriend.jpg"
        )
        assertEquals("42", template.id)
        assertEquals("Distracted Boyfriend", template.name)
        assertEquals("templates/distracted-boyfriend.jpg", template.imageAssetName)
    }

    @Test
    fun `two templates with same values are equal`() {
        val t1 = MemeTemplate("1", "Drake", "drake.jpg")
        val t2 = MemeTemplate("1", "Drake", "drake.jpg")
        assertEquals(t1, t2)
        assertEquals(t1.hashCode(), t2.hashCode())
    }

    @Test
    fun `two templates with different ids are not equal`() {
        val t1 = MemeTemplate("1", "Drake", "drake.jpg")
        val t2 = MemeTemplate("2", "Drake", "drake.jpg")
        assertNotEquals(t1, t2)
    }

    @Test
    fun `copy produces a template with updated field`() {
        val original = MemeTemplate("1", "Drake", "drake.jpg")
        val copied = original.copy(name = "Drake Hotline Bling")
        assertEquals("1", copied.id)
        assertEquals("Drake Hotline Bling", copied.name)
        assertEquals("drake.jpg", copied.imageAssetName)
    }
}
