package com.example.tsumobilkabeta.Clustering.metric

import android.content.Context
import com.example.tsumobilkabeta.AStar.AStarPathfinder
import com.example.tsumobilkabeta.AStar.GridProjection
import com.example.tsumobilkabeta.AStar.WalkabilityCsvLoader
import com.example.tsumobilkabeta.AStar.WalkabilityGrid
import com.example.tsumobilkabeta.Clustering.model.Point

class WalkingMetric(context: Context) : DistMetric {

    private val grid: WalkabilityGrid =
        WalkabilityCsvLoader.load(context.applicationContext)

    private val cache = mutableMapOf<Pair<Point, Point>, Double>()

    override fun dist(a: Point, b: Point): Double {
        val key = orderedKey(a, b)
        cache[key]?.let { return it }

        val start = com.yandex.mapkit.geometry.Point(a.y, a.x)
        val end = com.yandex.mapkit.geometry.Point(b.y, b.x)

        val startNode = grid.nearestWalkable(GridProjection.pointToNode(start, grid))
        val endNode = grid.nearestWalkable(GridProjection.pointToNode(end, grid))

        val result = if (startNode == null || endNode == null) {
            Double.MAX_VALUE
        } else {
            val path = AStarPathfinder.findPath(grid, startNode, endNode)
            if (path == null || path.isEmpty()) {
                Double.MAX_VALUE
            } else {
                AStarPathfinder.pathDistanceMeters(path)
            }
        }

        cache[key] = result
        return result
    }

    fun orderedKey(a: Point, b: Point): Pair<Point, Point> {
        return if (a.x < b.x || (a.x == b.x && a.y <= b.y)) {
            a to b
        } else {
            b to a
        }
    }
}
