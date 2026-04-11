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
import com.example.tsumobilkabeta.Ant.GridMap
import com.example.tsumobilkabeta.Ant.Places
import com.example.tsumobilkabeta.Ant.Reader
import com.example.tsumobilkabeta.Ant.RouteBuilder
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
    private var antGrid: GridMap? = null
    private var allPlaces: List<Places> = emptyList()
    private var animationJob: Job? = null
    private var computedPath: List<GridNode> = emptyList()

    val currentGrid: WalkabilityGrid? get() = grid

    fun loadGrid(context: Context) {
        if (gridLoaded) return
        grid = WalkabilityCsvLoader.load(context.applicationContext)
        antGrid = Reader.readGridMap(context.applicationContext, "ant.csv")
        allPlaces = Reader.readPlaces(context.applicationContext, "plasec.csv")
        gridLoaded = true
        buildRouteIfReady()
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
            RouteAlgorithm.ANT -> buildRouteAntIfReady()
            RouteAlgorithm.ANOTHER -> Unit
            RouteAlgorithm.DECISION_TREE -> Unit
            RouteAlgorithm.GENETIC -> Unit
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

    fun buildRouteAntIfReady() {
        val gridMap = antGrid ?: return
        val userPoint = startPoint.value ?: return

        val routeBuilder = RouteBuilder(gridMap)
        val placesId = setOf(1, 2, 3, 4, 5, 6)
        val selectedPlaces = allPlaces.filter { it.id in placesId }

        val mappedPlaces = routeBuilder.mapPlaces(selectedPlaces, maxRadius = 15)
        if (mappedPlaces.isEmpty()) { routePoints.value = emptyList(); return }

        val (userX, userY) = GridProjection.pointToMeters(userPoint)
        val userGridRaw = gridMap.QGisToGrid(userX, userY)
        val userGrid = gridMap.snapToNear(userGridRaw, maxRadius = 20) ?: run {
            routePoints.value = emptyList(); return
        }

        val result = routeBuilder.buildRoute(
            userPoint = userGrid,
            places = mappedPlaces,
            antCount = 20,
            iter = 150,
            alpha = 1.0,
            beta = 4.0,
            evaporation = 0.4,
            q = 100.0,
            end = false
        )
        routePoints.value = result.test.map { (x, y) -> GridProjection.metersToPoint(x, y) }
    }
}

enum class PointSelectionMode { START, END, BARRIER }

enum class RouteAlgorithm { ASTAR, ANT, ANOTHER, DECISION_TREE, GENETIC }

enum class PathStatus { NONE, SEARCHING, FOUND, NOT_FOUND }
