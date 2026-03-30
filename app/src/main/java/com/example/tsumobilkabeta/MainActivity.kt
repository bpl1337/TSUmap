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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.tsumobilkabeta.ui.theme.TSUmobilkaBETATheme
import android.graphics.PointF

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
    var currentDestination by remember { mutableStateOf(AppDestinations.HOME) }

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
            when (currentDestination) {
                AppDestinations.HOME -> MapRouteScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.FAVORITES -> PlaceholderScreen(
                    title = "Favorites",
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.PROFILE -> PlaceholderScreen(
                    title = "Profile",
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    FAVORITES("Favorites", R.drawable.ic_favorite),
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Карта маршрута",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Выбери метку и тапни по карте",
            style = MaterialTheme.typography.bodyMedium
        )

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

            if (startPoint == null || endPoint == null) {
                Text(
                    text = "Зажми и двигай карту, щипком меняй масштаб",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .background(Color(0xAA000000), RoundedCornerShape(8.dp))
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TSUmobilkaBETATheme {
        MapRouteScreen()
    }
}