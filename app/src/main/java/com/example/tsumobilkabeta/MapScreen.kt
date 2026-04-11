package com.example.tsumobilkabeta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tsumobilkabeta.AI.AIMainActivity
import com.example.tsumobilkabeta.AStar.AStarOverlayView
import com.example.tsumobilkabeta.DecisionTree.DecisionTreeActivity
import com.example.tsumobilkabeta.ui.theme.BorderColor
import com.example.tsumobilkabeta.ui.theme.BorderFill
import com.example.tsumobilkabeta.ui.theme.PathColor
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.LinearRing
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: NavigationViewModel = viewModel(),
    workAreaBounds: WorkAreaBounds = WorkAreaBounds(
        minLatitude = 56.462946,
        maxLatitude = 56.476156,
        minLongitude = 84.932614,
        maxLongitude = 84.957602
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val route = viewModel.routePoints.value
    val selectionMode = viewModel.selectionMode.value
    val selectedAlgorithm = viewModel.selectedAlgorithm.value
    val hasBothPoints = viewModel.startPoint.value != null && viewModel.endPoint.value != null
    var isAlgorithmMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadGrid(context) }

    val mapView = remember {
        MapView(context).apply {
            mapWindow.map.isNightModeEnabled = true
            mapWindow.map.move(CameraPosition(workAreaBounds.centerPoint, 16f, 0f, 0f))
        }
    }
    val yandexMap = mapView.mapWindow.map

    val overlayView = remember { AStarOverlayView(context) }
    val isAutoCorrecting = remember { booleanArrayOf(false) }

    val boundsListener = remember {
        object : CameraListener {
            override fun onCameraPositionChanged(
                map: Map,
                cameraPosition: CameraPosition,
                cameraUpdateReason: CameraUpdateReason,
                finished: Boolean
            ) {
                overlayView.post { overlayView.invalidate() }
                if (!finished) return
                if (isAutoCorrecting[0]) { isAutoCorrecting[0] = false; return }

                val clampedLat = cameraPosition.target.latitude.coerceIn(
                    workAreaBounds.minLatitude, workAreaBounds.maxLatitude
                )
                val clampedLon = cameraPosition.target.longitude.coerceIn(
                    workAreaBounds.minLongitude, workAreaBounds.maxLongitude
                )
                if (clampedLat != cameraPosition.target.latitude || clampedLon != cameraPosition.target.longitude) {
                    isAutoCorrecting[0] = true
                    map.move(
                        CameraPosition(Point(clampedLat, clampedLon), cameraPosition.zoom, cameraPosition.azimuth, cameraPosition.tilt),
                        Animation(Animation.Type.SMOOTH, 0.2f), null
                    )
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(yandexMap) {
        yandexMap.addCameraListener(boundsListener)

        val tapListener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                if (!workAreaBounds.contains(point)) return
                when (viewModel.selectedAlgorithm.value) {
                    RouteAlgorithm.ASTAR -> when (viewModel.selectionMode.value) {
                        PointSelectionMode.START -> viewModel.setStartPoint(point)
                        PointSelectionMode.END -> viewModel.setEndPoint(point)
                        PointSelectionMode.BARRIER -> viewModel.toggleBarrier(point)
                    }
                    RouteAlgorithm.ANT -> viewModel.setStartPoint(point)
                    RouteAlgorithm.ANOTHER -> Unit
                    RouteAlgorithm.DECISION_TREE -> Unit
                }
            }
            override fun onMapLongTap(map: Map, point: Point) = Unit
        }

        yandexMap.addInputListener(tapListener)
        onDispose {
            yandexMap.removeInputListener(tapListener)
            yandexMap.removeCameraListener(boundsListener)
        }
    }

    DisposableEffect(yandexMap, route) {
        val routePolyline = if (route.size >= 2) {
            yandexMap.mapObjects.addPolyline(Polyline(route)).apply {
                setStrokeColor(PathColor.toArgb())
                strokeWidth = 6f
            }
        } else null
        onDispose { routePolyline?.let { yandexMap.mapObjects.remove(it) } }
    }

    DisposableEffect(yandexMap, viewModel.startPoint.value, viewModel.endPoint.value) {
        val startPoint = viewModel.startPoint.value
        val endPoint = viewModel.endPoint.value

        val startMarker = startPoint?.let { point ->
            yandexMap.mapObjects.addPlacemark(point).apply {
                setIcon(imageProviderFromDrawable(context, R.drawable.startpoint), IconStyle().apply { anchor = PointF(0.5f, 1f); scale = 0.5f })
                zIndex = 10f
            }
        }
        val endMarker = endPoint?.let { point ->
            yandexMap.mapObjects.addPlacemark(point).apply {
                setIcon(imageProviderFromDrawable(context, R.drawable.endpoint), IconStyle().apply { anchor = PointF(0.5f, 1f); scale = 0.5f })
                zIndex = 10f
            }
        }
        onDispose {
            startMarker?.let { yandexMap.mapObjects.remove(it) }
            endMarker?.let { yandexMap.mapObjects.remove(it) }
        }
    }

    DisposableEffect(yandexMap, workAreaBounds) {
        val polygon = Polygon(LinearRing(workAreaBounds.toRectanglePoints()), emptyList())
        val rect = yandexMap.mapObjects.addPolygon(polygon).apply {
            fillColor = BorderFill.toArgb()
            strokeColor = BorderColor.toArgb()
            strokeWidth = 3f
        }
        onDispose { yandexMap.mapObjects.remove(rect) }
    }

    val barriers = viewModel.barrierNodes.value
    val animClosed = viewModel.animClosed.value
    val animOpen = viewModel.animOpen.value
    val animCurrent = viewModel.animCurrent.value
    val animPath = viewModel.animPath.value

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { _ -> overlayView.apply { mapWindow = mapView.mapWindow } },
            update = { view ->
                view.grid = viewModel.currentGrid
                view.barriers = barriers
                view.closedSet = animClosed
                view.openSet = animOpen
                view.currentNode = animCurrent
                view.pathNodes = animPath
                view.invalidate()
            }
        )

        AlgorithmLayer(
            selectedAlgorithm = selectedAlgorithm,
            selectionMode = selectionMode,
            hasBothPoints = hasBothPoints,
            pathStatus = viewModel.pathStatus,
            isAnimating = viewModel.isAnimating,
            onSelectionModeChange = { viewModel.setSelectionMode(it) },
            onBuildRoute = { viewModel.buildRouteIfReady() },
            onReset = { viewModel.resetPoints() },
            onSkipAnimation = { viewModel.skipAnimation() }
        )

        AlgorithmSwitcher(
            isOpen = isAlgorithmMenuOpen,
            selectedAlgorithm = selectedAlgorithm,
            onMenuToggle = { isAlgorithmMenuOpen = !isAlgorithmMenuOpen },
            onDismiss = { isAlgorithmMenuOpen = false },
            onSelectAlgorithm = {
                when (it) {
                    RouteAlgorithm.ANOTHER -> context.startActivity(Intent(context, AIMainActivity::class.java))
                    RouteAlgorithm.DECISION_TREE -> context.startActivity(Intent(context, DecisionTreeActivity::class.java))
                    else -> viewModel.selectAlgorithm(it)
                }
                isAlgorithmMenuOpen = false
            }
        )
    }

    if (viewModel.showNoPathDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoPathDialog() },
            title = { Text("Маршрут не найден") },
            text = { Text("Все возможные пути перекрыты. Измените расположение барьеров или начальной/конечной точки и попробуйте снова.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissNoPathDialog() }) {
                    Text("Понятно")
                }
            }
        )
    }
}

