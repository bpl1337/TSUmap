package com.example.tsumobilkabeta.AStar

import com.yandex.mapkit.geometry.Point
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

object GridProjection {
    private const val EARTH_RADIUS = 6378137.0

    fun pointToMeters(point: Point): Pair<Double, Double> {
        val x = longitudeToMeters(point.longitude)
        val y = latitudeToMeters(point.latitude)
        return x to y
    }

    fun metersToPoint(x: Double, y: Double): Point {
        return Point(
            metersToLatitude(y),
            metersToLongitude(x)
        )
    }

    fun pointToNode(point: Point, grid: WalkabilityGrid): GridNode {
        val x = longitudeToMeters(point.longitude)
        val y = latitudeToMeters(point.latitude)
        val hasCentroids = grid.uniqueX.size == grid.width && grid.uniqueY.size == grid.height

        val gridX = if (hasCentroids) {
            findNearestIndex(x, grid.uniqueX)
        } else {
            ((x - grid.minX) / grid.step).roundToInt()
                .coerceIn(0, (grid.width - 1).coerceAtLeast(0))
        }

        val gridY = if (hasCentroids) {
            findNearestIndex(y, grid.uniqueY)
        } else {
            ((y - grid.minY) / grid.step).roundToInt()
                .coerceIn(0, (grid.height - 1).coerceAtLeast(0))
        }

        return GridNode(gridX, gridY)
    }

    fun nodeToPoint(node: GridNode, grid: WalkabilityGrid): Point {
        val hasCentroids = grid.uniqueX.size == grid.width && grid.uniqueY.size == grid.height

        val x = if (hasCentroids) {
            grid.uniqueX[node.x.coerceIn(0, grid.uniqueX.lastIndex)]
        } else {
            grid.minX + node.x * grid.step
        }

        val y = if (hasCentroids) {
            grid.uniqueY[node.y.coerceIn(0, grid.uniqueY.lastIndex)]
        } else {
            grid.minY + node.y * grid.step
        }

        val longitude = metersToLongitude(x)
        val latitude = metersToLatitude(y)

        return Point(latitude, longitude)
    }

    private fun findNearestIndex(value: Double, values: List<Double>): Int {
        if (values.isEmpty()) return 0
        if (value <= values.first()) return 0
        if (value >= values.last()) return values.lastIndex

        var left = 0
        var right = values.lastIndex

        while (left < right) {
            val mid = (left + right) / 2
            if (values[mid] < value) {
                left = mid + 1
            } else {
                right = mid
            }
        }

        val rightIndex = left
        val leftIndex = (rightIndex - 1).coerceAtLeast(0)

        return if (abs(values[leftIndex] - value) <= abs(values[rightIndex] - value)) {
            leftIndex
        } else {
            rightIndex
        }
    }

    private fun longitudeToMeters(longitude: Double): Double {
        return EARTH_RADIUS * Math.toRadians(longitude)
    }

    private fun latitudeToMeters(latitude: Double): Double {
        val latRad = Math.toRadians(latitude.coerceIn(-85.05112878, 85.05112878))
        return EARTH_RADIUS * ln(tan(PI / 4.0 + latRad / 2.0))
    }

    private fun metersToLongitude(x: Double): Double {
        return Math.toDegrees(x / EARTH_RADIUS)
    }

    private fun metersToLatitude(y: Double): Double {
        val latRad = 2.0 * atan(exp(y / EARTH_RADIUS)) - PI / 2.0
        return Math.toDegrees(latRad)
    }
}
