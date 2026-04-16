package com.example.tsumobilkabeta.Clustering

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ClusteringViewModel : ViewModel() {

    val establishments: List<Establishments> = DataProvider.getEstablishments()

    var clusterCount by mutableIntStateOf(3)
        private set

    var clusters by mutableStateOf<List<Cluster>>(emptyList())
        private set

    fun increaseClusterCount() {
        if (clusterCount < establishments.size) {
            clusterCount++
        }
    }

    fun decreaseClusterCount() {
        if (clusterCount > 1) {
            clusterCount--
        }
    }

    fun runClustering() {
        clusters = KMeans.clusterize(establishments, clusterCount)
    }

    fun resetClustering() {
        clusterCount = 3
        clusters = emptyList()
    }
}
