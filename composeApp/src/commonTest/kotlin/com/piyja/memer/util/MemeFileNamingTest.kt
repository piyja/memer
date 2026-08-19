package com.piyja.memer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemeFileNamingTest {

    @Test
    fun `generateFileName starts with meme prefix`() {
        val name = MemeFileNaming.generateFileName(System.currentTimeMillis())
        assertTrue("Filename should start with 'meme_', got: $name", name.startsWith("meme_"))
    }

    @Test
    fun `generateFileName ends with jpg extension`() {
        val name = MemeFileNaming.generateFileName(System.currentTimeMillis())
        assertTrue("Filename should end with '.jpg', got: $name", name.endsWith(".jpg"))
    }

    @Test
    fun `generateFileName contains the timestamp`() {
        val ts = 1700000000000L
        val name = MemeFileNaming.generateFileName(ts)
        assertEquals("meme_1700000000000.jpg", name)
    }

    @Test
    fun `generateFileName produces different names for different timestamps`() {
        val name1 = MemeFileNaming.generateFileName(1700000000000L)
        val name2 = MemeFileNaming.generateFileName(1700000001000L)
        assertNotEquals("Different timestamps should produce different filenames", name1, name2)
    }

    @Test
    fun `generateFileName produces same name for same timestamp`() {
        val ts = 1700000000000L
        val name1 = MemeFileNaming.generateFileName(ts)
        val name2 = MemeFileNaming.generateFileName(ts)
        assertEquals("Same timestamp should produce same filename", name1, name2)
    }
}
