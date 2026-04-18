package com.example.tsumobilkabeta.AI

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.tsumobilkabeta.floatArrayToBitmap
import com.example.tsumobilkabeta.processDrawing2px
import com.example.tsumobilkabeta.ui.theme.TSUMapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIDrawingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val classifier = NnClassifier(this)
        lifecycleScope.launch(Dispatchers.IO) {
            classifier.loadWeights()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val isDark = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_dark_theme", false)

        setContent {
            TSUMapTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AIDrawingScreen(
                        classifier = classifier,
                        onRateEstablishment = {
                            startActivity(
                                Intent(
                                    this@AIDrawingActivity,
                                    AIMainActivity::class.java
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowInsetsControllerCompat(window, window.decorView).hide(
                WindowInsetsCompat.Type.systemBars()
            )
        }
    }
}

@Composable
private fun AIDrawingScreen(
    classifier: NnClassifier,
    onRateEstablishment: () -> Unit
) {
    var debugBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var currentPath by remember { mutableStateOf(Path()) }
    var digitResult by remember { mutableStateOf(-1) }
    var confidence by remember { mutableStateOf(0f) }

    val canvasSizeDp = 300.dp
    val canvasSizePx = with(LocalDensity.current) { canvasSizeDp.toPx() }

    LaunchedEffect(currentPath) {
        if (currentPath.isEmpty) {
            digitResult = -1
            confidence = 0f
            debugBitmap = null
            return@LaunchedEffect
        }
        delay(80)
        val result = withContext(Dispatchers.Default) {
            val pixels = processDrawing2px(currentPath)
            val (digit, conf) = classifier.classifyWithConfidence(pixels)
            Triple(digit, conf, floatArrayToBitmap(pixels))
        }
        digitResult = result.first
        confidence = result.second
        debugBitmap = result.third
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Рейтинг заведения", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(20.dp))

        Box(
            Modifier
                .size(canvasSizeDp)
                .clipToBounds()
                .background(Color(0xFFF5F5F5))
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            if (down.position.x in 0f..canvasSizePx && down.position.y in 0f..canvasSizePx) {
                                currentPath.moveTo(down.position.x, down.position.y)
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.first()
                                    if (change.pressed) {
                                        val pos = change.position
                                        if (pos.x in 0f..canvasSizePx && pos.y in 0f..canvasSizePx) {
                                            currentPath.lineTo(pos.x, pos.y)
                                            change.consume()
                                            val nextPath = Path().apply { addPath(currentPath) }
                                            currentPath = nextPath
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    }) {
                drawPath(
                    path = currentPath,
                    color = Color.Black,
                    style = Stroke(width = 30f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                currentPath = Path()
                digitResult = -1
                confidence = 0f
                debugBitmap = null
            }, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
        ) {
            Text("Стереть", color = Color.Black)
        }

        Spacer(Modifier.height(12.dp))

        if (digitResult != -1) {
            Text(
                text = "$digitResult",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = "${(confidence * 100).toInt()}% уверен",
                fontSize = 16.sp,
                color = Color(0xFF558B2F)
            )
        } else {
            Text(text = "Нарисуйте оценку", fontSize = 18.sp, color = Color.Gray)
        }

        if (debugBitmap != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Так видит нейронка (50x50):", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Image(
                    bitmap = debugBitmap!!,
                    contentDescription = "Debug View",
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.White)
                        .border(1.dp, Color.LightGray)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onRateEstablishment,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Поставить оценку заведению", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))
    }
}
