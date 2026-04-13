package com.example.tsumobilkabeta.Ant

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tsumobilkabeta.AStar.GridProjection
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AntMode { TSP, COWORKING }
enum class AntPhase { SELECT, COMPUTING, RESULT }

data class AntRouteSegment(
    val points: List<Point>,
    val color: Int
)

data class AntRouteMarker(
    val point: Point,
    val label: String,
    val color: Int,
    val detail: String? = null
)

data class AntDrawableRoute(
    val segments: List<AntRouteSegment>,
    val markers: List<AntRouteMarker>,
    val isFinal: Boolean
)

class AntViewModel : ViewModel() {

    private var antGrid: GridMap? = null
    var gridLoaded = false
        private set

    fun loadGrid(context: Context) {
        if (gridLoaded) return
        antGrid = Reader.readGridMap(context.applicationContext, "walkability.csv")
        gridLoaded = true
    }


    private val _mode = mutableStateOf(AntMode.TSP)
    val mode: State<AntMode> = _mode

    private val _phase = mutableStateOf(AntPhase.SELECT)
    val phase: State<AntPhase> = _phase


    private val _awaitingStart = mutableStateOf(false)
    val awaitingStart: State<Boolean> = _awaitingStart

    private val _startPoint = mutableStateOf<Point?>(null)
    val startPoint: State<Point?> = _startPoint


    private val _selectedPlaceIds = mutableStateOf(setOf<Int>())
    val selectedPlaceIds: State<Set<Int>> = _selectedPlaceIds

    private val _returnToStart = mutableStateOf(false)
    val returnToStart: State<Boolean> = _returnToStart


    private val _selectedCoworkingIds = mutableStateOf(setOf<Int>())
    val selectedCoworkingIds: State<Set<Int>> = _selectedCoworkingIds

    private val _studentCount = mutableIntStateOf(10)
    val studentCount: State<Int> = _studentCount


    private val _drawableRoute = mutableStateOf<AntDrawableRoute?>(null)
    val drawableRoute: State<AntDrawableRoute?> = _drawableRoute

    private val _resultDistance = mutableDoubleStateOf(0.0)
    val resultDistance: State<Double> = _resultDistance

    private val _resultPlaces = mutableStateOf<List<Places>>(emptyList())
    val resultPlaces: State<List<Places>> = _resultPlaces

    private val _coworkingResult = mutableStateOf<CoworkingResult?>(null)
    val coworkingResult: State<CoworkingResult?> = _coworkingResult

    private val _errorMsg = mutableStateOf<String?>(null)
    val errorMsg: State<String?> = _errorMsg

    private var computeJob: Job? = null


    fun getAllPlaces(): List<Places> = LandmarksDatabase.locations

    fun setMode(newMode: AntMode) {
        if (_mode.value == newMode) return
        _mode.value = newMode
        resetResult()
    }

    fun requestStartPoint() {
        _awaitingStart.value = true
    }

    fun handleMapTap(point: Point) {
        if (!_awaitingStart.value) return
        val gridMap = antGrid ?: return

        val (x, y) = GridProjection.pointToMeters(point)
        val raw = gridMap.QGisToGrid(x, y)
        val snap = gridMap.snapToNear(raw, 20) ?: return

        _startPoint.value = point
        _awaitingStart.value = false
    }

    fun togglePlace(id: Int) {
        val current = _selectedPlaceIds.value
        _selectedPlaceIds.value = if (id in current) current - id else current + id
    }

    fun selectAllPlaces() {
        _selectedPlaceIds.value = LandmarksDatabase.locations.map { it.id }.toSet()
    }

    fun deselectAllPlaces() {
        _selectedPlaceIds.value = emptySet()
    }

    fun setReturnToStart(value: Boolean) {
        _returnToStart.value = value
    }

    fun setStudentCount(count: Int) {
        _studentCount.intValue = count.coerceIn(1, 300)
    }

    fun toggleCoworkingLocation(id: Int) {
        val current = _selectedCoworkingIds.value
        _selectedCoworkingIds.value = if (id in current) current - id else current + id
    }

    fun selectAllCoworkingLocations() {
        _selectedCoworkingIds.value = CoworkingDatabase.locations.map { it.id }.toSet()
    }

    fun deselectAllCoworkingLocations() {
        _selectedCoworkingIds.value = emptySet()
    }


