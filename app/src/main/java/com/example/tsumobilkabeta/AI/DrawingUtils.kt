package com.example.tsumobilkabeta

import android.graphics.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath

fun processDrawingImproved(composePath: Path, originalSize: Float): FloatArray {
    val androidPath = android.graphics.Path(composePath.asAndroidPath())
    val bounds = RectF()
    androidPath.computeBounds(bounds, true)

    val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val drawSize = maxOf(bounds.width(), bounds.height())
    if (drawSize <= 0f) return FloatArray(2500)

    val scale = 38f / drawSize
    val matrix = Matrix()

    matrix.postTranslate(-(bounds.left + bounds.width() / 2f), -(bounds.top + bounds.height() / 2f))
    matrix.postScale(scale, scale)
    matrix.postTranslate(25f, 25f)

    androidPath.transform(matrix)

    val paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    canvas.drawPath(androidPath, paint)

    val pixels = FloatArray(2500)
    for (y in 0 until 50) {
        for (x in 0 until 50) {
            pixels[y * 50 + x] = if (bitmap.getPixel(x, y) != Color.WHITE) 1.0f else 0.0f
        }
    }
    return pixels
}