@Composable
private fun AlgorithmSwitcher(
    isOpen: Boolean,
    selectedAlgorithm: RouteAlgorithm,
    onMenuToggle: () -> Unit,
    onDismiss: () -> Unit,
    onSelectAlgorithm: (RouteAlgorithm) -> Unit
) {
    if (isOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
        )
    }

    Column(
        modifier = Modifier.padding(start = 12.dp, top = 35.dp, end = 12.dp, bottom = 12.dp).widthIn(max = 240.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Button(onClick = onMenuToggle) { Text("☰") }

        AnimatedVisibility(visible = isOpen, enter = slideInHorizontally { -it }, exit = slideOutHorizontally { -it }) {
            Card(modifier = Modifier.padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlgorithmOptionButton(RouteAlgorithm.ASTAR.title, selectedAlgorithm == RouteAlgorithm.ASTAR) { onSelectAlgorithm(RouteAlgorithm.ASTAR) }
                    AlgorithmOptionButton(RouteAlgorithm.ANT.title, selectedAlgorithm == RouteAlgorithm.ANT) { onSelectAlgorithm(RouteAlgorithm.ANT) }
                    AlgorithmOptionButton(RouteAlgorithm.ANOTHER.title, selectedAlgorithm == RouteAlgorithm.ANOTHER) { onSelectAlgorithm(RouteAlgorithm.ANOTHER) }
                    AlgorithmOptionButton(RouteAlgorithm.DECISION_TREE.title, selectedAlgorithm == RouteAlgorithm.DECISION_TREE) { onSelectAlgorithm(RouteAlgorithm.DECISION_TREE) }
                }
            }
        }
    }
}

