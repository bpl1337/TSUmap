package com.example.tsumobilkabeta

import com.example.tsumobilkabeta.AStar.AStarPathfinder
import com.example.tsumobilkabeta.AStar.GridProjection
import com.example.tsumobilkabeta.AStar.WalkabilityCsvLoader
import com.example.tsumobilkabeta.AStar.WalkabilityGrid
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.yandex.mapkit.geometry.Point

class NavigationViewModel : ViewModel() {
    val startPoint: MutableState<Point?> = mutableStateOf(null)
    val endPoint: MutableState<Point?> = mutableStateOf(null)
    val selectionMode: MutableState<PointSelectionMode> = mutableStateOf(PointSelectionMode.START)
    val selectedAlgorithm: MutableState<RouteAlgorithm> = mutableStateOf(RouteAlgorithm.ASTAR)

    var gridLoaded by mutableStateOf(false)
        private set

    val routePoints: MutableState<List<Point>> = mutableStateOf(emptyList())

    private var grid: WalkabilityGrid? = null

    fun loadGrid(context: Context) {
        if (gridLoaded) return
        grid = WalkabilityCsvLoader.load(context.applicationContext)
        gridLoaded = true
        buildRouteIfReady()
    }

    fun setStartPoint(point: Point) {
        startPoint.value = point
    }

    fun setEndPoint(point: Point) {
        endPoint.value = point
    }

    fun setSelectionMode(mode: PointSelectionMode) {
        selectionMode.value = mode
    }

    fun selectAlgorithm(algorithm: RouteAlgorithm) {
        selectedAlgorithm.value = algorithm
    }

    fun resetPoints() {
        startPoint.value = null
        endPoint.value = null
        routePoints.value = emptyList()
    }

    fun buildRouteIfReady() {
        val currentGrid = grid ?: run {
            return
        }
        val start = startPoint.value ?: return
        val end = endPoint.value ?: return

        val startGridNode = GridProjection.pointToNode(start, currentGrid)
        val endGridNode = GridProjection.pointToNode(end, currentGrid)

        val startNode = currentGrid.nearestWalkable(startGridNode) ?: run {
            routePoints.value = emptyList()
            return
        }

        val endNode = currentGrid.nearestWalkable(endGridNode) ?: run {
            routePoints.value = emptyList()
            return
        }

        val nodes = AStarPathfinder.findPath(currentGrid, startNode, endNode)

        routePoints.value = nodes.map { GridProjection.nodeToPoint(it, currentGrid) }
    }
}

enum class PointSelectionMode {
    START,
    END
}

enum class RouteAlgorithm {
    ASTAR,
    ANOTHER
}

