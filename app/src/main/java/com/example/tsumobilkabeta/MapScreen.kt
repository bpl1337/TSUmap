package com.example.tsumobilkabeta

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tsumobilkabeta.AI.AIDrawingActivity
import com.example.tsumobilkabeta.AStar.AStarOverlayView
import com.example.tsumobilkabeta.Ant.AntRoutePanel
import com.example.tsumobilkabeta.Ant.AntViewModel
import com.example.tsumobilkabeta.Clustering.UI.ClusteringPanel
import com.example.tsumobilkabeta.Clustering.UI.ClusteringViewModel
import com.example.tsumobilkabeta.DecisionTree.DecisionTreeActivity
import com.example.tsumobilkabeta.Genetic.FoodRoutePanel
import com.example.tsumobilkabeta.Genetic.GeneticFoodViewModel
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
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider


@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: NavigationViewModel = viewModel(),
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},

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

    val geneticViewModel: GeneticFoodViewModel = viewModel()
    val antViewModel: AntViewModel = viewModel()
    val clusteringViewModel: ClusteringViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.loadGrid(context)
        geneticViewModel.loadGrid(context)
        antViewModel.loadGrid(context)
    }

    val mapView = remember {
        MapView(context).apply {
            mapWindow.map.isNightModeEnabled = true
            mapWindow.map.move(CameraPosition(workAreaBounds.centerPoint, 16f, 0f, 0f))
        }
    }
    val yandexMap = mapView.mapWindow.map

    var myLocationPoint by remember { mutableStateOf<Point?>(null) }

    val hasLocationPermission = remember(context) {
        {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val moveToCurrentLocation: () -> Unit = remember(context, yandexMap) {
        {
            val location = getBestLastKnownLocation(context)
            if (location == null) {
                Toast.makeText(context, "Локация недоступна", Toast.LENGTH_SHORT).show()
            } else {
                val point = Point(location.latitude, location.longitude)
                myLocationPoint = point
                yandexMap.move(
                    CameraPosition(point, 17f, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 0.4f),
                    null
                )
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allowed = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (allowed) {
            moveToCurrentLocation()
        } else {
            Toast.makeText(context, "Разрешите использование геолокации", Toast.LENGTH_SHORT).show()
        }
    }

    val onLocateMeClick = remember(
        context,
        hasLocationPermission,
        locationPermissionLauncher,
        moveToCurrentLocation
    ) {
        {
            if (hasLocationPermission()) {
                moveToCurrentLocation()
            } else {
                val activity = context as? Activity
                if (activity == null) {
                    Toast.makeText(context, "Не удалось запросить разрешение", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }
    }
    val isAutoCorrecting = remember { booleanArrayOf(false) }

    val overlayView = remember { AStarOverlayView(context) }

    LaunchedEffect(isDarkTheme) {
        mapView.mapWindow.map.isNightModeEnabled = isDarkTheme
    }

    DisposableEffect(yandexMap, myLocationPoint) {
        val marker = myLocationPoint?.let { point ->
            yandexMap.mapObjects.addPlacemark(point).apply {
                setIcon(
                    imageProviderFromDrawable(context, R.drawable.im_here),
                    IconStyle().apply {
                        anchor = PointF(0.5f, 0.5f)
                        scale = 0.05f
                    }
                )
                zIndex = 20f
            }
        }

        onDispose { marker?.let { runCatching { yandexMap.mapObjects.remove(it) } } }
    }

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
                if (isAutoCorrecting[0]) {
                    isAutoCorrecting[0] = false; return
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

                    RouteAlgorithm.ANT -> antViewModel.handleMapTap(point)
                    RouteAlgorithm.GENETIC -> geneticViewModel.handleMapTap(point)
                    RouteAlgorithm.ANOTHER -> Unit
                    RouteAlgorithm.DECISION_TREE -> Unit
                    RouteAlgorithm.CLUSTERING -> Unit
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
                setIcon(
                    imageProviderFromDrawable(context, R.drawable.startpoint),
                    IconStyle().apply { anchor = PointF(0.5f, 1f); scale = 0.5f })
                zIndex = 10f
            }
        }
        val endMarker = endPoint?.let { point ->
            yandexMap.mapObjects.addPlacemark(point).apply {
                setIcon(
                    imageProviderFromDrawable(context, R.drawable.endpoint),
                    IconStyle().apply { anchor = PointF(0.5f, 1f); scale = 0.5f })
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

    val geneticDrawable by geneticViewModel.drawableRoute
    val geneticStartPt by geneticViewModel.startPoint

    DisposableEffect(yandexMap, geneticStartPt, selectedAlgorithm) {
        val marker = if (geneticStartPt != null && selectedAlgorithm == RouteAlgorithm.GENETIC) {
            yandexMap.mapObjects.addPlacemark(geneticStartPt!!).apply {
                setIcon(
                    imageProviderFromDrawable(context, R.drawable.startpoint),
                    IconStyle().apply { anchor = PointF(0.5f, 1f); scale = 0.5f }
                )
                zIndex = 10f
            }
        } else null
        onDispose { marker?.let { runCatching { yandexMap.mapObjects.remove(it) } } }
    }

    DisposableEffect(yandexMap, geneticDrawable, selectedAlgorithm) {
        val added = mutableListOf<MapObject>()
        val dr = geneticDrawable
        if (dr != null && selectedAlgorithm == RouteAlgorithm.GENETIC) {
            val legColors = intArrayOf(
                0xFF2563EB.toInt(), 0xFF16A34A.toInt(), 0xFFEA580C.toInt(),
                0xFF9333EA.toInt(), 0xFF0891B2.toInt(), 0xFFDC2626.toInt(),
                0xFFCA8A04.toInt()
            )
            val liveColor = 0xD0FAA014.toInt()

            dr.segments.forEachIndexed { idx, pts ->
                if (pts.size >= 2) {
                    val color = if (dr.isFinal) legColors[idx % legColors.size] else liveColor
                    val width = if (dr.isFinal) 4f else 3f
                    val poly = yandexMap.mapObjects.addPolyline(Polyline(pts)).apply {
                        setStrokeColor(color); strokeWidth = width
                    }
                    added.add(poly)

                    if (dr.isFinal) {
                        val arrows = addArrowhead(yandexMap, pts, color)
                        added.addAll(arrows)
                    }
                }
            }
        }
        onDispose { added.forEach { runCatching { yandexMap.mapObjects.remove(it) } } }
    }

    val antStartPt by antViewModel.startPoint
    val antDrawable by antViewModel.drawableRoute

    DisposableEffect(yandexMap, antStartPt, selectedAlgorithm) {
        val marker = if (antStartPt != null && selectedAlgorithm == RouteAlgorithm.ANT) {
            yandexMap.mapObjects.addPlacemark(antStartPt!!).apply {
                setIcon(
                    imageProviderFromDrawable(context, R.drawable.startpoint),
                    IconStyle().apply { anchor = PointF(0.5f, 1f); scale = 0.5f }
                )
                zIndex = 10f
            }
        } else null
        onDispose { marker?.let { runCatching { yandexMap.mapObjects.remove(it) } } }
    }

    DisposableEffect(yandexMap, antDrawable, selectedAlgorithm) {
        val added = mutableListOf<MapObject>()
        val dr = antDrawable
        if (dr != null && selectedAlgorithm == RouteAlgorithm.ANT) {
            for (seg in dr.segments) {
                if (seg.points.size >= 2) {
                    val poly = yandexMap.mapObjects.addPolyline(Polyline(seg.points)).apply {
                        setStrokeColor(seg.color); strokeWidth = 4f
                    }
                    added.add(poly)
                    val arrows = addArrowhead(yandexMap, seg.points, seg.color)
                    added.addAll(arrows)
                }
            }
            for (m in dr.markers) {
                val bmp = createCircleMarkerBitmap(m.label, m.color)
                val pm = yandexMap.mapObjects.addPlacemark(m.point).apply {
                    setIcon(ImageProvider.fromBitmap(bmp), IconStyle().apply {
                        anchor = PointF(0.5f, 0.5f); scale = 0.8f
                    })
                    zIndex = 15f
                }
                added.add(pm)
            }
        }
        onDispose { added.forEach { runCatching { yandexMap.mapObjects.remove(it) } } }
    }


    DisposableEffect(yandexMap, clusteringViewModel.displayedClusters, selectedAlgorithm) {
        val added = mutableListOf<MapObject>()

        if (selectedAlgorithm == RouteAlgorithm.CLUSTERING) {
            val clusters = clusteringViewModel.displayedClusters
            if (clusters.isNotEmpty()) {
                val colors = listOf(
                    0xFFE53935.toInt(),
                    0xFF1E88E5.toInt(),
                    0xFF43A047.toInt(),
                    0xFFFB8C00.toInt(),
                    0xFF8E24AA.toInt(),
                    0xFF00897B.toInt()
                )

                clusters.forEachIndexed { index, cluster ->
                    val color = colors[index % colors.size]

                    cluster.items.forEach { establishment ->
                        val point = Point(
                            establishment.coordinate.y,
                            establishment.coordinate.x
                        )

                        val placemark = yandexMap.mapObjects.addPlacemark(point).apply {
                            setIcon(
                                ImageProvider.fromBitmap(
                                    createCircleMarkerBitmap("${index + 1}", color)
                                ),
                                IconStyle().apply {
                                    anchor = PointF(0.5f, 0.5f)
                                    scale = 0.8f
                                }
                            )
                            zIndex = 12f
                        }

                        added.add(placemark)
                    }

                    val centerPoint = Point(
                        cluster.center.y,
                        cluster.center.x
                    )

                    val centerPlacemark = yandexMap.mapObjects.addPlacemark(centerPoint).apply {
                        setIcon(
                            ImageProvider.fromBitmap(
                                createCircleMarkerBitmap("C", color)
                            ),
                            IconStyle().apply {
                                anchor = PointF(0.5f, 0.5f)
                                scale = 0.9f
                            }
                        )
                        zIndex = 15f
                    }

                    added.add(centerPlacemark)
                }
            }
        }

        onDispose {
            added.forEach { runCatching { yandexMap.mapObjects.remove(it) } }
        }
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

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .width(110.dp)
                .height(50.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.small
                )
                .border(3.dp, BorderColor, shape = MaterialTheme.shapes.small),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TSU.Map",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,

                )
        }

        AlgorithmSwitcher(
            isOpen = isAlgorithmMenuOpen,
            selectedAlgorithm = selectedAlgorithm,
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle,
            onMenuToggle = { isAlgorithmMenuOpen = !isAlgorithmMenuOpen },
            onDismiss = { isAlgorithmMenuOpen = false },
            onLocateMe = onLocateMeClick,
            onSelectAlgorithm = {
                when (it) {
                    RouteAlgorithm.ANOTHER -> context.startActivity(
                        Intent(
                            context,
                            AIDrawingActivity::class.java
                        )
                    )

                    RouteAlgorithm.DECISION_TREE -> context.startActivity(
                        Intent(
                            context,
                            DecisionTreeActivity::class.java
                        )
                    )

                    else -> viewModel.selectAlgorithm(it)
                }
                isAlgorithmMenuOpen = false
            }
        )

        AlgorithmLayer(
            selectedAlgorithm = selectedAlgorithm,
            selectionMode = selectionMode,
            aStarAnimationEnabled = viewModel.aStarAnimationEnabled.value,
            hasBothPoints = hasBothPoints,
            pathStatus = viewModel.pathStatus,
            isAnimating = viewModel.isAnimating,
            onSelectionModeChange = { viewModel.setSelectionMode(it) },
            onAStarAnimationEnabledChange = { viewModel.setAStarAnimationEnabled(it) },
            onBuildRoute = { viewModel.buildRouteIfReady() },
            onReset = { viewModel.resetPoints() },
            onSkipAnimation = { viewModel.skipAnimation() },
            geneticViewModel = geneticViewModel,
            antViewModel = antViewModel,
            clusteringViewModel = clusteringViewModel
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
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onMenuToggle: () -> Unit,
    onDismiss: () -> Unit,
    onLocateMe: () -> Unit,
    onSelectAlgorithm: (RouteAlgorithm) -> Unit
) {
    if (isOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
        )
    }

    Column(
        modifier = Modifier
            .padding(start = 12.dp, top = 35.dp, end = 12.dp, bottom = 12.dp)
            .widthIn(max = 240.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Button(onClick = onMenuToggle) { Text("☰") }

        Button(
            onClick = onThemeToggle,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(if (isDarkTheme) "🌙" else "☀")
        }
        Button(
            onClick = onLocateMe,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("📍")
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }) {
            Card(modifier = Modifier.padding(top = 8.dp)) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlgorithmOptionButton(
                        RouteAlgorithm.ASTAR.title,
                        selectedAlgorithm == RouteAlgorithm.ASTAR
                    ) { onSelectAlgorithm(RouteAlgorithm.ASTAR) }
                    AlgorithmOptionButton(
                        RouteAlgorithm.ANT.title,
                        selectedAlgorithm == RouteAlgorithm.ANT
                    ) { onSelectAlgorithm(RouteAlgorithm.ANT) }
                    AlgorithmOptionButton(
                        RouteAlgorithm.GENETIC.title,
                        selectedAlgorithm == RouteAlgorithm.GENETIC
                    ) { onSelectAlgorithm(RouteAlgorithm.GENETIC) }
                    AlgorithmOptionButton(
                        RouteAlgorithm.ANOTHER.title,
                        selectedAlgorithm == RouteAlgorithm.ANOTHER
                    ) { onSelectAlgorithm(RouteAlgorithm.ANOTHER) }
                    AlgorithmOptionButton(
                        RouteAlgorithm.DECISION_TREE.title,
                        selectedAlgorithm == RouteAlgorithm.DECISION_TREE
                    ) { onSelectAlgorithm(RouteAlgorithm.DECISION_TREE) }
                    AlgorithmOptionButton(
                        RouteAlgorithm.CLUSTERING.title,
                        selectedAlgorithm == RouteAlgorithm.CLUSTERING
                    ) { onSelectAlgorithm(RouteAlgorithm.CLUSTERING) }
                }
            }
        }
    }
}

@Composable
private fun AlgorithmLayer(
    selectedAlgorithm: RouteAlgorithm,
    selectionMode: PointSelectionMode,
    aStarAnimationEnabled: Boolean,
    hasBothPoints: Boolean,
    pathStatus: PathStatus,
    isAnimating: Boolean,
    onSelectionModeChange: (PointSelectionMode) -> Unit,
    onAStarAnimationEnabledChange: (Boolean) -> Unit,
    onBuildRoute: () -> Unit,
    onReset: () -> Unit,
    onSkipAnimation: () -> Unit,
    geneticViewModel: GeneticFoodViewModel? = null,
    antViewModel: AntViewModel? = null,
    clusteringViewModel: ClusteringViewModel? = null
) {
    var aStarPanelExpanded by rememberSaveable { mutableStateOf(false) }
    var antPanelExpanded by rememberSaveable { mutableStateOf(false) }
    var clusteringPanelExpanded by rememberSaveable { mutableStateOf(false) }
    var geneticPanelExpanded by rememberSaveable { mutableStateOf(false) }

    when (selectedAlgorithm) {
        RouteAlgorithm.ASTAR -> {
            SettingsRightPanel(
                expanded = aStarPanelExpanded,
                onToggle = { aStarPanelExpanded = !aStarPanelExpanded },
                panelMaxWidth = 280.dp,
                panelMaxHeight = 520.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeButton(
                        "Старт",
                        selectionMode == PointSelectionMode.START
                    ) { onSelectionModeChange(PointSelectionMode.START) }
                    ModeButton(
                        "Финиш",
                        selectionMode == PointSelectionMode.END
                    ) { onSelectionModeChange(PointSelectionMode.END) }
                    ModeButton(
                        "Барьеры",
                        selectionMode == PointSelectionMode.BARRIER
                    ) { onSelectionModeChange(PointSelectionMode.BARRIER) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = aStarAnimationEnabled,
                            onCheckedChange = onAStarAnimationEnabledChange
                        )
                        Text("Анимация")
                    }
                    Button(
                        onClick = onBuildRoute,
                        enabled = hasBothPoints && !isAnimating
                    ) { Text("Готово") }
                    if (isAnimating && aStarAnimationEnabled) {
                        Button(onClick = onSkipAnimation) { Text("Пропустить") }
                    }
                    Button(onClick = onReset) { Text("Сброс") }
                    when (pathStatus) {
                        PathStatus.SEARCHING -> Text(
                            "Поиск...",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        PathStatus.FOUND -> Text(
                            "Путь найден",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )

                        PathStatus.NOT_FOUND -> Text(
                            "Путь не существует",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )

                        PathStatus.NONE -> Unit
                    }
                }
            }
        }

        RouteAlgorithm.ANT -> {
            if (antViewModel != null) {
                SettingsRightPanel(
                    expanded = antPanelExpanded,
                    onToggle = { antPanelExpanded = !antPanelExpanded },
                    panelMaxWidth = 320.dp,
                    panelMaxHeight = 720.dp
                ) {
                    AntRoutePanel(viewModel = antViewModel)
                }
            }
        }

        RouteAlgorithm.CLUSTERING -> {
            if (clusteringViewModel != null) {
                SettingsRightPanel(
                    expanded = clusteringPanelExpanded,
                    onToggle = { clusteringPanelExpanded = !clusteringPanelExpanded },
                    panelMaxWidth = 280.dp,
                    panelMaxHeight = 320.dp
                ) {
                    ClusteringPanel(viewModel = clusteringViewModel)
                }
            }
        }


        RouteAlgorithm.GENETIC -> {
            if (geneticViewModel != null) {
                SettingsRightPanel(
                    expanded = geneticPanelExpanded,
                    onToggle = { geneticPanelExpanded = !geneticPanelExpanded },
                    panelMaxWidth = 280.dp,
                    panelMaxHeight = 480.dp
                ) {
                    FoodRoutePanel(viewModel = geneticViewModel)
                }
            }
        }

        RouteAlgorithm.ANOTHER -> Unit
        RouteAlgorithm.DECISION_TREE -> Unit
    }
}

@Composable
private fun SettingsRightPanel(
    expanded: Boolean,
    onToggle: () -> Unit,
    panelMaxWidth: Dp,
    panelMaxHeight: Dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 35.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onToggle) { Text("⚙") }
            AnimatedVisibility(visible = expanded) {
                Card(modifier = Modifier.widthIn(max = panelMaxWidth)) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(max = panelMaxHeight)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

private fun addArrowhead(yandexMap: Map, points: List<Point>, color: Int): List<MapObject> {
    if (points.size < 2) return emptyList()

    val tip = points.last()
    val tail = points[maxOf(0, points.size - 6)]

    val cosLat = Math.cos(Math.toRadians(tip.latitude))

    val dx = (tip.longitude - tail.longitude) * cosLat
    val dy = tip.latitude - tail.latitude
    val len = Math.sqrt(dx * dx + dy * dy)
    if (len < 1e-10) return emptyList()

    val ux = dx / len
    val uy = dy / len
    val arrowLen = 15.0 / 111_320.0

    fun wing(angleDeg: Double): Point {
        val a = Math.toRadians(angleDeg)
        val wx = (ux * Math.cos(a) - uy * Math.sin(a)) * arrowLen / cosLat
        val wy = (ux * Math.sin(a) + uy * Math.cos(a)) * arrowLen
        return Point(tip.latitude + wy, tip.longitude + wx)
    }

    return listOf(
        yandexMap.mapObjects.addPolyline(Polyline(listOf(wing(145.0), tip))).apply {
            setStrokeColor(color); strokeWidth = 3f
        },
        yandexMap.mapObjects.addPolyline(Polyline(listOf(wing(-145.0), tip))).apply {
            setStrokeColor(color); strokeWidth = 3f
        }
    )
}

data class WorkAreaBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double
) {
    val centerPoint: Point
        get() = Point(
            (minLatitude + maxLatitude) / 2.0,
            (minLongitude + maxLongitude) / 2.0
        )

    fun contains(point: Point) =
        point.latitude in minLatitude..maxLatitude && point.longitude in minLongitude..maxLongitude

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
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}

private fun createCircleMarkerBitmap(text: String, fillColor: Int, size: Int = 64): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = android.graphics.Paint.Style.FILL
    }
    val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = size * 0.42f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }

    val cx = size / 2f
    val cy = size / 2f
    val radius = size / 2f - 4f

    canvas.drawCircle(cx, cy, radius, bgPaint)
    canvas.drawCircle(cx, cy, radius, borderPaint)

    val fm = textPaint.fontMetrics
    canvas.drawText(text, cx, cy - (fm.ascent + fm.descent) / 2, textPaint)

    return bitmap
}

private val RouteAlgorithm.title: String
    get() = when (this) {
        RouteAlgorithm.ASTAR -> "A*"
        RouteAlgorithm.ANT -> "Муравьи"
        RouteAlgorithm.GENETIC -> "Генетика (еда)"
        RouteAlgorithm.ANOTHER -> "Нейронка"
        RouteAlgorithm.DECISION_TREE -> "Дерево решений"
        RouteAlgorithm.CLUSTERING -> "Кластеризация"
    }


private fun getBestLastKnownLocation(context: Context): Location? {
    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) return null

    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )

    var best: Location? = null
    for (provider in providers) {
        val location =
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() ?: continue
        if (location.time > (best?.time ?: Long.MIN_VALUE)) {
            best = location
        }
    }
    return best
}
