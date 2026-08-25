package com.piyja.memer.util

/**
 * Minimal animated-GIF encoder that runs entirely in Kotlin common code.
 *
 * Strategy: a fixed 6x6x6 (216 colour) palette is used for every frame, so the
 * whole animation shares one global colour table. Each frame's pixels are
 * quantised to that palette and run through standard GIF LZW compression.
 */
object GifEncoder {

    private const val MIN_CODE_SIZE = 8 // 256-entry global colour table

    fun encodeAnimatedGif(
        frames: List<RgbaImage>,
        delaysMs: List<Int>,
        loop: Int = 0
    ): ByteArray {
        require(frames.isNotEmpty()) { "At least one frame is required" }
        val width = frames.first().width
        val height = frames.first().height
        require(frames.all { it.width == width && it.height == height }) {
            "All frames must share the same dimensions"
        }

        val palette = buildPalette()
        val globalColorTable = buildGlobalColorTable(palette)
        val out = ByteWriter()

        out.writeAscii("GIF89a")
        out.writeShort(width)
        out.writeShort(height)
        out.writeByte(0xF7) // global colour table, 256 entries
        out.writeByte(0) // background colour index
        out.writeByte(0) // pixel aspect ratio
        out.writeBytes(globalColorTable)
        writeNetscapeLoop(out, loop)

        frames.forEachIndexed { index, frame ->
            val delayCs = (delaysMs.getOrElse(index) { delaysMs.lastOrNull() ?: 400 } / 10)
                .coerceAtLeast(2)
            writeFrame(out, frame, delayCs)
        }

        out.writeByte(0x3B) // trailer
        return out.toByteArray()
    }

    // --- palette ---------------------------------------------------------

    /** 216 colours: 6 levels per channel. Returns [r,g,b] triples (0..255). */
    private fun buildPalette(): Array<IntArray> {
        val palette = Array(216) { IntArray(3) }
        var i = 0
        for (lr in 0..5) for (lg in 0..5) for (lb in 0..5) {
            palette[i][0] = lr * 51
            palette[i][1] = lg * 51
            palette[i][2] = lb * 51
            i++
        }
        return palette
    }

    private fun buildGlobalColorTable(palette: Array<IntArray>): ByteArray {
        val table = ByteArray(256 * 3)
        for (i in palette.indices) {
            table[i * 3] = palette[i][0].toByte()
            table[i * 3 + 1] = palette[i][1].toByte()
            table[i * 3 + 2] = palette[i][2].toByte()
        }
        return table
    }

    /** Map an RGBA frame to palette indices using nearest 6-level quantisation. */
    private fun quantize(frame: RgbaImage): IntArray {
        val indices = IntArray(frame.width * frame.height)
        val px = frame.pixels
        var p = 0
        var i = 0
        while (p < px.size) {
            val r = px[p].toInt() and 0xFF
            val g = px[p + 1].toInt() and 0xFF
            val b = px[p + 2].toInt() and 0xFF
            p += 4
            val lr = (r * 5 + 127) / 255
            val lg = (g * 5 + 127) / 255
            val lb = (b * 5 + 127) / 255
            indices[i++] = lr * 36 + lg * 6 + lb
        }
        return indices
    }

    // --- structure writers ----------------------------------------------

    private fun writeNetscapeLoop(out: ByteWriter, loop: Int) {
        out.writeByte(0x21)
        out.writeByte(0xFF)
        out.writeByte(0x0B)
        out.writeAscii("NETSCAPE2.0")
        out.writeByte(0x03)
        out.writeByte(0x01)
        out.writeShort(loop)
        out.writeByte(0x00)
    }

