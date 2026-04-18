package com.example.tsumobilkabeta.AI

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.tsumobilkabeta.Genetic.FoodDatabase
import com.example.tsumobilkabeta.Genetic.FoodEstablishment
import com.example.tsumobilkabeta.processDrawing2px
import com.example.tsumobilkabeta.ui.theme.TSUMapTheme
import java.io.File
import kotlin.math.roundToInt

private const val ADMIN_MODE = false

class AIMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val isDark = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_dark_theme", false)

        setContent {
            TSUMapTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    RatingApp()
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
fun RatingApp() {
    val context = LocalContext.current

    val establishments = remember { FoodDatabase.allEstablishments }
    var currentPath by remember { mutableStateOf(Path()) }
    var adminStatus by remember { mutableStateOf("") }
    var countsPerDigit by remember { mutableStateOf(countSamplesPerDigit(context)) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                Text("Заведения", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
            }

            items(establishments) { establishment ->
                EstablishmentCard(
                    establishment = establishment,
                    onClick = {
                        context.startActivity(
                            EstablishmentRatingActivity.createIntent(context, establishment.id)
                        )
                    }
                )
            }

            if (ADMIN_MODE) {
                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    val totalSaved = countsPerDigit.sum()
                    Text(
                        "Панель датасета",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Всего: $totalSaved образцов", fontSize = 11.sp, color = Color.Gray)

                    if (adminStatus.isNotEmpty()) {
                        Text(adminStatus, fontSize = 12.sp, color = Color(0xFF1565C0))
                    }

                    Spacer(Modifier.height(8.dp))
                }

                item {
                    val digits = (0..9).toList()
                    Column {
                        listOf(digits.take(5), digits.drop(5)).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { digit ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Button(
                                            onClick = {
                                                if (!currentPath.isEmpty) {
                                                    val pixels = processDrawing2px(currentPath)
                                                    saveSample(context, digit, pixels)
                                                    countsPerDigit =
                                                        countsPerDigit.copyOf().also { it[digit]++ }
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
                }

                item {
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
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                    ) {
                        Text("Удалить последнюю запись", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EstablishmentCard(
    establishment: FoodEstablishment,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(establishment.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val avg = (establishment.averageRating.floatValue * 10f).roundToInt() / 10f
            Text(
                text = "Рейтинг: $avg (${establishment.ratingsCount()} оценок)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = establishment.menu.take(3).joinToString(", ") { it.name },
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = "Открыто: ${
                    establishment.openHour.toString().padStart(2, '0')
                }:${
                    establishment.openMinute.toString().padStart(2, '0')
                } - ${
                    establishment.closeHour.toString().padStart(2, '0')
                }:${establishment.closeMinute.toString().padStart(2, '0')}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


private fun saveSample(context: android.content.Context, label: Int, pixels: FloatArray) {
    File(context.filesDir, "dataset_2px.txt").appendText("$label|${pixels.joinToString(",")}\n")
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
