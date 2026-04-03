package com.example.tsumobilkabeta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.annotation.DrawableRes
import kotlin.math.roundToInt
import kotlin.math.hypot
import kotlin.math.abs

class PathFindingCoordinator(
    private val context: Context,
    @DrawableRes private val userMapResId: Int = R.drawable.usermap,
    @DrawableRes private val walkableMapResId: Int = R.drawable.walkablemap,
) {
    private var userMapBitmap: Bitmap? = null
    private var walkableMapBitmap: Bitmap? = null
    
    private var userMapWidth: Int = 0
    private var userMapHeight: Int = 0
    private var walkableMapWidth: Int = 0
    private var walkableMapHeight: Int = 0

    fun initialize() {
        userMapBitmap = BitmapFactory.decodeResource(context.resources, userMapResId)
        walkableMapBitmap = BitmapFactory.decodeResource(context.resources, walkableMapResId)
        
        userMapWidth = userMapBitmap?.width ?: 1
        userMapHeight = userMapBitmap?.height ?: 1
        walkableMapWidth = walkableMapBitmap?.width ?: 1
        walkableMapHeight = walkableMapBitmap?.height ?: 1
    }

    fun convertToWalkableCoordinates(userMapPoint: PointF): PointF? {
        if (userMapWidth <= 0 || userMapHeight <= 0 || walkableMapWidth <= 0 || walkableMapHeight <= 0) {
            return null
        }

        val scaleX = walkableMapWidth.toFloat() / userMapWidth
        val scaleY = walkableMapHeight.toFloat() / userMapHeight

        val walkableX = (userMapPoint.x * scaleX).coerceIn(0f, (walkableMapWidth - 1).toFloat())
        val walkableY = (userMapPoint.y * scaleY).coerceIn(0f, (walkableMapHeight - 1).toFloat())

        return PointF(walkableX, walkableY)
    }

    fun isWalkableAtPoint(walkablePoint: PointF): Boolean {
        val bitmap = walkableMapBitmap ?: return false
        
        val x = walkablePoint.x.roundToInt().coerceIn(0, bitmap.width - 1)
        val y = walkablePoint.y.roundToInt().coerceIn(0, bitmap.height - 1)
        
        val pixel = bitmap.getPixel(x, y)
        
        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        
        val brightness = (red + green + blue) / 3
        return brightness > 128
    }

    fun findNearestWalkablePoint(walkablePoint: PointF, maxRadius: Int = 50): PointF {
        val bitmap = walkableMapBitmap ?: return walkablePoint
        
        val startX = walkablePoint.x.roundToInt()
        val startY = walkablePoint.y.roundToInt()
        
        if (isWalkableAtPoint(walkablePoint)) {
            return walkablePoint
        }
        
        var nearestPoint = walkablePoint
        var nearestDistance = Float.MAX_VALUE
        
        for (radius in 1..maxRadius) {
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    if (abs(dx) != radius && abs(dy) != radius) continue
                    
                    val x = startX + dx
                    val y = startY + dy
                    
                    if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) continue
                    
                    val testPoint = PointF(x.toFloat(), y.toFloat())
                    
                    if (isWalkableAtPoint(testPoint)) {
                        val distance = hypot(
                            (x - startX).toFloat(),
                            (y - startY).toFloat()
                        )
                        
                        if (distance < nearestDistance) {
                            nearestDistance = distance
                            nearestPoint = testPoint
                        }
                    }
                }
            }
            
            if (nearestDistance != Float.MAX_VALUE) {
                break
            }
        }
        
        return nearestPoint
    }

    fun snapUserPointToWalkable(userMapPoint: PointF): PointF? {
        val walkablePoint = convertToWalkableCoordinates(userMapPoint) ?: return null
        return findNearestWalkablePoint(walkablePoint)
    }

    fun snapPointAndConvertBack(userMapPoint: PointF): PointF {
        val snappedWalkable = snapUserPointToWalkable(userMapPoint) ?: return userMapPoint
        
        val scaleX = userMapWidth.toFloat() / walkableMapWidth
        val scaleY = userMapHeight.toFloat() / walkableMapHeight
        
        return PointF(
            snappedWalkable.x * scaleX,
            snappedWalkable.y * scaleY
        )
    }

    fun processPathRequest(startPoint: PointF, endPoint: PointF): PathFindingData? {
        val walkableStart = convertToWalkableCoordinates(startPoint) ?: return null
        val walkableEnd = convertToWalkableCoordinates(endPoint) ?: return null

        val startWalkable = isWalkableAtPoint(walkableStart)
        val endWalkable = isWalkableAtPoint(walkableEnd)
        android.util.Log.d("PathFinding", """
            |Start point walkable check: $startWalkable at (${walkableStart.x.toInt()}, ${walkableStart.y.toInt()})
            |End point walkable check: $endWalkable at (${walkableEnd.x.toInt()}, ${walkableEnd.y.toInt()})
        """.trimMargin())

        if (!startWalkable || !endWalkable) {
            return null
        }

        return PathFindingData(
            startPoint = startPoint,
            endPoint = endPoint,
            walkableStart = walkableStart,
            walkableEnd = walkableEnd,
            walkableMapWidth = walkableMapWidth,
            walkableMapHeight = walkableMapHeight
        )
    }

    fun processPathRequestWithAutoSnap(
        startPoint: PointF,
        endPoint: PointF,
        maxSnapRadius: Int = 50,
    ): PathFindingData? {
        val rawStart = convertToWalkableCoordinates(startPoint) ?: return null
        val rawEnd = convertToWalkableCoordinates(endPoint) ?: return null

        val adaptiveRadius = maxOf(
            maxSnapRadius,
            minOf(walkableMapWidth, walkableMapHeight) / 12,
        )

        val snappedStart = if (isWalkableAtPoint(rawStart)) {
            rawStart
        } else {
            findNearestWalkablePoint(rawStart, adaptiveRadius)
        }

        val snappedEnd = if (isWalkableAtPoint(rawEnd)) {
            rawEnd
        } else {
            findNearestWalkablePoint(rawEnd, adaptiveRadius)
        }

        if (!isWalkableAtPoint(snappedStart) || !isWalkableAtPoint(snappedEnd)) {
            android.util.Log.d("PathFinding", """
                |AutoSnap failed
                |Raw start walkable=${isWalkableAtPoint(rawStart)} at (${rawStart.x.toInt()}, ${rawStart.y.toInt()})
                |Raw end walkable=${isWalkableAtPoint(rawEnd)} at (${rawEnd.x.toInt()}, ${rawEnd.y.toInt()})
                |Snapped start walkable=${isWalkableAtPoint(snappedStart)} at (${snappedStart.x.toInt()}, ${snappedStart.y.toInt()})
                |Snapped end walkable=${isWalkableAtPoint(snappedEnd)} at (${snappedEnd.x.toInt()}, ${snappedEnd.y.toInt()})
                |Adaptive radius=$adaptiveRadius
            """.trimMargin())
            return null
        }

        return PathFindingData(
            startPoint = startPoint,
            endPoint = endPoint,
            walkableStart = snappedStart,
            walkableEnd = snappedEnd,
            walkableMapWidth = walkableMapWidth,
            walkableMapHeight = walkableMapHeight,
        )
    }

    fun processPreSnappedPathRequest(startPoint: PointF, endPoint: PointF): PathFindingData? {
        val walkableStart = convertToWalkableCoordinates(startPoint) ?: return null
        val walkableEnd = convertToWalkableCoordinates(endPoint) ?: return null

        val finalWalkableStart = if (!isWalkableAtPoint(walkableStart)) {
            findNearestWalkablePoint(walkableStart, maxRadius = 3)
        } else {
            walkableStart
        }
        
        val finalWalkableEnd = if (!isWalkableAtPoint(walkableEnd)) {
            findNearestWalkablePoint(walkableEnd, maxRadius = 3)
        } else {
            walkableEnd
        }

        android.util.Log.d("PathFinding", """
            |Pre-snapped route processing
            |Start valid: ${isWalkableAtPoint(finalWalkableStart)} at (${finalWalkableStart.x.toInt()}, ${finalWalkableStart.y.toInt()})
            |End valid: ${isWalkableAtPoint(finalWalkableEnd)} at (${finalWalkableEnd.x.toInt()}, ${finalWalkableEnd.y.toInt()})
        """.trimMargin())

        return PathFindingData(
            startPoint = startPoint,
            endPoint = endPoint,
            walkableStart = finalWalkableStart,
            walkableEnd = finalWalkableEnd,
            walkableMapWidth = walkableMapWidth,
            walkableMapHeight = walkableMapHeight
        )
    }

    fun getWalkablePixelColor(walkablePoint: PointF): Int {
        val bitmap = walkableMapBitmap ?: return 0
        val x = walkablePoint.x.roundToInt().coerceIn(0, bitmap.width - 1)
        val y = walkablePoint.y.roundToInt().coerceIn(0, bitmap.height - 1)
        return bitmap.getPixel(x, y)
    }

    fun release() {
        userMapBitmap?.recycle()
        walkableMapBitmap?.recycle()
        userMapBitmap = null
        walkableMapBitmap = null
    }
}

data class PathFindingData(
    val startPoint: PointF,
    val endPoint: PointF,
    val walkableStart: PointF,
    val walkableEnd: PointF,
    val walkableMapWidth: Int,
    val walkableMapHeight: Int,
)

