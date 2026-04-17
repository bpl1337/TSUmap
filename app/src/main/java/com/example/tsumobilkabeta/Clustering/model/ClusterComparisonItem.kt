package com.example.tsumobilkabeta.Clustering.model


data class ClusterComparisonItem(
    val establishment: Establishments,
    val euclideanCluster: Int,
    val walkingCluster: Int,
    val changed: Boolean
)