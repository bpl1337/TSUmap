package com.example.tsumobilkabeta.Clustering.metric

import com.example.tsumobilkabeta.Clustering.model.Point
import kotlin.math.sqrt

object EuclideanMetric: DistMetric{
    override fun dist(a: Point, b: Point): Double {
        val dx = a.x-b.x
        val dy = a.y-b.y
        return sqrt(dx*dx+dy*dy)
    }
}