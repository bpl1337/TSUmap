package com.example.tsumobilkabeta.Genetic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                containerColor = if (startNode != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            for ((category, items) in categoryGroups) {
                item {
                    Text(
                        FoodDatabase.categoryDisplayName(category),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
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
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(itemName, fontSize = 13.sp)
                            Text(
                                sources.joinToString(", ") { it.name },
                                fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (errorMsg != null) {
            Text(
                errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp,
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (extra != null) {
            Text(
                extra, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
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
            Text(
                "Маршрут не найден",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                "Маршрут построен!", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))

            Text(
                "${"%.0f".format(route.totalDistMeters)} м · " +
                        "${"%.0f".format(route.totalTimeMin)} мин · " +
                        "${route.establishments.size} заведений",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface
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
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(11.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${idx + 1}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(est.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (items.isNotEmpty())
                            Text(
                                items,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        Text(
                            "${est.openHour}:${"%02d".format(est.openMinute)} — " +
                                    "${est.closeHour}:${"%02d".format(est.closeMinute)}",
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
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
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) { Text("Заново", fontSize = 12.sp) }
    }
}
