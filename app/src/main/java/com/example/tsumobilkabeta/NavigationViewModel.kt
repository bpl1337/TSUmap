package com.example.tsumobilkabeta

import com.example.tsumobilkabeta.AStar.AStarPathfinder
import com.example.tsumobilkabeta.AStar.GridNode
import com.example.tsumobilkabeta.AStar.GridProjection
import com.example.tsumobilkabeta.AStar.WalkabilityCsvLoader
import com.example.tsumobilkabeta.AStar.WalkabilityGrid
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NavigationViewModel : ViewModel() {
    val startPoint: MutableState<Point?> = mutableStateOf(null)
    val endPoint: MutableState<Point?> = mutableStateOf(null)
    val selectionMode: MutableState<PointSelectionMode> = mutableStateOf(PointSelectionMode.START)
    val selectedAlgorithm: MutableState<RouteAlgorithm> = mutableStateOf(RouteAlgorithm.ASTAR)

    var gridLoaded by mutableStateOf(false)
        private set

    val routePoints: MutableState<List<Point>> = mutableStateOf(emptyList())

    val barrierNodes: MutableState<Set<GridNode>> = mutableStateOf(emptySet())
    val animClosed: MutableState<Set<GridNode>> = mutableStateOf(emptySet())
    val animOpen: MutableState<Set<GridNode>> = mutableStateOf(emptySet())
    val animCurrent: MutableState<GridNode?> = mutableStateOf(null)
    val animPath: MutableState<List<GridNode>> = mutableStateOf(emptyList())
    var isAnimating by mutableStateOf(false)
        private set
    var pathStatus by mutableStateOf(PathStatus.NONE)
        private set
    var showNoPathDialog by mutableStateOf(false)
        private set

    private var grid: WalkabilityGrid? = null
    private var animationJob: Job? = null
    private var computedPath: List<GridNode> = emptyList()

    val currentGrid: WalkabilityGrid? get() = grid

    fun loadGrid(context: Context) {
        if (gridLoaded) return
        grid = WalkabilityCsvLoader.load(context.applicationContext)
        gridLoaded = true
    }

    fun setStartPoint(point: Point) { startPoint.value = point }
    fun setEndPoint(point: Point) { endPoint.value = point }
    fun setSelectionMode(mode: PointSelectionMode) { selectionMode.value = mode }

    fun selectAlgorithm(algorithm: RouteAlgorithm) {
        selectedAlgorithm.value = algorithm
        routePoints.value = emptyList()
        if (algorithm == RouteAlgorithm.ANOTHER) {
            endPoint.value = null
            selectionMode.value = PointSelectionMode.START
        }
    }

    fun toggleBarrier(point: Point) {
        val g = grid ?: return
        val node = GridProjection.pointToNode(point, g)
        if (!g.inBounds(node)) return
        if (g.isBarrier(node)) g.removeBarrier(node) else g.addBarrier(node)
        barrierNodes.value = g.barrierSnapshot()
        routePoints.value = emptyList()
        clearAnimState()
        pathStatus = PathStatus.NONE
    }

    fun dismissNoPathDialog() { showNoPathDialog = false }

    fun resetPoints() {
        startPoint.value = null
        endPoint.value = null
        routePoints.value = emptyList()
        grid?.clearBarriers()
        barrierNodes.value = emptySet()
        clearAnimState()
        pathStatus = PathStatus.NONE
    }

    fun skipAnimation() {
        animationJob?.cancel()
        animationJob = null
        val g = grid
        isAnimating = false
        animClosed.value = emptySet()
        animOpen.value = emptySet()
        animCurrent.value = null
        animPath.value = emptyList()
        if (g != null) applyComputedPath(g)
    }

    private fun clearAnimState() {
        animationJob?.cancel()
        animationJob = null
        isAnimating = false
        computedPath = emptyList()
        showNoPathDialog = false
        animClosed.value = emptySet()
        animOpen.value = emptySet()
        animCurrent.value = null
        animPath.value = emptyList()
    }

    private fun applyComputedPath(g: WalkabilityGrid) {
        if (computedPath.isNotEmpty()) {
            routePoints.value = computedPath.map { GridProjection.nodeToPoint(it, g) }
        } else if (pathStatus == PathStatus.NOT_FOUND) {
            showNoPathDialog = true
        }
    }

    fun buildRouteIfReady() {
        when (selectedAlgorithm.value) {
            RouteAlgorithm.ASTAR -> buildAstarRouteIfReady()
            RouteAlgorithm.ANT -> Unit
            RouteAlgorithm.ANOTHER -> Unit
            RouteAlgorithm.DECISION_TREE -> Unit
            RouteAlgorithm.GENETIC -> Unit
            RouteAlgorithm.CLUSTERING-> Unit

        }
    }

    fun buildAstarRouteIfReady() {
        val g = grid ?: return
        val start = startPoint.value ?: return
        val end = endPoint.value ?: return

        val startNode = g.nearestWalkable(GridProjection.pointToNode(start, g)) ?: run {
            pathStatus = PathStatus.NOT_FOUND
            showNoPathDialog = true
            return
        }
        val endNode = g.nearestWalkable(GridProjection.pointToNode(end, g)) ?: run {
            pathStatus = PathStatus.NOT_FOUND
            showNoPathDialog = true
            return
        }

        clearAnimState()
        pathStatus = PathStatus.SEARCHING

        animationJob = viewModelScope.launch {
            val (path, steps) = withContext(Dispatchers.Default) {
                runCatching { AStarPathfinder.findPathWithSteps(g, startNode, endNode) }
                    .getOrDefault(Pair(emptyList(), emptyList()))
            }

            computedPath = path
            pathStatus = if (path.isEmpty()) PathStatus.NOT_FOUND else PathStatus.FOUND

            if (steps.isNotEmpty()) {
                isAnimating = true
                val delayMs = (5000L / steps.size).coerceIn(10L, 80L)
                for (step in steps) {
                    animClosed.value = step.closedSet
                    animOpen.value = step.openSet
                    animCurrent.value = step.current
                    animPath.value = step.path
                    delay(delayMs)
                }
            }

            animClosed.value = emptySet()
            animOpen.value = emptySet()
            animCurrent.value = null
            animPath.value = emptyList()
            isAnimating = false
            applyComputedPath(g)
        }
    }

}

enum class PointSelectionMode { START, END, BARRIER }

enum class RouteAlgorithm { ASTAR, ANT, ANOTHER, DECISION_TREE, GENETIC, CLUSTERING}

enum class PathStatus { NONE, SEARCHING, FOUND, NOT_FOUND }
