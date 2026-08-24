package com.piyja.memer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MemeTextBoxTest {

    @Test
    fun `toPositionedTexts maps text and ratios`() {
        val boxes = listOf(
            MemeTextBox(id = 1, text = "hello", xRatio = 0.5f, yRatio = 0.2f)
        )

        val positioned = boxes.toPositionedTexts()

        assertEquals(1, positioned.size)
        assertEquals("hello", positioned[0].text)
        assertEquals(0.5f, positioned[0].xRatio, 1e-6f)
        assertEquals(0.2f, positioned[0].yRatio, 1e-6f)
    }

    @Test
    fun `toPositionedTexts filters blank and whitespace-only boxes`() {
        val boxes = listOf(
            MemeTextBox(id = 1, text = "", xRatio = 0.1f, yRatio = 0.1f),
            MemeTextBox(id = 2, text = "   ", xRatio = 0.2f, yRatio = 0.2f),
            MemeTextBox(id = 3, text = "kept", xRatio = 0.3f, yRatio = 0.3f)
        )

        val positioned = boxes.toPositionedTexts()

        assertEquals(1, positioned.size)
        assertEquals("kept", positioned[0].text)
    }

    @Test
    fun `toPositionedTexts of empty list is empty`() {
        assertEquals(0, emptyList<MemeTextBox>().toPositionedTexts().size)
    }
}
