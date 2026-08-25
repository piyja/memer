package com.piyja.memer.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GifEncoderTest {

    private fun rgba(width: Int, height: Int, fill: (x: Int, y: Int) -> Int): RgbaImage {
        val px = ByteArray(width * height * 4)
        var p = 0
        for (y in 0 until height) for (x in 0 until width) {
            val c = fill(x, y)
            px[p++] = (c shr 16).toByte()
            px[p++] = (c shr 8).toByte()
            px[p++] = c.toByte()
            px[p++] = 0xFF.toByte()
        }
        return RgbaImage(width, height, px)
    }

    private fun expectedIndices(img: RgbaImage): IntArray {
        val idx = IntArray(img.width * img.height)
        var p = 0
        var i = 0
        while (p < img.pixels.size) {
            val r = img.pixels[p].toInt() and 0xFF
            val g = img.pixels[p + 1].toInt() and 0xFF
            val b = img.pixels[p + 2].toInt() and 0xFF
            p += 4
            val lr = (r * 5 + 127) / 255
            val lg = (g * 5 + 127) / 255
            val lb = (b * 5 + 127) / 255
            idx[i++] = lr * 36 + lg * 6 + lb
        }
        return idx
    }

    @Test
    fun encodesValidGifHeaderAndTrailer() {
        val f1 = rgba(4, 4) { _, _ -> 0xFF0000 }
        val f2 = rgba(4, 4) { _, _ -> 0x00FF00 }
        val bytes = GifEncoder.encodeAnimatedGif(listOf(f1, f2), listOf(400, 400))
        assertTrue(bytes.size > 20, "GIF should not be empty")
        assertEquals('G'.code, bytes[0].toInt() and 0xFF)
        assertEquals('I'.code, bytes[1].toInt() and 0xFF)
        assertEquals('F'.code, bytes[2].toInt() and 0xFF)
        assertEquals('8'.code, bytes[3].toInt() and 0xFF)
        assertEquals('9'.code, bytes[4].toInt() and 0xFF)
        assertEquals('a'.code, bytes[5].toInt() and 0xFF)
        assertEquals(0x3B, bytes.last().toInt() and 0xFF)
    }

    @Test
    fun encodesTwoImageFrames() {
        val f1 = rgba(4, 4) { _, _ -> 0xFF0000 }
        val f2 = rgba(4, 4) { _, _ -> 0x00FF00 }
        val bytes = GifEncoder.encodeAnimatedGif(listOf(f1, f2), listOf(400, 400))
        assertEquals(2, countImageFrames(bytes))
    }

    @Test
    fun roundTripsPixelsThroughLzw() {
        val f1 = rgba(8, 8) { x, y -> if ((x + y) % 2 == 0) 0xFF000000.toInt() else 0x00FFFFFF }
        val bytes = GifEncoder.encodeAnimatedGif(listOf(f1), listOf(400))
        val decoded = decodeFirstFrameIndices(bytes, 8, 8)
        val expected = expectedIndices(f1)
        assertEquals(expected.size, decoded.size, "decoded length")
        for (i in expected.indices) {
            assertEquals(expected[i], decoded[i], "pixel $i mismatch")
        }
    }

    // --- minimal GIF block walker + LZW decoder (test-only) -------------

    private fun countImageFrames(bytes: ByteArray): Int {
        var pos = 13
        val gctSize = (1 shl ((bytes[10].toInt() and 0x07) + 1))
        pos += gctSize * 3
        var frames = 0
        while (pos < bytes.size) {
            val b = bytes[pos].toInt() and 0xFF
            when {
                b == 0x3B -> break
                b == 0x21 -> {
                    pos++
                    pos++ // label
                    while (true) {
                        val size = bytes[pos].toInt() and 0xFF
                        pos++
                        if (size == 0) break
                        pos += size
                    }
                }
                b == 0x2C -> {
                    frames++
                    pos++
                    pos += 8
                    val packed = bytes[pos].toInt() and 0xFF
                    pos++
                    if (packed and 0x80 != 0) {
                        val lctSize = (1 shl ((packed and 0x07) + 1))
                        pos += lctSize * 3
                    }
                    pos++ // min code size
                    while (true) {
                        val size = bytes[pos].toInt() and 0xFF
                        pos++
                        if (size == 0) break
                        pos += size
                    }
                }
                else -> error("unexpected GIF block 0x${b.toString(16)} at $pos")
            }
        }
        return frames
    }

    private fun decodeFirstFrameIndices(bytes: ByteArray, width: Int, height: Int): IntArray {
        var pos = 13
        val gctSize = (1 shl ((bytes[10].toInt() and 0x07) + 1))
        pos += gctSize * 3
        var frameIndex = 0
        while (pos < bytes.size) {
            val b = bytes[pos].toInt() and 0xFF
            when {
                b == 0x3B -> break
                b == 0x21 -> {
                    pos++
                    pos++ // label
                    while (true) {
                        val size = bytes[pos].toInt() and 0xFF
                        pos++
                        if (size == 0) break
                        pos += size
                    }
                }
                b == 0x2C -> {
                    pos++
                    pos += 8
                    val packed = bytes[pos].toInt() and 0xFF
                    pos++
                    if (packed and 0x80 != 0) {
                        val lctSize = (1 shl ((packed and 0x07) + 1))
                        pos += lctSize * 3
                    }
                    val minCode = bytes[pos].toInt() and 0xFF
                    pos++
                    val lzw = mutableListOf<Byte>()
                    while (true) {
                        val size = bytes[pos].toInt() and 0xFF
                        pos++
                        if (size == 0) break
                        repeat(size) { lzw.add(bytes[pos++]) }
                    }
                    if (frameIndex == 0) return lzwDecode(lzw, minCode, width * height)
                    frameIndex++
                }
                else -> error("unexpected GIF block 0x${b.toString(16)} at $pos")
            }
        }
        error("no image frame found")
    }

    private fun lzwDecode(data: List<Byte>, minCodeSize: Int, expectedSize: Int): IntArray {
        val clear = 1 shl minCodeSize
        val eoi = clear + 1
        var codeSize = minCodeSize + 1
        val dict = mutableListOf<IntArray>()
        var nextCode = eoi + 1
        fun initDict() {
            dict.clear()
            for (i in 0 until clear) dict.add(intArrayOf(i))
            dict.add(intArrayOf()) // clear placeholder (index == clear)
            dict.add(intArrayOf()) // eoi placeholder (index == eoi)
            nextCode = eoi + 1
            codeSize = minCodeSize + 1
        }
        initDict()
        var bitPos = 0
        fun readCode(): Int {
            var code = 0
            for (i in 0 until codeSize) {
                val byteIndex = bitPos / 8
                val bitIndex = bitPos % 8
                val bit = (data[byteIndex].toInt() shr bitIndex) and 1
                code = code or (bit shl i)
                bitPos++
            }
            return code
        }
        val out = mutableListOf<Int>()
        var prev: IntArray? = null
        while (true) {
            val code = readCode()
            if (code == clear) {
                initDict()
                prev = null
                continue
            }
            if (code == eoi) break
            val entry = if (code < dict.size) dict[code] else (prev!! + prev[0])
            out.addAll(entry.toList())
            if (prev != null) {
                dict.add(prev + entry[0])
                nextCode++
                if (nextCode == (1 shl codeSize) && codeSize < 12) codeSize++
            }
            prev = entry
        }
        return out.take(expectedSize).toIntArray()
    }
}
