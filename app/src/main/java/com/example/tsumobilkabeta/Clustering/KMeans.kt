package com.example.tsumobilkabeta.Clustering

import kotlin.math.sqrt
import kotlin.random.Random

data class Cluster(
    val center: Point,
    val items: List<Establishments>
)

object KMeans{
    fun clusterize(
        establishments: List<Establishments>,
        k: Int
    ): List<Cluster> {

        var centers = establishments
            .shuffled(Random(System.currentTimeMillis())).take(k).map { it.coordinate }

        repeat(100) {
            val groupedItems = List(k) { mutableListOf<Establishments>() }

            establishments.forEach { establishment ->
                val nearestCenterIndex = centers.indices.minBy { index ->
                    dist(establishment.coordinate, centers[index])
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
                dist(establishment.coordinate, centers[index])
            }
            groupedItems[nearestCenterIndex].add(establishment)
        }

        return centers.mapIndexed { index, center ->
            Cluster(center = center, items = groupedItems[index])
        }
    }

}


fun dist(a: Point,b:Point): Double{
    val dx =a.x-b.x
    val dy = a.y-b.y
    return sqrt(dx*dx+dy*dy)
}

fun findCenter(items: List<Establishments>): Point{
    val x = items.map{it.coordinate.x}.average()
    val y = items.map{it.coordinate.y}.average()
    return Point(x,y)
}