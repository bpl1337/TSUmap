package com.example.tsumobilkabeta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.annotation.DrawableRes
import kotlin.math.roundToInt
import kotlin.math.hypot
import kotlin.math.abs

/**
 * Преобразует координаты с пользовательской карты (usermap) на черно-белую карту (walkable)
 * и проверяет проходимость для алгоритма поиска пути.
 */
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

    /**
     * Инициализирует карты. Должен быть вызван один раз при создании.
     */
    fun initialize() {
        userMapBitmap = BitmapFactory.decodeResource(context.resources, userMapResId)
        walkableMapBitmap = BitmapFactory.decodeResource(context.resources, walkableMapResId)
        
        userMapWidth = userMapBitmap?.width ?: 1
        userMapHeight = userMapBitmap?.height ?: 1
        walkableMapWidth = walkableMapBitmap?.width ?: 1
        walkableMapHeight = walkableMapBitmap?.height ?: 1
    }

    /**
     * Преобразует координаты с пользовательской карты на черно-белую walkable карту.
     * 
     * @param userMapPoint точка на пользовательской карте
     * @return точка на walkable карте, или null если инициализация не выполнена
     */
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

    /**
     * Проверяет, является ли точка на walkable карте проходимой (белая).
     * Белый пиксель = проходимо, черный = непроходимо.
     * 
     * @param walkablePoint точка на walkable карте
     * @return true если точка проходима, false если нет или карта не инициализирована
     */
    fun isWalkableAtPoint(walkablePoint: PointF): Boolean {
        val bitmap = walkableMapBitmap ?: return false
        
        val x = walkablePoint.x.roundToInt().coerceIn(0, bitmap.width - 1)
        val y = walkablePoint.y.roundToInt().coerceIn(0, bitmap.height - 1)
        
        val pixel = bitmap.getPixel(x, y)
        
        // Проверяем если пиксель белый (RGB близко к белому)
        // В черно-белом изображении белый = 0xFFFFFFFF, черный = 0xFF000000
        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        
        // Если пиксель светлый (белый/серый), то это проходимая область
        val brightness = (red + green + blue) / 3
        return brightness > 128
    }

    /**
     * Находит ближайшую белую (проходимую) точку к заданной точке на walkable карте.
     * Ищет в радиусе до maxRadius пикселей.
     * 
     * @param walkablePoint точка на walkable карте
     * @param maxRadius максимальный радиус поиска (по умолчанию 50 пикселей)
     * @return ближайшая проходимая точка, или исходная точка если ничего не найдено
     */
    fun findNearestWalkablePoint(walkablePoint: PointF, maxRadius: Int = 50): PointF {
        val bitmap = walkableMapBitmap ?: return walkablePoint
        
        val startX = walkablePoint.x.roundToInt()
        val startY = walkablePoint.y.roundToInt()
        
        // Если текущая точка уже белая, возвращаем её
        if (isWalkableAtPoint(walkablePoint)) {
            return walkablePoint
        }
        
        var nearestPoint = walkablePoint
        var nearestDistance = Float.MAX_VALUE
        
        // Ищем в квадрате вокруг точки, начиная с малого радиуса
        for (radius in 1..maxRadius) {
            // Проходим по границе квадрата с текущим радиусом
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    // Проверяем только пиксели на границе радиуса для оптимизации
                    if (abs(dx) != radius && abs(dy) != radius) continue
                    
                    val x = startX + dx
                    val y = startY + dy
                    
                    // Проверяем границы
                    if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) continue
                    
                    val testPoint = PointF(x.toFloat(), y.toFloat())
                    
                    // Если точка белая, проверяем расстояние
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
            
            // Если нашли точку, можно закончить (это будет ближайшая в этом радиусе)
            if (nearestDistance != Float.MAX_VALUE) {
                break
            }
        }
        
        return nearestPoint
    }

    /**
     * Преобразует точку с usermap на walkable и снапит её к ближайшей белой точке.
     * 
     * @param userMapPoint точка на пользовательской карте
     * @return снапленная точка на walkable карте, или null если инициализация не выполнена
     */
    fun snapUserPointToWalkable(userMapPoint: PointF): PointF? {
        val walkablePoint = convertToWalkableCoordinates(userMapPoint) ?: return null
        return findNearestWalkablePoint(walkablePoint)
    }

    /**
     * Преобразует точку с usermap на walkable, снапит её к ближайшей белой точке,
     * и преобразует обратно на usermap.
     * 
     * @param userMapPoint точка на пользовательской карте
     * @return снапленная точка обратно на usermap
     */
    fun snapPointAndConvertBack(userMapPoint: PointF): PointF {
        val snappedWalkable = snapUserPointToWalkable(userMapPoint) ?: return userMapPoint
        
        // Преобразуем обратно на usermap
        val scaleX = userMapWidth.toFloat() / walkableMapWidth
        val scaleY = userMapHeight.toFloat() / walkableMapHeight
        
        return PointF(
            snappedWalkable.x * scaleX,
            snappedWalkable.y * scaleY
        )
    }

    /**
     * Обрабатывает две точки с пользовательской карты:
     * преобразует их на walkable и проверяет проходимость.
     * 
     * @param startPoint точка начала на пользовательской карте
     * @param endPoint точка конца на пользовательской карте
     * @return данные для алгоритма A*, или null если один из пунктов непроходим
     */
    fun processPathRequest(startPoint: PointF, endPoint: PointF): PathFindingData? {
        val walkableStart = convertToWalkableCoordinates(startPoint) ?: return null
        val walkableEnd = convertToWalkableCoordinates(endPoint) ?: return null

        // Логирование для отладки
        val startWalkable = isWalkableAtPoint(walkableStart)
        val endWalkable = isWalkableAtPoint(walkableEnd)
        android.util.Log.d("PathFinding", """
            |Start point walkable check: $startWalkable at (${walkableStart.x.toInt()}, ${walkableStart.y.toInt()})
            |End point walkable check: $endWalkable at (${walkableEnd.x.toInt()}, ${walkableEnd.y.toInt()})
        """.trimMargin())

        // Проверяем что обе точки находятся на проходимых местах
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

    /**
     * Обрабатывает точки без "магнита" в UI:
     * пользовательские точки остаются как выбраны, снап применяется только к walkable-координатам.
     */
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

    /**
     * Обрабатывает две уже снапленные точки и гарантирует их проходимость.
     * Используется когда точки уже приведены к ближайшей белой области.
     * 
     * @param startPoint снапленная точка начала на пользовательской карте
     * @param endPoint снапленная точка конца на пользовательской карте  
     * @return данные для алгоритма A*
     */
    fun processPreSnappedPathRequest(startPoint: PointF, endPoint: PointF): PathFindingData? {
        val walkableStart = convertToWalkableCoordinates(startPoint) ?: return null
        val walkableEnd = convertToWalkableCoordinates(endPoint) ?: return null

        // Если точки после преобразования не совсем белые (из-за округления), 
        // снапим их ещё раз для гарантии
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

    /**
     * Получает цвет пикселя на walkable карте (для отладки).
     */
    fun getWalkablePixelColor(walkablePoint: PointF): Int {
        val bitmap = walkableMapBitmap ?: return 0
        val x = walkablePoint.x.roundToInt().coerceIn(0, bitmap.width - 1)
        val y = walkablePoint.y.roundToInt().coerceIn(0, bitmap.height - 1)
        return bitmap.getPixel(x, y)
    }

    /**
     * Освобождает ресурсы.
     */
    fun release() {
        userMapBitmap?.recycle()
        walkableMapBitmap?.recycle()
        userMapBitmap = null
        walkableMapBitmap = null
    }
}

/**
 * Данные для передачи алгоритму A*.
 * Содержит исходные координаты с пользовательской карты 
 * и преобразованные координаты для walkable карты.
 */
data class PathFindingData(
    /** Точка начала на пользовательской карте */
    val startPoint: PointF,
    /** Точка конца на пользовательской карте */
    val endPoint: PointF,
    /** Точка начала на walkable карте (для алгоритма) */
    val walkableStart: PointF,
    /** Точка конца на walkable карте (для алгоритма) */
    val walkableEnd: PointF,
    /** Ширина walkable карты */
    val walkableMapWidth: Int,
    /** Высота walkable карты */
    val walkableMapHeight: Int,
)

