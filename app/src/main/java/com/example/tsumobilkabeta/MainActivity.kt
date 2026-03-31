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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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

@PreviewScreenSizes
@Composable
fun TSUmobilkaBETAApp() {
    var currentDestination by remember { mutableStateOf(AppDestinations.ASTAR) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // Держим экраны в композиции: карта не пересоздается при переключении вкладок.
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

    // Инициализируем PathFindingCoordinator один раз
    val context = LocalContext.current
    val coordinator = remember {
        PathFindingCoordinator(context).apply {
            initialize()
        }
    }

    // Обрабатываем точки когда обе установлены
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
                // Логируем данные для передачи алгоритму A*
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
                        // Маркер ставим строго в место тапа, без сдвигов на UI.
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
    var prediction by remember { mutableStateOf<DigitPrediction?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<DigitRecognizer?>(null) }
    
    // Инициализируем распознаватель один раз
    LaunchedEffect(Unit) {
        // Используем пустую заглушку, друг может заменить на свою реализацию
        val rec = NoOpDigitRecognizer()
        if (rec.loadModel()) {
            recognizer = rec
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Холст для рисования
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DigitDrawingCanvas(
                    canvasSize = 50,
                    drawingCanvasSize = 384,
                    onDrawComplete = { pixelData ->
                        if (recognizer != null) {
                            isLoading = true
                            // Передаём данные в нейронку
                            Log.d("NeuralNetwork", "Recognizing digit from ${pixelData.size} pixels")
                            val result = recognizer!!.predict(pixelData)
                            prediction = result
                            isLoading = false
                            Log.d("NeuralNetwork", "Prediction: digit=${result.digit}, confidence=${result.confidence}")
                        } else {
                            Log.e("NeuralNetwork", "Recognizer not initialized")
                        }
                    }
                )
            }
        }
        
        // Результат предсказания
        if (prediction != null && !isLoading && prediction!!.digit >= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Результат: ${prediction!!.digit}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Уверенность: ${(prediction!!.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Анализирую рисунок...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TSUmobilkaBETATheme {
        MapRouteScreen()
    }
}