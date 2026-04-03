package com.example.tsumobilkabeta

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tsumobilkabeta.ui.theme.BorderColor
import com.example.tsumobilkabeta.ui.theme.BorderFill
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.LinearRing
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.AndroidView

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

    LaunchedEffect(Unit) {
        viewModel.loadGrid(context)
    }

    val mapView = remember {
        MapView(context).apply {
            mapWindow.map.move(CameraPosition(workAreaBounds.centerPoint, 16f, 0f, 0f))
        }
    }
    val yandexMap = mapView.mapWindow.map

    val isAutoCorrecting = remember { booleanArrayOf(false) }

    val boundsListener = remember {
        object : CameraListener {
            override fun onCameraPositionChanged(
                map: Map,
                cameraPosition: CameraPosition,
                cameraUpdateReason: CameraUpdateReason,
                finished: Boolean
            ) {
                if (!finished) return
                if (isAutoCorrecting[0]) {
                    isAutoCorrecting[0] = false
                    return
                }

                val clampedLat = cameraPosition.target.latitude.coerceIn(
                    workAreaBounds.minLatitude,
                    workAreaBounds.maxLatitude
                )
                val clampedLon = cameraPosition.target.longitude.coerceIn(
                    workAreaBounds.minLongitude,
                    workAreaBounds.maxLongitude
                )

                if (clampedLat != cameraPosition.target.latitude || clampedLon != cameraPosition.target.longitude) {
                    isAutoCorrecting[0] = true
                    map.move(
                        CameraPosition(
                            Point(clampedLat, clampedLon),
                            cameraPosition.zoom,
                            cameraPosition.azimuth,
                            cameraPosition.tilt
                        ),
                        Animation(Animation.Type.SMOOTH, 0.2f),
                        null
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(yandexMap) {
        yandexMap.addCameraListener(boundsListener)

        val tapListener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                if (!workAreaBounds.contains(point)) return

                if (viewModel.startPoint.value == null) {
                    viewModel.setStartPoint(point)
                } else if (viewModel.endPoint.value == null) {
                    viewModel.setEndPoint(point)
                } else {
                    viewModel.resetPoints()
                    viewModel.setStartPoint(point)
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

    DisposableEffect(yandexMap, workAreaBounds) {
        val polygon = Polygon(LinearRing(workAreaBounds.toRectanglePoints()), emptyList())

        val rect = yandexMap.mapObjects.addPolygon(polygon).apply {
            fillColor = BorderFill.toArgb()
            strokeColor = BorderColor.toArgb()
            strokeWidth = 3f
        }

        onDispose {
            yandexMap.mapObjects.remove(rect)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView }
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

    fun contains(point: Point): Boolean {
        return point.latitude in minLatitude..maxLatitude &&
            point.longitude in minLongitude..maxLongitude
    }

    fun toRectanglePoints(): List<Point> {
        val northWest = Point(maxLatitude, minLongitude)
        val northEast = Point(maxLatitude, maxLongitude)
        val southEast = Point(minLatitude, maxLongitude)
        val southWest = Point(minLatitude, minLongitude)
        return listOf(northWest, northEast, southEast, southWest)
    }
}