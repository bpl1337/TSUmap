package com.example.tsumobilkabeta

import AStar.AStarPathfinder
import AStar.GridProjection
import AStar.WalkabilityCsvLoader
import AStar.WalkabilityGrid
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.yandex.mapkit.geometry.Point
import com.example.tsumobilkabeta.Ant.GridMap
import com.example.tsumobilkabeta.Ant.Places
import com.example.tsumobilkabeta.Ant.Reader
import com.example.tsumobilkabeta.Ant.RouteBuilder

class NavigationViewModel : ViewModel() {
    val startPoint: MutableState<Point?> = mutableStateOf(null)
    val endPoint: MutableState<Point?> = mutableStateOf(null)
    val selectionMode: MutableState<PointSelectionMode> = mutableStateOf(PointSelectionMode.START)
    val selectedAlgorithm: MutableState<RouteAlgorithm> = mutableStateOf(RouteAlgorithm.ASTAR)

    var gridLoaded by mutableStateOf(false)
        private set

    val routePoints: MutableState<List<Point>> = mutableStateOf(emptyList())

    private var grid: WalkabilityGrid? = null
    private var antGrid: GridMap? = null
    private var allPlaces: List<Places> = emptyList()

    fun loadGrid(context: Context) {
        if (gridLoaded) return
        grid = WalkabilityCsvLoader.load(context.applicationContext)
        antGrid = Reader.readGridMap(context.applicationContext,"ant.csv")
        allPlaces = Reader.readPlaces(context.applicationContext,"plasec.csv")
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
        routePoints.value=emptyList()
        if (algorithm== RouteAlgorithm.ANOTHER){
            endPoint.value=null
            selectionMode.value= PointSelectionMode.START
        }
    }

    fun resetPoints() {
        startPoint.value = null
        endPoint.value = null
        routePoints.value = emptyList()
    }
    fun buildRouteIfReady() {
        when (selectedAlgorithm.value) {
            RouteAlgorithm.ASTAR -> buildAstarRouteIfReady()
            RouteAlgorithm.ANOTHER -> buildRouteAntIfReady()
        }
    }

    fun buildAstarRouteIfReady() {
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

    fun buildRouteAntIfReady(){
        var gridMap = antGrid ?: return
        var userPoint = startPoint.value ?: return

        val routeBuilder = RouteBuilder(gridMap)

        val placesId = setOf(1,2,3,4,5,6)
        val selectedPlaces = allPlaces.filter { it.id in placesId }

        val mappedPlaces = routeBuilder.mapPlaces(selectedPlaces, maxRadius = 15)
        if (mappedPlaces.isEmpty()){
            routePoints.value=emptyList()
            return
        }
        val userGridRaw = gridMap.QGisToGrid(
            userPoint.longitude,
            userPoint.latitude
        )

        val userGrid = gridMap.snapToNear(userGridRaw, maxRadius = 20) ?: run{
            routePoints.value = emptyList()
            return
        }

        val result = routeBuilder.buildRoute(
            userPoint=userGrid,
            places=mappedPlaces,
            antCount = 20,
            iter=150,
            alpha = 1.0,
            beta=4.0,
            evaporation = 0.4,
            q=100.0,
            end=false
        )

        routePoints.value=result.test.map{(x,y)->
            Point(
                y,
                x
            )
        }

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

