package com.example.tsumobilkabeta.AI

import android.os.Bundle
import com.example.tsumobilkabeta.KnnClassifier
import com.example.tsumobilkabeta.processDrawingImproved
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val classifier = KnnClassifier(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RatingApp(classifier)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingApp(classifier: KnnClassifier) {
    var isProductionMode by remember { mutableStateOf(false) }

    var currentPath by remember { mutableStateOf(Path()) }
    var resultText by remember { mutableStateOf("Нарисуйте оценку") }
    var digitToLearn by remember { mutableStateOf("") }

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
                onClick = { currentPath = Path(); resultText = "Холст очищен" },
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("Стереть", color = Color.Black)
            }
            Button(onClick = {
                if (currentPath.isEmpty) return@Button
                val data = processDrawingImproved(currentPath, canvasSizePx)
                val pred = classifier.classify(data)
                resultText = if (pred != -1) "Оценка: $pred" else "Система не обучена!"
            }) { Text("Оценить") }
        }

        Text(resultText, Modifier.padding(20.dp), fontSize = 22.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)

        if (!isProductionMode) {
            Spacer(Modifier.weight(1f))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Панель обучения", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = digitToLearn,
                            onValueChange = { digitToLearn = it },
                            label = { Text("Цифра") },
                            modifier = Modifier.width(90.dp),
                            singleLine = true
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = {
                            val d = digitToLearn.toIntOrNull()
                            if (d != null && !currentPath.isEmpty) {
                                classifier.addTemplate(d, processDrawingImproved(currentPath, canvasSizePx))
                                currentPath = Path()
                                resultText = "Эталон '$d' сохранен!"
                            }
                        }) { Text("Запомнить") }
                    }

                    TextButton(onClick = {
                        classifier.removeLastTemplate()
                        resultText = "Последний эталон удален"
                    }) {
                        Text("Удалить последнюю запись (Отмена)", color = Color.Red)
                    }
                }
            }
        }
    }
}
