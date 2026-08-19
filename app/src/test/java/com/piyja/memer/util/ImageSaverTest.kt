package com.piyja.memer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSaverTest {

    @Test
    fun `generateFileName starts with meme prefix`() {
        val name = ImageSaver.generateFileName(System.currentTimeMillis())
        assertTrue("Filename should start with 'meme_', got: $name", name.startsWith("meme_"))
    }

    @Test
    fun `generateFileName ends with jpg extension`() {
        val name = ImageSaver.generateFileName(System.currentTimeMillis())
        assertTrue("Filename should end with '.jpg', got: $name", name.endsWith(".jpg"))
    }

    @Test
    fun `generateFileName contains timestamp pattern`() {
        val name = ImageSaver.generateFileName(1700000000000L)
        assertTrue(
            "Filename should match pattern meme_YYYYMMDD_HHMMSS.jpg, got: $name",
            name.matches(Regex("meme_\\d{8}_\\d{6}\\.jpg"))
        )
    }

    @Test
    fun `generateFileName produces different names for different timestamps`() {
        val name1 = ImageSaver.generateFileName(1700000000000L)
        val name2 = ImageSaver.generateFileName(1700000001000L)
        assertNotEquals("Different timestamps should produce different filenames", name1, name2)
    }

    @Test
    fun `generateFileName produces same name for same timestamp`() {
        val ts = 1700000000000L
        val name1 = ImageSaver.generateFileName(ts)
        val name2 = ImageSaver.generateFileName(ts)
        assertEquals("Same timestamp should produce same filename", name1, name2)
    }

    @Test
    fun `generateFileName has correct total length`() {
        val name = ImageSaver.generateFileName(1700000000000L)
        val expected = "meme_".length + "yyyyMMdd_HHmmss".length + ".jpg".length
        assertEquals(
            "Filename should have expected length: prefix(5) + timestamp(15) + extension(4) = 24",
            expected,
            name.length
        )
    }
}