package com.example.tsumobilkabeta.Clustering.metric

import com.example.tsumobilkabeta.Clustering.model.Point

interface DistMetric{
    fun dist(a: Point, b: Point): Double
}