    private fun writeFrame(out: ByteWriter, frame: RgbaImage, delayCs: Int) {
        // Graphic Control Extension
        out.writeByte(0x21)
        out.writeByte(0xF9)
        out.writeByte(0x04)
        out.writeByte(0x04) // disposal method 1 (do not dispose)
        out.writeShort(delayCs)
        out.writeByte(0x00) // transparent colour index
        out.writeByte(0x00)

        // Image Descriptor
        out.writeByte(0x2C)
        out.writeShort(0) // left
        out.writeShort(0) // top
        out.writeShort(frame.width)
        out.writeShort(frame.height)
        out.writeByte(0x00) // no local colour table

        // Image data
        out.writeByte(MIN_CODE_SIZE)
        val lzw = lzwEncode(quantize(frame), MIN_CODE_SIZE)
        var offset = 0
        while (offset < lzw.size) {
            val blockSize = minOf(255, lzw.size - offset)
            out.writeByte(blockSize)
            out.writeBytes(lzw.copyOfRange(offset, offset + blockSize))
            offset += blockSize
        }
        out.writeByte(0x00) // block terminator
    }

    // --- LZW compression (GIF variant) ----------------------------------

    private fun lzwEncode(indices: IntArray, minCodeSize: Int): ByteArray {
        val clearCode = 1 shl minCodeSize
        val eoiCode = clearCode + 1
        val bitWriter = BitWriter()

        var codeSize = minCodeSize + 1
        var maxCode = (1 shl codeSize) - 1
        var nextCode = eoiCode + 1
        val dict = HashMap<Int, Int>()

        fun resetDict() {
            dict.clear()
            nextCode = eoiCode + 1
            codeSize = minCodeSize + 1
            maxCode = (1 shl codeSize) - 1
        }

        if (indices.isEmpty()) {
            bitWriter.writeCode(clearCode, codeSize)
            bitWriter.writeCode(eoiCode, codeSize)
            bitWriter.flush()
            return bitWriter.toByteArray()
        }

        bitWriter.writeCode(clearCode, codeSize)
        var prefix = indices[0]
        for (i in 1 until indices.size) {
            val k = indices[i]
            val key = (prefix shl 8) or k
            val existing = dict[key]
            if (existing != null) {
                prefix = existing
            } else {
                bitWriter.writeCode(prefix, codeSize)
                if (nextCode < 4096) {
                    dict[key] = nextCode
                    nextCode++
                    if (nextCode > maxCode && codeSize < 12) {
                        codeSize++
                        maxCode = if (codeSize == 12) 4096 else (1 shl codeSize) - 1
                    }
                } else {
                    bitWriter.writeCode(clearCode, codeSize)
                    resetDict()
                }
                prefix = k
            }
        }
        bitWriter.writeCode(prefix, codeSize)
        bitWriter.writeCode(eoiCode, codeSize)
        bitWriter.flush()
        return bitWriter.toByteArray()
    }

    // --- low level byte/bit writers -------------------------------------

    private class ByteWriter {
        private val bytes = mutableListOf<Byte>()

        fun writeByte(value: Int) {
            bytes.add((value and 0xFF).toByte())
        }

        fun writeBytes(array: ByteArray) {
            for (b in array) bytes.add(b)
        }

        fun writeShort(value: Int) {
            writeByte(value and 0xFF)
            writeByte((value shr 8) and 0xFF)
        }

        fun writeAscii(text: String) {
            for (c in text) writeByte(c.code)
        }

        fun toByteArray(): ByteArray = bytes.toByteArray()
    }

    private class BitWriter {
        private val bytes = mutableListOf<Byte>()
        private var accumulator = 0
        private var bitCount = 0

        fun writeCode(code: Int, codeSize: Int) {
            accumulator = accumulator or (code shl bitCount)
            bitCount += codeSize
            while (bitCount >= 8) {
                bytes.add((accumulator and 0xFF).toByte())
                accumulator = accumulator shr 8
                bitCount -= 8
            }
        }

        fun flush() {
            if (bitCount > 0) {
                bytes.add((accumulator and 0xFF).toByte())
                accumulator = 0
                bitCount = 0
            }
        }

        fun toByteArray(): ByteArray = bytes.toByteArray()
    }
}
