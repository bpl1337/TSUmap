package com.example.tsumobilkabeta

import android.graphics.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap

fun floatArrayToBitmap(data: FloatArray): ImageBitmap {
    val size = 50
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val color = if (data[y * size + x] == 1.0f) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            bitmap.setPixel(x, y, color)
        }
    }
    return bitmap.asImageBitmap()
}
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
fun processDrawing2px(composePath: Path, originalSize: Float): FloatArray {
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
        strokeWidth = 2f
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