    fun buildTspRoute() {
        val gridMap = antGrid ?: return
        val userPoint = _startPoint.value ?: run {
            _errorMsg.value = "Поставьте начальную точку на карте"
            return
        }
        val selected = LandmarksDatabase.locations.filter { it.id in _selectedPlaceIds.value }
        if (selected.isEmpty()) {
            _errorMsg.value = "Выберите хотя бы одну достопримечательность"
            return
        }

        _errorMsg.value = null
        _phase.value = AntPhase.COMPUTING

        computeJob = viewModelScope.launch(Dispatchers.Default) {
            val routeBuilder = RouteBuilder(gridMap)
            val mapped = routeBuilder.mapPlaces(selected, 15)
            if (mapped.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _errorMsg.value = "Не удалось привязать точки к карте"
                    _phase.value = AntPhase.SELECT
                }
                return@launch
            }

            val (userX, userY) = GridProjection.pointToMeters(userPoint)
            val userGridRaw = gridMap.QGisToGrid(userX, userY)
            val userGrid = gridMap.snapToNear(userGridRaw, 20) ?: run {
                withContext(Dispatchers.Main) {
                    _errorMsg.value = "Начальная точка недоступна"
                    _phase.value = AntPhase.SELECT
                }
                return@launch
            }

            val result = routeBuilder.buildRoute(
                userPoint = userGrid,
                places = mapped,
                antCount = 20,
                iter = 150,
                end = _returnToStart.value
            )

            val allPoints = mutableListOf<GridPoint>()
            allPoints.add(userGrid)
            allPoints.addAll(mapped.map { it.snapPoint })

            val segments = routeBuilder.restoreSegments(result.order, allPoints)
            val segmentList = mutableListOf<AntRouteSegment>()
            val markerList = mutableListOf<AntRouteMarker>()

            if (segments != null) {
                for ((idx, seg) in segments.withIndex()) {
                    val mapPts = seg.points.map { gridMap.gridToQgis(it) }
                        .map { (x, y) -> GridProjection.metersToPoint(x, y) }
                    if (mapPts.size >= 2) {
                        segmentList.add(AntRouteSegment(mapPts, LEG_COLORS[idx % LEG_COLORS.size]))
                    }
                }
            }

            result.allPlaces.forEachIndexed { idx, place ->
                val pt = GridProjection.metersToPoint(place.x, place.y)
                markerList.add(
                    AntRouteMarker(
                        point = pt,
                        label = "${idx + 1}",
                        color = LEG_COLORS[idx % LEG_COLORS.size],
                        detail = place.name
                    )
                )
            }

            withContext(Dispatchers.Main) {
                _drawableRoute.value = AntDrawableRoute(segmentList, markerList, true)
                _resultDistance.doubleValue = result.dist
                _resultPlaces.value = result.allPlaces
                _phase.value = AntPhase.RESULT
            }
        }
    }


    fun buildCoworkingRoute() {
        val gridMap = antGrid ?: return
        val userPoint = _startPoint.value ?: run {
            _errorMsg.value = "Поставьте начальную точку на карте"
            return
        }
        val students = _studentCount.intValue
        if (students <= 0) {
            _errorMsg.value = "Введите количество студентов"
            return
        }
        if (_selectedCoworkingIds.value.isEmpty()) {
            _errorMsg.value = "Выберите хотя бы одну локацию"
            return
        }

        _errorMsg.value = null
        _phase.value = AntPhase.COMPUTING

        computeJob = viewModelScope.launch(Dispatchers.Default) {
            val (userX, userY) = GridProjection.pointToMeters(userPoint)
            val userGridRaw = gridMap.QGisToGrid(userX, userY)
            val userGrid = gridMap.snapToNear(userGridRaw, 20) ?: run {
                withContext(Dispatchers.Main) {
                    _errorMsg.value = "Начальная точка недоступна"
                    _phase.value = AntPhase.SELECT
                }
                return@launch
            }

            val locations = CoworkingDatabase.locations.filter { it.id in _selectedCoworkingIds.value }
            val snapped = locations.mapNotNull { loc ->
                val orig = gridMap.QGisToGrid(loc.x, loc.y)
                val snap = gridMap.snapToNear(orig, 30)
                if (snap != null) Triple(loc, snap, gridMap.bfs(userGrid, snap)) else null
            }.filter { it.third != null }

            if (snapped.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _errorMsg.value = "Ни одна локация недоступна"
                    _phase.value = AntPhase.SELECT
                }
                return@launch
            }

            val validLocs = snapped.map { it.first }
            val validDists = snapped.map { it.third!!.dist }.toDoubleArray()
            val validPaths = snapped.map { it.third!! }

            val optimizer = CoworkingOptimizer(
                locations = validLocs,
                distances = validDists,
                studentCount = students
            )
            val cwResult = optimizer.solve()

            val segmentList = mutableListOf<AntRouteSegment>()
            val markerList = mutableListOf<AntRouteMarker>()

            for ((idx, assignment) in cwResult.assignments.withIndex()) {
                val color = LEG_COLORS[idx % LEG_COLORS.size]
                val locPt = GridProjection.metersToPoint(assignment.location.x, assignment.location.y)

                if (assignment.studentsAssigned > 0 && idx < validPaths.size) {
                    val mapPts = validPaths[idx].points.map { gridMap.gridToQgis(it) }
                        .map { (x, y) -> GridProjection.metersToPoint(x, y) }
                    if (mapPts.size >= 2) {
                        segmentList.add(AntRouteSegment(mapPts, color))
                    }
                }

                markerList.add(
                    AntRouteMarker(
                        point = locPt,
                        label = "${assignment.studentsAssigned}",
                        color = if (assignment.studentsAssigned > 0) color else 0xFF9CA3AF.toInt(),
                        detail = "${assignment.location.name}\n${assignment.studentsAssigned}/${assignment.location.capacity}"
                    )
                )
            }

            withContext(Dispatchers.Main) {
                _drawableRoute.value = AntDrawableRoute(segmentList, markerList, true)
                _coworkingResult.value = cwResult
                _resultDistance.doubleValue = cwResult.totalDistance
                _phase.value = AntPhase.RESULT
            }
        }
    }

    fun resetResult() {
        computeJob?.cancel()
        computeJob = null
        _phase.value = AntPhase.SELECT
        _drawableRoute.value = null
        _resultDistance.doubleValue = 0.0
        _resultPlaces.value = emptyList()
        _coworkingResult.value = null
        _errorMsg.value = null
    }

    fun fullReset() {
        resetResult()
        _startPoint.value = null
        _awaitingStart.value = false
        _selectedPlaceIds.value = emptySet()
        _selectedCoworkingIds.value = emptySet()
        _studentCount.intValue = 10
        _returnToStart.value = false
    }

    companion object {
        val LEG_COLORS = intArrayOf(
            0xFFEA580C.toInt(),
            0xFF16A34A.toInt(),
            0xFF2563EB.toInt(),
            0xFF9333EA.toInt(),
            0xFF0891B2.toInt(),
            0xFFDC2626.toInt(),
            0xFFF59E0B.toInt(),
            0xFFCA8A04.toInt()
        )
    }
}
