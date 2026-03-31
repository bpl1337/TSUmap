package com.example.tsumobilkabeta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.tsumobilkabeta.AI.RatingApp
import com.example.tsumobilkabeta.ui.theme.TSUmobilkaBETATheme
import android.graphics.PointF
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TSUmobilkaBETATheme {
                TSUmobilkaBETAApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun TSUmobilkaBETAApp() {
    var currentDestination by remember { mutableStateOf(AppDestinations.ASTAR) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TSUmobilka",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                AppDestinations.entries.forEach { destination ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painterResource(destination.icon),
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        selected = destination == currentDestination,
                        onClick = {
                            currentDestination = destination
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppTopBar(
                    title = currentDestination.label,
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                MapRouteScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .tabVisibility(currentDestination == AppDestinations.ASTAR)
                )
                NeuralNetworkScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .tabVisibility(currentDestination == AppDestinations.NEURAL_NETWORK)
                )
                PlaceholderScreen(
                    title = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                        .tabVisibility(currentDestination == AppDestinations.PROFILE)
                )
            }
        }
    }
}

private fun Modifier.tabVisibility(visible: Boolean): Modifier {
    return this
        .alpha(if (visible) 1f else 0f)
        .zIndex(if (visible) 1f else 0f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    onMenuClick: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            Button(
                onClick = onMenuClick,
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        }
    )
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    ASTAR("A*", R.drawable.ic_astar),
    NEURAL_NETWORK("Нейронка", R.drawable.ic_neural_network),
    PROFILE("Profile", R.drawable.ic_account_box),
}

private enum class MarkerMode {
    START,
    END,
}

private data class MapPoint(
    val x: Float,
    val y: Float,
)

@Composable
fun MapRouteScreen(modifier: Modifier = Modifier) {
    var startPoint by remember { mutableStateOf<MapPoint?>(null) }
    var endPoint by remember { mutableStateOf<MapPoint?>(null) }
    var markerMode by remember { mutableStateOf(MarkerMode.START) }
    var lastPathData by remember { mutableStateOf<PathFindingData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    val context = LocalContext.current
    val coordinator = remember {
        PathFindingCoordinator(context).apply {
            initialize()
        }
    }


    LaunchedEffect(startPoint, endPoint) {
        if (startPoint != null && endPoint != null) {
            Log.d("PathFinding", "Processing route from START to END")
            val pathData = coordinator.processPathRequestWithAutoSnap(
                PointF(startPoint!!.x, startPoint!!.y),
                PointF(endPoint!!.x, endPoint!!.y)
            )
            if (pathData != null) {
                lastPathData = pathData
                errorMessage = null
                Log.d("PathFinding", """
                    |✓ ROUTE VALID
                    |Start on usermap: (${pathData.startPoint.x.toInt()}, ${pathData.startPoint.y.toInt()})
                    |End on usermap: (${pathData.endPoint.x.toInt()}, ${pathData.endPoint.y.toInt()})
                    |Start on walkable: (${pathData.walkableStart.x.toInt()}, ${pathData.walkableStart.y.toInt()})
                    |End on walkable: (${pathData.walkableEnd.x.toInt()}, ${pathData.walkableEnd.y.toInt()})
                    |Walkable map size: ${pathData.walkableMapWidth}x${pathData.walkableMapHeight}
                """.trimMargin())
            } else {
                errorMessage = "Ошибка обработки маршрута"
                lastPathData = null
                Log.e("PathFinding", "✗ ROUTE PROCESSING FAILED")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleMarkerButton(
                text = "Начало",
                selected = markerMode == MarkerMode.START,
                onClick = { markerMode = MarkerMode.START }
            )
            ToggleMarkerButton(
                text = "Конец",
                selected = markerMode == MarkerMode.END,
                onClick = { markerMode = MarkerMode.END }
            )
            Button(onClick = {
                startPoint = null
                endPoint = null
            }) {
                Text("Сброс")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFECECEC))
        ) {
            AndroidView<TiledRouteMapView>(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    TiledRouteMapView(context).apply {
                        setMapImageResource(R.drawable.usermap)
                    }
                },
                update = { mapView: TiledRouteMapView ->
                    mapView.startPoint = startPoint?.let { PointF(it.x, it.y) }
                    mapView.endPoint = endPoint?.let { PointF(it.x, it.y) }
                    mapView.onMapTap = { sourcePoint ->
                        if (markerMode == MarkerMode.START) {
                            startPoint = MapPoint(sourcePoint.x, sourcePoint.y)
                        } else {
                            endPoint = MapPoint(sourcePoint.x, sourcePoint.y)
                        }
                    }
                }
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .background(Color(0xAAC62828), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ToggleMarkerButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = if (selected) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Text(text)
    }
}


@Composable
private fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}

@Composable
fun NeuralNetworkScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val classifier = remember(context.applicationContext) {
        KnnClassifier(context.applicationContext)
    }

    Box(modifier = modifier.fillMaxSize()) {
        RatingApp(classifier = classifier)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TSUmobilkaBETATheme {
        MapRouteScreen()
    }
}