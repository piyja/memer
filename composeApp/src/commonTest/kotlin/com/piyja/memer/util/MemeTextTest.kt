package com.piyja.memer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MemeTextTest {

    @Test
    fun `formatMemeText uppercases lowercase input`() {
        assertEquals("HELLO WORLD", MemeText.formatMemeText("hello world"))
    }

    @Test
    fun `formatMemeText uppercases mixed case input`() {
        assertEquals("HELLO WORLD", MemeText.formatMemeText("HeLLo WoRLd"))
    }

    @Test
    fun `formatMemeText trims leading whitespace`() {
        assertEquals("HELLO", MemeText.formatMemeText("   hello"))
    }

    @Test
    fun `formatMemeText trims trailing whitespace`() {
        assertEquals("HELLO", MemeText.formatMemeText("hello   "))
    }

    @Test
    fun `formatMemeText trims both leading and trailing whitespace`() {
        assertEquals("HELLO WORLD", MemeText.formatMemeText("  hello world  "))
    }

    @Test
    fun `formatMemeText returns empty string for blank input`() {
        assertEquals("", MemeText.formatMemeText(""))
    }

    @Test
    fun `formatMemeText returns empty string for whitespace-only input`() {
        assertEquals("", MemeText.formatMemeText("   "))
    }

    @Test
    fun `formatMemeText handles special characters`() {
        assertEquals("HELLO! 123 #@$", MemeText.formatMemeText("hello! 123 #@$"))
    }

    @Test
    fun `formatMemeText preserves internal spaces`() {
        assertEquals("TOP TEXT HERE", MemeText.formatMemeText("top text here"))
    }

    @Test
    fun `formatMemeText handles newlines by keeping them`() {
        val result = MemeText.formatMemeText("line1\nline2")
        assertEquals("LINE1\nLINE2", result)
    }
}
