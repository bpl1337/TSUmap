package com.example.tsumobilkabeta.Clustering

import com.example.tsumobilkabeta.Clustering.metric.DistMetric
import com.example.tsumobilkabeta.Clustering.model.Establishments
import com.example.tsumobilkabeta.Clustering.model.Point
import kotlin.random.Random

data class Cluster(
    val center: Point,
    val items: List<Establishments>
)

object KMeans{
    fun clusterize(
        establishments: List<Establishments>,
        k: Int,
        metric: DistMetric
    ): List<Cluster> {

        var centers = establishments
            .shuffled(Random(System.currentTimeMillis())).take(k).map { it.coordinate }

        repeat(100) {
            val groupedItems = List(k) { mutableListOf<Establishments>() }

            establishments.forEach { establishment ->
                val nearestCenterIndex = centers.indices.minBy { index ->
                    metric.dist(establishment.coordinate, centers[index])
                }
                groupedItems[nearestCenterIndex].add(establishment)
            }

            val newCenters = centers.mapIndexed { index, oldCenter ->
                val items = groupedItems[index]
                if (items.isEmpty()) oldCenter else findCenter(items)
            }

            if (newCenters == centers) {
                return centers.mapIndexed { index, center ->
                    Cluster(center = center, items = groupedItems[index])
                }
            }

            centers = newCenters
        }

        val groupedItems = List(k) { mutableListOf<Establishments>() }

        establishments.forEach { establishment ->
            val nearestCenterIndex = centers.indices.minBy { index ->
                metric.dist(establishment.coordinate, centers[index])
            }
            groupedItems[nearestCenterIndex].add(establishment)
        }

        return centers.mapIndexed { index, center ->
            Cluster(center = center, items = groupedItems[index])
        }
    }

}

fun findCenter(items: List<Establishments>): Point {
    val x = items.map{it.coordinate.x}.average()
    val y = items.map{it.coordinate.y}.average()
    return Point(x, y)
}