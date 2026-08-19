package com.piyja.memer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.piyja.memer.data.MemeTemplate

object MemeRenderer {

    fun formatMemeText(text: String): String {
        return text.trim().uppercase()
    }

    fun calculateTextSize(imageWidth: Int, text: String): Float {
        if (text.isEmpty()) return 0f
        val targetWidth = imageWidth * 0.8f
        val paint = Paint().apply {
            typeface = Typeface.DEFAULT_BOLD
        }
        var size = imageWidth * 0.12f
        paint.textSize = size
        while (paint.measureText(text) > targetWidth && size > 12f) {
            size -= 2f
            paint.textSize = size
        }
        return size
    }

    fun loadTemplateBitmap(context: Context, assetName: String): Bitmap {
        val input = context.assets.open(assetName)
        return BitmapFactory.decodeStream(input)
    }

    fun render(
        context: Context,
        template: MemeTemplate,
        topText: String,
        bottomText: String
    ): Bitmap {
        val bitmap = loadTemplateBitmap(context, template.imageAssetName)
        return renderOnBitmap(bitmap, topText, bottomText)
    }

    fun renderOnBitmap(bitmap: Bitmap, topText: String, bottomText: String): Bitmap {
        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)

        val fillPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val strokePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val formattedTop = formatMemeText(topText)
        val formattedBottom = formatMemeText(bottomText)

        if (formattedTop.isNotEmpty()) {
            val topSize = calculateTextSize(mutable.width, formattedTop)
            fillPaint.textSize = topSize
            strokePaint.textSize = topSize
            val topY = topSize + 10f
            canvas.drawText(formattedTop, mutable.width / 2f, topY, strokePaint)
            canvas.drawText(formattedTop, mutable.width / 2f, topY, fillPaint)
        }

        if (formattedBottom.isNotEmpty()) {
            val bottomSize = calculateTextSize(mutable.width, formattedBottom)
            fillPaint.textSize = bottomSize
            strokePaint.textSize = bottomSize
            val bottomY = mutable.height - 15f
            canvas.drawText(formattedBottom, mutable.width / 2f, bottomY, strokePaint)
            canvas.drawText(formattedBottom, mutable.width / 2f, bottomY, fillPaint)
        }

        return mutable
    }
}