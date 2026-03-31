package com.example.tsumobilkabeta

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.nativeCanvas


class DigitDrawingManager(
    drawingSize: Int = 256,
    private val modelSize: Int = 50,
) {
    val canvasSize = drawingSize
    val bitmap = Bitmap.createBitmap(drawingSize, drawingSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    init {
        clear()
    }
    
    fun clear() {
        canvas.drawColor(Color.WHITE)
    }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK

            strokeWidth = 12f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawLine(x1, y1, x2, y2, paint)
    }
    

    fun getPixelData(): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, modelSize, modelSize, true)
        val pixelData = ByteArray(modelSize * modelSize)
        val pixels = IntArray(modelSize * modelSize)
        scaled.getPixels(pixels, 0, modelSize, 0, 0, modelSize, modelSize)
        
        for (i in pixels.indices) {

            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            pixelData[i] = (255 - gray).toByte() // Инвертируем
        }
        
        scaled.recycle()
        return pixelData
    }
    

    fun getNormalizedPixelData(): FloatArray {
        val pixelData = getPixelData()
        return FloatArray(pixelData.size) { i ->
            (pixelData[i].toInt() and 0xFF) / 255.0f
        }
    }
}


@Composable
fun DigitDrawingCanvas(
    modifier: Modifier = Modifier,
    canvasSize: Int = 50,
    drawingCanvasSize: Int = 256,
    onDrawComplete: (FloatArray) -> Unit = {}
) {
    val manager = remember(drawingCanvasSize, canvasSize) {
        DigitDrawingManager(
            drawingSize = drawingCanvasSize,
            modelSize = canvasSize,
        )
    }
    var drawVersion by remember { mutableStateOf(0) }
    
    val displaySize = 300.dp
    val density = LocalDensity.current
    val displaySizePx = with(density) { displaySize.toPx() }
    val pxToBitmap = drawingCanvasSize / displaySizePx
    
    Column(modifier = modifier) {
        

        Box(
            modifier = Modifier
                .size(displaySize)
                .background(androidx.compose.ui.graphics.Color.White)
                .border(
                    width = 2.dp,
                    color = androidx.compose.ui.graphics.Color.Black
                )
                .pointerInput(drawingCanvasSize, displaySizePx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var x = (down.position.x * pxToBitmap).coerceIn(0f, (drawingCanvasSize - 1).toFloat())
                        var y = (down.position.y * pxToBitmap).coerceIn(0f, (drawingCanvasSize - 1).toFloat())

                        manager.drawLine(x, y, x, y)
                        drawVersion++
                        
                        var drag = down
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            drag = event.changes.first()
                            
                            val newX = (drag.position.x * pxToBitmap).coerceIn(0f, (drawingCanvasSize - 1).toFloat())
                            val newY = (drag.position.y * pxToBitmap).coerceIn(0f, (drawingCanvasSize - 1).toFloat())
                            
                            manager.drawLine(x, y, newX, newY)
                            drawVersion++
                            x = newX
                            y = newY
                        } while (drag.pressed)
                    }
                }
        ) {
            Canvas(
                modifier = Modifier.size(displaySize)
            ) {

                val version = drawVersion
                if (version >= 0) {
                    drawContext.canvas.nativeCanvas.drawBitmap(
                        manager.bitmap,
                        null,
                        android.graphics.RectF(0f, 0f, size.width, size.height),
                        null,
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    manager.clear()
                    drawVersion++
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Очистить")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    val pixelData = manager.getNormalizedPixelData()
                    onDrawComplete(pixelData)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Определить")
            }
        }
    }
}
