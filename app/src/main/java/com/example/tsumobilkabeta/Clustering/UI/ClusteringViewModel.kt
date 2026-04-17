package com.example.tsumobilkabeta.Clustering.UI

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.tsumobilkabeta.Clustering.Cluster
import com.example.tsumobilkabeta.Clustering.KMeans
import com.example.tsumobilkabeta.Clustering.metric.EuclideanMetric
import com.example.tsumobilkabeta.Clustering.metric.WalkingMetric
import com.example.tsumobilkabeta.Clustering.model.ClusterComparisonItem
import com.example.tsumobilkabeta.Clustering.model.ClusteringMode
import com.example.tsumobilkabeta.Clustering.model.DataProvider
import com.example.tsumobilkabeta.Clustering.model.Establishments

class ClusteringViewModel(application: Application) : AndroidViewModel(application) {

    val establishments: List<Establishments> = DataProvider.getEstablishments()

    var clusterCount by mutableIntStateOf(3)
        private set

    var mode by mutableStateOf(ClusteringMode.EUCLIDEAN)
        private set

    var euclideanClusters by mutableStateOf<List<Cluster>>(emptyList())
        private set

    var walkingClusters by mutableStateOf<List<Cluster>>(emptyList())
        private set

    var comparisonItems by mutableStateOf<List<ClusterComparisonItem>>(emptyList())
        private set

    val walkingMetric = WalkingMetric(application.applicationContext)

    val displayedClusters: List<Cluster>
        get() = when (mode) {
            ClusteringMode.EUCLIDEAN -> euclideanClusters
            ClusteringMode.WALKING -> walkingClusters
            ClusteringMode.COMPARISON -> euclideanClusters
        }

    fun increaseClusterCount() {
        if (clusterCount < establishments.size) clusterCount++
    }

    fun decreaseClusterCount() {
        if (clusterCount > 1) clusterCount--
    }

    fun changeMode(newMode: ClusteringMode) {
        mode = newMode
    }

    fun runClustering() {
        when (mode) {
            ClusteringMode.EUCLIDEAN -> {
                euclideanClusters = KMeans.clusterize(
                    establishments = establishments,
                    k = clusterCount,
                    metric = EuclideanMetric
                )
                walkingClusters = emptyList()
                comparisonItems = emptyList()
            }

            ClusteringMode.WALKING -> {
                walkingClusters = KMeans.clusterize(
                    establishments = establishments,
                    k = clusterCount,
                    metric = walkingMetric
                )
                euclideanClusters = emptyList()
                comparisonItems = emptyList()
            }

            ClusteringMode.COMPARISON -> {
                euclideanClusters = KMeans.clusterize(
                    establishments = establishments,
                    k = clusterCount,
                    metric = EuclideanMetric
                )

                walkingClusters = KMeans.clusterize(
                    establishments = establishments,
                    k = clusterCount,
                    metric = walkingMetric
                )

                comparisonItems = buildComparison(
                    establishments = establishments,
                    euclideanClusters = euclideanClusters,
                    walkingClusters = walkingClusters
                )
            }
        }
    }

    fun resetClustering() {
        clusterCount = 3
        mode = ClusteringMode.EUCLIDEAN
        euclideanClusters = emptyList()
        walkingClusters = emptyList()
        comparisonItems = emptyList()
    }

    fun buildComparison(
        establishments: List<Establishments>,
        euclideanClusters: List<Cluster>,
        walkingClusters: List<Cluster>
    ): List<ClusterComparisonItem> {
        return establishments.map { establishment ->
            val euclideanIndex = euclideanClusters.indexOfFirst { cluster ->
                cluster.items.any { it.id == establishment.id }
            }

            val walkingIndex = walkingClusters.indexOfFirst { cluster ->
                cluster.items.any { it.id == establishment.id }
            }

            ClusterComparisonItem(
                establishment = establishment,
                euclideanCluster = euclideanIndex,
                walkingCluster = walkingIndex,
                changed = euclideanIndex != walkingIndex
            )
        }
    }
}
