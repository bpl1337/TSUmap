package com.example.tsumobilkabeta.Genetic

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tsumobilkabeta.AStar.GridNode
import com.example.tsumobilkabeta.AStar.GridProjection
import com.example.tsumobilkabeta.AStar.WalkabilityCsvLoader
import com.example.tsumobilkabeta.AStar.WalkabilityGrid
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class DrawableRoute(
    val segments: List<List<Point>>,
    val isFinal: Boolean
)

enum class GeneticPhase { SELECT, PRECOMPUTING, RUNNING_GA, RESULT }

class GeneticFoodViewModel : ViewModel() {

    private var grid: WalkabilityGrid? = null
    var gridLoaded = false
        private set

    fun loadGrid(context: Context) {
        if (gridLoaded) return
        grid = WalkabilityCsvLoader.load(context.applicationContext)
        gridLoaded = true
    }

    private val _awaitingStart = mutableStateOf(false)

    private val _startNode = mutableStateOf<GridNode?>(null)
    val startNode: State<GridNode?> = _startNode

    private val _startPoint = mutableStateOf<Point?>(null)
    val startPoint: State<Point?> = _startPoint

    private val _phase = mutableStateOf(GeneticPhase.SELECT)
    val phase: State<GeneticPhase> = _phase

    private val _precompDone = mutableIntStateOf(0)
    val precompDone: State<Int> = _precompDone

    private val _precompTotal = mutableIntStateOf(1)
    val precompTotal: State<Int> = _precompTotal

    private val _gaGeneration = mutableIntStateOf(0)
    val gaGeneration: State<Int> = _gaGeneration

    val gaTotalGenerations = 250

    private val _bestRoute = mutableStateOf<Route?>(null)
    val bestRoute: State<Route?> = _bestRoute

    private val _drawableRoute = mutableStateOf<DrawableRoute?>(null)
    val drawableRoute: State<DrawableRoute?> = _drawableRoute

    private val _errorMsg = mutableStateOf<String?>(null)
    val errorMsg: State<String?> = _errorMsg

    private var gaJob: Job? = null

    fun requestStartPoint() {
        _awaitingStart.value = true
    }

    fun handleMapTap(point: Point) {
        val g = grid ?: return
        if (!_awaitingStart.value) return

        val raw = GridProjection.pointToNode(point, g)
        val walkable = g.nearestWalkable(raw) ?: return

        _startNode.value = walkable
        _startPoint.value = GridProjection.nodeToPoint(walkable, g)
        _awaitingStart.value = false
    }

    fun startGA(selectedItems: Set<String>) {
        val g = grid ?: return
        val start = _startNode.value ?: run {
            _errorMsg.value = "Поставьте начальную точку на карте"
            return
        }
        if (selectedItems.isEmpty()) {
            _errorMsg.value = "Выберите хотя бы одно блюдо"
            return
        }

        _errorMsg.value = null
        _phase.value = GeneticPhase.PRECOMPUTING
        _precompDone.intValue = 0
        _precompTotal.intValue = 1

        val candidates = FoodDatabase.allEstablishments.filter { est ->
            est.menu.any { it.name in selectedItems }
        }

        gaJob = viewModelScope.launch(Dispatchers.Default) {

            val precomputed = RoutePrecomputer.compute(
                grid = g,
                startNode = start,
                candidates = candidates,
                onProgress = { done, total ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _precompDone.intValue = done
                        _precompTotal.intValue = total
                    }
                }
            )

            if (precomputed == null || precomputed.establishments.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _errorMsg.value = "Не удалось вычислить маршрут"
                    _phase.value = GeneticPhase.SELECT
                }
                return@launch
            }

            withContext(Dispatchers.Main) { _phase.value = GeneticPhase.RUNNING_GA }

            val cal = Calendar.getInstance()
            val nowHour   = cal.get(Calendar.HOUR_OF_DAY)
            val nowMinute = cal.get(Calendar.MINUTE)

            val ga = GeneticAlgorithm(
                data = precomputed,
                requiredItems = selectedItems,
                totalGenerations = gaTotalGenerations,
                startHour = nowHour,
                startMinute = nowMinute
            )

            val updateEvery = 15
            ga.onGenerationUpdate = { route, gen ->
                viewModelScope.launch(Dispatchers.Main) {
                    _bestRoute.value = route
                    _gaGeneration.intValue = gen
                    if (gen % updateEvery == 0 || gen == 1) {
                        _drawableRoute.value = routeToDrawable(route, g, isFinal = false)
                    }
                }
            }

            val result = ga.run()

            withContext(Dispatchers.Main) {
                _bestRoute.value = result
                _drawableRoute.value = if (result != null) routeToDrawable(result, g, isFinal = true) else null
                _phase.value = GeneticPhase.RESULT
            }
        }
    }

    fun reset() {
        gaJob?.cancel()
        gaJob = null
        _bestRoute.value = null
        _drawableRoute.value = null
        _gaGeneration.intValue = 0
        _errorMsg.value = null
        _phase.value = GeneticPhase.SELECT
    }

    private fun routeToDrawable(route: Route, grid: WalkabilityGrid, isFinal: Boolean): DrawableRoute {
        val segments = route.pathSegments.map { seg ->
            seg.map { node -> GridProjection.nodeToPoint(node, grid) }
        }
        return DrawableRoute(segments, isFinal)
    }
}
