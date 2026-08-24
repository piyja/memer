package com.piyja.memer.util

import com.piyja.memer.data.MemeTextBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateStateCodecTest {

    @Test
    fun `round trip preserves texts and ratios`() {
        val boxes = listOf(
            MemeTextBox(id = 1, text = "TOP TEXT", xRatio = 0.5f, yRatio = 0.1f),
            MemeTextBox(id = 2, text = "bottom text", xRatio = 0.25f, yRatio = 0.9f)
        )

        val decoded = TemplateStateCodec.decodeBoxes(TemplateStateCodec.encodeBoxes(boxes))

        assertEquals(boxes.size, decoded.size)
        assertEquals("TOP TEXT", decoded[0].text)
        assertEquals(0.5f, decoded[0].xRatio, 1e-6f)
        assertEquals(0.1f, decoded[0].yRatio, 1e-6f)
        assertEquals("bottom text", decoded[1].text)
        assertEquals(0.25f, decoded[1].xRatio, 1e-6f)
        assertEquals(0.9f, decoded[1].yRatio, 1e-6f)
    }

    @Test
    fun `round trip preserves special characters`() {
        val boxes = listOf(
            MemeTextBox(id = 1, text = "when | breaks", xRatio = 0.4f, yRatio = 0.4f),
            MemeTextBox(id = 2, text = "line\nbreak", xRatio = 0.6f, yRatio = 0.6f),
            MemeTextBox(id = 3, text = "emoji 😂 ok", xRatio = 0.7f, yRatio = 0.7f)
        )

        val decoded = TemplateStateCodec.decodeBoxes(TemplateStateCodec.encodeBoxes(boxes))

        assertEquals(listOf("when | breaks", "line\nbreak", "emoji 😂 ok"), decoded.map { it.text })
    }

    @Test
    fun `blank text box survives round trip`() {
        val boxes = listOf(MemeTextBox(id = 1, text = "", xRatio = 0.1f, yRatio = 0.2f))

        val decoded = TemplateStateCodec.decodeBoxes(TemplateStateCodec.encodeBoxes(boxes))

        assertEquals(1, decoded.size)
        assertEquals("", decoded[0].text)
    }

    @Test
    fun `decode of blank input returns empty list`() {
        assertTrue(TemplateStateCodec.decodeBoxes(null).isEmpty())
        assertTrue(TemplateStateCodec.decodeBoxes("").isEmpty())
        assertTrue(TemplateStateCodec.decodeBoxes("   \n  ").isEmpty())
    }

    @Test
    fun `malformed lines are skipped without failing`() {
        val raw = """
            not-valid-base64!!|0.5|0.5
            a|b|c|d
            ${TemplateStateCodec.encodeBoxes(listOf(MemeTextBox(id = 1, text = "kept", xRatio = 0.3f, yRatio = 0.8f)))}
            junk
        """.trimIndent()

        val decoded = TemplateStateCodec.decodeBoxes(raw)

        assertEquals(1, decoded.size)
        assertEquals("kept", decoded[0].text)
        assertEquals(0.3f, decoded[0].xRatio, 1e-6f)
        assertEquals(0.8f, decoded[0].yRatio, 1e-6f)
    }
}
