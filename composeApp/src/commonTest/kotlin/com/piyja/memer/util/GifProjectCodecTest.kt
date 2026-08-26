package com.piyja.memer.util

import com.piyja.memer.data.GifProject
import com.piyja.memer.data.TextSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GifProjectCodecTest {

    @Test
    fun roundTripsProjectWithSections() {
        val sections = listOf(
            TextSection("a", 0, 2000, "hello", 0xFFFFFFFF, true, 0.5f, 0.3f, 1.2f),
            TextSection("b", 2000, 5000, "world\n!", 0xFF000000, false, 0.1f, 0.9f, 2.5f)
        )
        val project = GifProject(
            sourcePath = "/tmp/clip.mp4",
            durationMs = 10000,
            isGif = false,
            trimStartMs = 1500,
            trimEndMs = 6500,
            fps = 12,
            sections = sections
        )

        val decoded = decodeGifProject(encodeGifProject(project))
        assertNotNull(decoded)
        assertEquals(project.sourcePath, decoded.sourcePath)
        assertEquals(project.durationMs, decoded.durationMs)
        assertEquals(project.isGif, decoded.isGif)
        assertEquals(project.trimStartMs, decoded.trimStartMs)
        assertEquals(project.trimEndMs, decoded.trimEndMs)
        assertEquals(project.fps, decoded.fps)
        assertEquals(2, decoded.sections.size)
        assertEquals("hello", decoded.sections[0].text)
        assertEquals("world\n!", decoded.sections[1].text)
        assertEquals(0.5f, decoded.sections[0].xRatio)
        assertEquals(2.5f, decoded.sections[1].scale)
    }

    @Test
    fun rejectsUnknownMagic() {
        assertNull(decodeGifProject("NOTAPROJECT\nfoo"))
    }

    @Test
    fun selectsSectionCoveringRelativeTime() {
        val sections = listOf(
            TextSection("a", 0, 2000, "first", 0xFFFFFFFF, true, 0.5f, 0.5f, 1f),
            TextSection("b", 2000, 5000, "second", 0xFF000000, true, 0.5f, 0.5f, 1f)
        )
        fun sectionAt(rel: Long) = sections.firstOrNull { rel >= it.startMs && rel < it.endMs }

        assertEquals("first", sectionAt(0)?.text)
        assertEquals("first", sectionAt(1999)?.text)
        assertEquals("second", sectionAt(2000)?.text)
        assertEquals("second", sectionAt(4999)?.text)
        assertEquals(null, sectionAt(5000)?.text)
    }
}
