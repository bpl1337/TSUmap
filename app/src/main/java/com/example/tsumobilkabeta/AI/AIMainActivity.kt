package com.example.tsumobilkabeta.AI

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tsumobilkabeta.processDrawing2px
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumobilkabeta.floatArrayToBitmap
import com.example.tsumobilkabeta.ui.theme.TSUMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val classifier = NnClassifier(this)
        lifecycleScope.launch(Dispatchers.IO) {
            classifier.loadWeights()
        }
        setContent {
            TSUMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RatingApp(classifier)
                }
            }
        }
    }
}
@Composable
fun RatingApp(classifier: NnClassifier) {
    var debugBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var currentPath by remember { mutableStateOf(Path()) }
    var resultText by remember { mutableStateOf("Нарисуйте оценку") }

    val canvasSizeDp = 300.dp
    val canvasSizePx = with(LocalDensity.current) { canvasSizeDp.toPx() }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Text("Рейтинг заведения", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(20.dp))

        Box(Modifier.size(canvasSizeDp).clipToBounds().background(Color(0xFFF5F5F5))) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
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
                }
            ) {
                drawPath(
                    path = currentPath,
                    color = Color.Black,
                    style = Stroke(width = 30f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        Spacer(Modifier.height(20.dp))


        Row(Modifier.width(canvasSizeDp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = { currentPath = Path(); resultText = "Нарисуйте оценку" },
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("Стереть", color = Color.Black)
            }
            Button(onClick = {
                if (currentPath.isEmpty) return@Button
                val data = processDrawing2px(currentPath, canvasSizePx)
                debugBitmap = floatArrayToBitmap(data)
                val pred = classifier.classify(data)
                resultText = if (pred != -1) "Оценка: $pred" else "Нейросеть не загружена!"
            }) { Text("Оценить") }
        }

        Text(resultText, Modifier.padding(20.dp), fontSize = 22.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        if (debugBitmap != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Так видит нейронка (50x50):", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.Image(
                    bitmap = debugBitmap!!,
                    contentDescription = "Debug View",
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.White)
                        .border(1.dp, androidx.compose.ui.graphics.Color.LightGray)
                )
            }
        }

    }
}
