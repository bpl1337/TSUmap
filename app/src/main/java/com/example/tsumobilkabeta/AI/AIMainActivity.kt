package com.example.tsumobilkabeta.AI

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.example.tsumobilkabeta.processDrawing2px
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumobilkabeta.floatArrayToBitmap
import com.example.tsumobilkabeta.ui.theme.TSUMapTheme
import java.io.File
private const val ADMIN_MODE = true

class AIMainActivity : ComponentActivity() {
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
    val context = LocalContext.current

    var debugBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var currentPath by remember { mutableStateOf(Path()) }
    var digitResult by remember { mutableStateOf(-1) }
    var confidence by remember { mutableStateOf(0f) }
    var adminStatus by remember { mutableStateOf("") }
    var countsPerDigit by remember { mutableStateOf(countSamplesPerDigit(context)) }

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
            val pixels = processDrawing2px(currentPath, canvasSizePx)
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

        Button(
            onClick = {
                currentPath = Path()
                digitResult = -1
                confidence = 0f
                debugBitmap = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
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
            Text(
                text = "Нарисуйте оценку",
                fontSize = 18.sp,
                color = Color.Gray
            )
        }

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
                        .border(1.dp, Color.LightGray)
                )
            }
        }

        if (ADMIN_MODE) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            val totalSaved = countsPerDigit.sum()
            Text("Панель датасета", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text("Всего: $totalSaved образцов", fontSize = 11.sp, color = Color.Gray)

            if (adminStatus.isNotEmpty()) {
                Text(adminStatus, fontSize = 12.sp, color = Color(0xFF1565C0))
            }

            Spacer(Modifier.height(8.dp))

            val digits = (0..9).toList()
            Column {
                listOf(digits.take(5), digits.drop(5)).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { digit ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(
                                    onClick = {
                                        if (!currentPath.isEmpty) {
                                            val pixels = processDrawing2px(currentPath, canvasSizePx)
                                            saveSample(context, digit, pixels)
                                            countsPerDigit = countsPerDigit.copyOf().also { it[digit]++ }
                                            currentPath = Path()
                                            adminStatus = "Сохранено: цифра $digit"
                                        } else {
                                            adminStatus = "Сначала нарисуй!"
                                        }
                                    },
                                    modifier = Modifier.size(48.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(digit.toString(), fontSize = 16.sp)
                                }
                                Text(
                                    text = "${countsPerDigit[digit]}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val deletedDigit = deleteLastSample(context)
                    if (deletedDigit != -1) {
                        countsPerDigit = countsPerDigit.copyOf().also {
                            it[deletedDigit] = maxOf(0, it[deletedDigit] - 1)
                        }
                        adminStatus = "Удалена последняя запись (цифра $deletedDigit)"
                    } else {
                        adminStatus = "Датасет пуст"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
            ) {
                Text("Удалить последнюю запись", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}


private fun saveSample(context: android.content.Context, label: Int, pixels: FloatArray) {
    File(context.filesDir, "dataset_2px.txt")
        .appendText("$label|${pixels.joinToString(",")}\n")
}

private fun deleteLastSample(context: android.content.Context): Int {
    val file = File(context.filesDir, "dataset_2px.txt")
    if (!file.exists()) return -1
    val lines = file.readLines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return -1
    val lastDigit = lines.last().substringBefore('|').toIntOrNull() ?: -1
    file.writeText(lines.dropLast(1).joinToString("\n") + "\n")
    return lastDigit
}

private fun countSamplesPerDigit(context: android.content.Context): IntArray {
    val counts = IntArray(10)
    val file = File(context.filesDir, "dataset_2px.txt")
    if (!file.exists()) return counts
    file.forEachLine { line ->
        val digit = line.substringBefore('|').toIntOrNull()
        if (digit != null && digit in 0..9) counts[digit]++
    }
    return counts
}
