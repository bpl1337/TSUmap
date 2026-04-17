package com.example.tsumobilkabeta.Genetic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FoodRoutePanel(
    viewModel: GeneticFoodViewModel
) {
    val phase by viewModel.phase
    val errorMsg by viewModel.errorMsg

    var selectedItems by remember { mutableStateOf(setOf<String>()) }

    when (phase) {
        GeneticPhase.SELECT -> SelectionPanel(
            viewModel = viewModel,
            selectedItems = selectedItems,
            errorMsg = errorMsg,
            onToggleItem = { item ->
                selectedItems = if (item in selectedItems) selectedItems - item
                else selectedItems + item
            },
            onStart = { viewModel.startGA(selectedItems) }
        )

        GeneticPhase.PRECOMPUTING -> {
            val done by viewModel.precompDone
            val total by viewModel.precompTotal
            ProgressPanel(
                title = "Вычисление дорог...",
                progress = done.toFloat() / total.coerceAtLeast(1),
                detail = "$done / $total пар",
                onCancel = { viewModel.reset() }
            )
        }

        GeneticPhase.RUNNING_GA -> {
            val gen by viewModel.gaGeneration
            val route by viewModel.bestRoute
            ProgressPanel(
                title = "Генетический алгоритм",
                progress = gen.toFloat() / viewModel.gaTotalGenerations,
                detail = "Поколение $gen / ${viewModel.gaTotalGenerations}",
                extra = route?.let {
                    "${"%.0f".format(it.totalDistMeters)} м, ${it.establishments.size} заведений"
                },
                onCancel = { viewModel.reset() }
            )
        }

        GeneticPhase.RESULT -> {
            val route by viewModel.bestRoute
            ResultPanel(
                route = route,
                selectedItems = selectedItems,
                onReset = {
                    viewModel.reset()
                    selectedItems = emptySet()
                }
            )
        }
    }
}

@Composable
private fun SelectionPanel(
    viewModel: GeneticFoodViewModel,
    selectedItems: Set<String>,
    errorMsg: String?,
    onToggleItem: (String) -> Unit,
    onStart: () -> Unit
) {
    val startNode by viewModel.startNode

    val categoryGroups = remember {
        FoodDatabase.allEstablishments
            .flatMap { est -> est.menu.map { it to est } }
            .groupBy { it.first.category }
            .mapValues { e -> e.value.map { it.first.name }.distinct().sorted() }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { viewModel.requestStartPoint() },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (startNode != null) Color(0xFF16A34A) else Color(0xFFEA580C),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(
                if (startNode != null) "Точка установлена" else "Поставить точку",
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(6.dp))
        Text("Выберите блюда:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))

        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)) {
            for ((category, items) in categoryGroups) {
                item {
                    Text(
                        FoodDatabase.categoryDisplayName(category),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(items) { itemName ->
                    val selected = itemName in selectedItems
                    val sources = FoodDatabase.findEstablishmentsForItem(itemName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                            .background(
                                if (selected) Color(0xFFEFF6FF) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) Color(0xFF2563EB) else Color(0xFFE5E7EB),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onToggleItem(itemName) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onToggleItem(itemName) },
                            modifier = Modifier.size(18.dp),
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB))
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(itemName, fontSize = 13.sp)
                            Text(
                                sources.joinToString(", ") { it.name },
                                fontSize = 9.sp, color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (errorMsg != null) {
            Text(
                errorMsg, color = Color.Red, fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = selectedItems.isNotEmpty() && startNode != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB), contentColor = Color.White
            )
        ) {
            Text("Построить маршрут (${selectedItems.size})", fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProgressPanel(
    title: String,
    progress: Float,
    detail: String,
    extra: String? = null,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(4.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2563EB))
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = Color(0xFF2563EB),
            trackColor = Color(0xFFE5E7EB)
        )
        Spacer(Modifier.height(4.dp))
        Text(detail, fontSize = 11.sp, color = Color.Gray)
        if (extra != null) {
            Text(
                extra, fontSize = 12.sp, color = Color(0xFF2563EB),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            shape = RoundedCornerShape(8.dp)
        ) { Text("Отмена", fontSize = 12.sp) }
    }
}


@Composable
private fun ResultPanel(
    route: Route?,
    selectedItems: Set<String>,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (route == null) {
            Text("Маршрут не найден", color = Color.Red, fontWeight = FontWeight.Bold)
        } else {
            Text(
                "Маршрут построен!", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = Color(0xFF16A34A)
            )
            Spacer(Modifier.height(6.dp))

            Text(
                "${"%.0f".format(route.totalDistMeters)} м · " +
                        "${"%.0f".format(route.totalTimeMin)} мин · " +
                        "${route.establishments.size} заведений",
                fontSize = 12.sp, color = Color(0xFF374151)
            )

            Spacer(Modifier.height(8.dp))

            route.establishments.forEachIndexed { idx, est ->
                val items = est.menu
                    .filter { it.name in selectedItems }
                    .joinToString(", ") { it.name }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFF2563EB), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${idx + 1}", color = Color.White, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(est.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (items.isNotEmpty())
                            Text(items, fontSize = 11.sp, color = Color(0xFF374151))
                        Text(
                            "${est.openHour}:${"%02d".format(est.openMinute)} — " +
                                    "${est.closeHour}:${"%02d".format(est.closeMinute)}",
                            fontSize = 10.sp, color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6B7280), contentColor = Color.White
            )
        ) { Text("Заново", fontSize = 12.sp) }
    }
}
