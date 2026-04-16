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
    ): List<Cluster>{

        var center = establishments.shuffled(Random(System.currentTimeMillis())).take(k).map{it.coordinate}

        repeat(100){
            val groups = establishments.groupBy { establishments->center.minBy{center->dist(establishments.coordinate,center)} }

            val newCenters = groups.values.map{clusterItems -> findCenters(clusterItems)}

            if (newCenters==center){
                return groups.map{(center,items)->
                    Cluster(center=center,items=items)}
            }

            center=newCenters
        }

        val groups = establishments.groupBy { establishments -> center.minBy { center -> dist(establishments.coordinate,center) } }

        return groups.map{(center,items)->
            Cluster(center=center,items=items)
        }
    }
}


fun dist(a: Point,b:Point): Double{
    val dx =a.x-b.x
    val dy = a.y-b.y
    return sqrt(dx*dx+dy*dy)
}

fun findCenters(items: List<Establishments>): Point{
    val x = items.map{it.coordinate.x}.average()
    val y = items.map{it.coordinate.y}.average()
    return Point(x,y)
}