@Composable
private fun AlgorithmLayer(
    selectedAlgorithm: RouteAlgorithm,
    selectionMode: PointSelectionMode,
    hasBothPoints: Boolean,
    pathStatus: PathStatus,
    isAnimating: Boolean,
    onSelectionModeChange: (PointSelectionMode) -> Unit,
    onBuildRoute: () -> Unit,
    onReset: () -> Unit,
    onSkipAnimation: () -> Unit
) {
    when (selectedAlgorithm) {
        RouteAlgorithm.ASTAR -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 12.dp, top = 35.dp, end = 12.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top
            ) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton("Старт", selectionMode == PointSelectionMode.START) { onSelectionModeChange(PointSelectionMode.START) }
                    ModeButton("Финиш", selectionMode == PointSelectionMode.END) { onSelectionModeChange(PointSelectionMode.END) }
                    ModeButton("Барьеры", selectionMode == PointSelectionMode.BARRIER) { onSelectionModeChange(PointSelectionMode.BARRIER) }
                    Button(onClick = onBuildRoute, enabled = hasBothPoints && !isAnimating) { Text("Готово") }
                    if (isAnimating) {
                        Button(onClick = onSkipAnimation) { Text("Пропустить") }
                    }
                    Button(onClick = onReset) { Text("Сброс") }
                    when (pathStatus) {
                        PathStatus.SEARCHING -> Text("Поиск...", color = Color(0xFFFFCC00), style = MaterialTheme.typography.bodySmall)
                        PathStatus.FOUND -> Text("Путь найден", color = Color(0xFF00EE44), style = MaterialTheme.typography.bodySmall)
                        PathStatus.NOT_FOUND -> Text("Путь не существует", color = Color(0xFFFF4444), style = MaterialTheme.typography.bodySmall)
                        PathStatus.NONE -> Unit
                    }
                }
            }
        }

        RouteAlgorithm.ANT -> {
            Box(modifier = Modifier.fillMaxSize().padding(top = 35.dp, end = 12.dp), contentAlignment = Alignment.TopEnd) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onBuildRoute) { Text("Построить\nмаршрут") }
                    Button(onClick = onReset) { Text("Сброс") }
                }
            }
        }

        RouteAlgorithm.ANOTHER -> Unit
        RouteAlgorithm.DECISION_TREE -> Unit
    }
}

data class WorkAreaBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double
) {
    val centerPoint: Point get() = Point((minLatitude + maxLatitude) / 2.0, (minLongitude + maxLongitude) / 2.0)

    fun contains(point: Point) = point.latitude in minLatitude..maxLatitude && point.longitude in minLongitude..maxLongitude

    fun toRectanglePoints() = listOf(
        Point(maxLatitude, minLongitude), Point(maxLatitude, maxLongitude),
        Point(minLatitude, maxLongitude), Point(minLatitude, minLongitude)
    )
}

private fun imageProviderFromDrawable(context: Context, resId: Int): ImageProvider {
    val drawable = context.getDrawable(resId) ?: return ImageProvider.fromResource(context, resId)
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    return ImageProvider.fromBitmap(bitmap)
}

@Composable
private fun AlgorithmOptionButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) { Text(text) }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}

private val RouteAlgorithm.title: String get() = when (this) {
    RouteAlgorithm.ASTAR -> "A*"
    RouteAlgorithm.ANT -> "Муравьи"
    RouteAlgorithm.ANOTHER -> "Нейронка"
    RouteAlgorithm.DECISION_TREE -> "Дерево решений"
}
