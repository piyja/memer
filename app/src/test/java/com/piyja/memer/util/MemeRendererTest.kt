package com.piyja.memer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MemeRendererTest {

    @Test
    fun `formatMemeText uppercases lowercase input`() {
        assertEquals("HELLO WORLD", MemeRenderer.formatMemeText("hello world"))
    }

    @Test
    fun `formatMemeText uppercases mixed case input`() {
        assertEquals("HELLO WORLD", MemeRenderer.formatMemeText("HeLLo WoRLd"))
    }

    @Test
    fun `formatMemeText trims leading whitespace`() {
        assertEquals("HELLO", MemeRenderer.formatMemeText("   hello"))
    }

    @Test
    fun `formatMemeText trims trailing whitespace`() {
        assertEquals("HELLO", MemeRenderer.formatMemeText("hello   "))
    }

    @Test
    fun `formatMemeText trims both leading and trailing whitespace`() {
        assertEquals("HELLO WORLD", MemeRenderer.formatMemeText("  hello world  "))
    }

    @Test
    fun `formatMemeText returns empty string for blank input`() {
        assertEquals("", MemeRenderer.formatMemeText(""))
    }

    @Test
    fun `formatMemeText returns empty string for whitespace-only input`() {
        assertEquals("", MemeRenderer.formatMemeText("   "))
    }

    @Test
    fun `formatMemeText handles special characters`() {
        assertEquals("HELLO! 123 #@$", MemeRenderer.formatMemeText("hello! 123 #@$"))
    }

    @Test
    fun `formatMemeText preserves internal spaces`() {
        assertEquals("TOP TEXT HERE", MemeRenderer.formatMemeText("top text here"))
    }

    @Test
    fun `formatMemeText handles newlines by keeping them`() {
        val result = MemeRenderer.formatMemeText("line1\nline2")
        assertEquals("LINE1\nLINE2", result)
    }
}