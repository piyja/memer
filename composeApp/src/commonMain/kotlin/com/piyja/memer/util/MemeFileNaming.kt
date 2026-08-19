package com.piyja.memer.util

object MemeFileNaming {

    private const val PREFIX = "meme_"
    private const val EXTENSION = ".jpg"

    fun generateFileName(timestamp: Long): String {
        return PREFIX + timestamp + EXTENSION
    }
}
