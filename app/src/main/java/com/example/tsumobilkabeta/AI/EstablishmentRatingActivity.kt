package com.example.tsumobilkabeta.AI

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.tsumobilkabeta.Clustering.model.EstablishmentRatingStore
import com.example.tsumobilkabeta.Genetic.FoodDatabase
import com.example.tsumobilkabeta.Genetic.FoodEstablishment
import com.example.tsumobilkabeta.floatArrayToBitmap
import com.example.tsumobilkabeta.processDrawing2px
import com.example.tsumobilkabeta.ui.theme.TSUMapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EstablishmentRatingActivity : ComponentActivity() {
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

        val establishmentId = intent.getIntExtra(EXTRA_ESTABLISHMENT_ID, -1)
        val establishment = FoodDatabase.allEstablishments.firstOrNull { it.id == establishmentId }
            ?: run {
                Toast.makeText(this, "Заведение не найдено", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        val isDark = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_dark_theme", false)

        setContent {
            TSUMapTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EstablishmentRatingScreen(
                        establishment = establishment,
                        classifier = classifier,
                        initialRating = establishment.averageRating.floatValue.toInt(),
                        onSave = { rating ->
                            EstablishmentRatingStore.saveRating(this, establishment.id, rating)
                            establishment.addRating(rating)
                            Toast.makeText(this, "Оценка сохранена", Toast.LENGTH_SHORT).show()
                            finish()
                        },

                        onCancel = { finish() }
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

    companion object {
        private const val EXTRA_ESTABLISHMENT_ID = "extra_establishment_id"

        fun createIntent(context: Context, establishmentId: Int): Intent =
            Intent(context, EstablishmentRatingActivity::class.java)
                .putExtra(EXTRA_ESTABLISHMENT_ID, establishmentId)
    }
}

@Composable
private fun EstablishmentRatingScreen(
    establishment: FoodEstablishment,
    classifier: NnClassifier,
    initialRating: Int,
    onSave: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var currentPath by remember { mutableStateOf(Path()) }
    var currentRating by remember { mutableIntStateOf(initialRating.coerceIn(0, 9)) }
    var hasRecognizedDigit by remember { mutableStateOf(false) }
    var confidence by remember { mutableFloatStateOf(0f) }
    var debugBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val scrollState = rememberScrollState()

    val canvasSize = 300.dp
    val canvasSizePx = with(LocalDensity.current) { canvasSize.toPx() }

    LaunchedEffect(currentPath) {
        if (currentPath.isEmpty) {
            currentRating = initialRating.coerceIn(0, 9)
            hasRecognizedDigit = false
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

        if (result.first in 0..9) {
            currentRating = result.first
            hasRecognizedDigit = true
        } else {
            hasRecognizedDigit = false
        }
        confidence = result.second
        debugBitmap = result.third
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Оценка заведения",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = establishment.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Нарисуйте цифру от 0 до 9",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hasRecognizedDigit) "Распознано: $currentRating" else "Цифра ещё не распознана",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .size(canvasSize)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                if (down.position.x !in 0f..canvasSizePx || down.position.y !in 0f..canvasSizePx) {
                                    return@awaitEachGesture
                                }

                                currentPath.moveTo(down.position.x, down.position.y)

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.first()
                                    if (!change.pressed) break

                                    val pos = change.position
                                    if (pos.x in 0f..canvasSizePx && pos.y in 0f..canvasSizePx) {
                                        currentPath.lineTo(pos.x, pos.y)
                                        change.consume()
                                        currentPath = Path().apply { addPath(currentPath) }
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPath(
                            path = currentPath,
                            color = Color.Black,
                            style = Stroke(
                                width = 30f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (confidence > 0f) "Уверенность: ${(confidence * 100).toInt()}%" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (debugBitmap != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Так видит нейронка (50x50):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.foundation.Image(
                        bitmap = debugBitmap!!,
                        contentDescription = "Debug view",
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White)
                            .border(1.dp, Color.LightGray)
                    )
                }

            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { if (hasRecognizedDigit && currentRating in 0..9) onSave(currentRating) },
            enabled = hasRecognizedDigit && currentRating in 0..9,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Сохранить")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                currentPath = Path()
                currentRating = initialRating.coerceIn(0, 9)
                hasRecognizedDigit = false
                confidence = 0f
                debugBitmap = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Очистить холст")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Отмена")
        }
    }
}


