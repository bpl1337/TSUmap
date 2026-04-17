package com.example.tsumobilkabeta.Ant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Amber: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val AmberDark: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
private val AmberLight: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val AmberBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
private val OrangeDark: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary
private val Green: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val GrayBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val GrayText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val DarkText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

@Composable
fun AntRoutePanel(viewModel: AntViewModel) {
    val mode by viewModel.mode
    val phase by viewModel.phase

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = if (mode == AntMode.TSP) 0 else 1,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = AmberDark,
            indicator = {},
            divider = {}
        ) {
            Tab(
                selected = mode == AntMode.TSP,
                onClick = { viewModel.setMode(AntMode.TSP) },
                modifier = Modifier
                    .background(
                        if (mode == AntMode.TSP) Amber else Color.Transparent,
                        RoundedCornerShape(topStart = 8.dp)
                    )
                    .height(36.dp)
            ) {
                Text(
                    "Маршрут",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (mode == AntMode.TSP) MaterialTheme.colorScheme.onPrimary else GrayText
                )
            }
            Tab(
                selected = mode == AntMode.COWORKING,
                onClick = { viewModel.setMode(AntMode.COWORKING) },
                modifier = Modifier
                    .background(
                        if (mode == AntMode.COWORKING) Amber else Color.Transparent,
                        RoundedCornerShape(topEnd = 8.dp)
                    )
                    .height(36.dp)
            ) {
                Text(
                    "Коворкинг",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (mode == AntMode.COWORKING) MaterialTheme.colorScheme.onPrimary else GrayText
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        when (mode) {
            AntMode.TSP -> when (phase) {
                AntPhase.SELECT -> TspSelectPanel(viewModel)
                AntPhase.COMPUTING -> ComputingPanel(onCancel = { viewModel.resetResult() })
                AntPhase.RESULT -> TspResultPanel(viewModel)
            }

            AntMode.COWORKING -> when (phase) {
                AntPhase.SELECT -> CoworkingSelectPanel(viewModel)
                AntPhase.COMPUTING -> ComputingPanel(onCancel = { viewModel.resetResult() })
                AntPhase.RESULT -> CoworkingResultPanel(viewModel)
            }
        }
    }
}

@Composable
private fun TspSelectPanel(vm: AntViewModel) {
    val startPt by vm.startPoint
    val selected by vm.selectedPlaceIds
    val returnHome by vm.returnToStart
    val error by vm.errorMsg
    val places = remember { vm.getAllPlaces() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { vm.requestStartPoint() },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (startPt != null) Green else OrangeDark,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(
                if (startPt != null) "Точка установлена" else "Поставить точку",
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Достопримечательности:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DarkText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { vm.selectAllPlaces() },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Все", fontSize = 11.sp, color = Amber)
                }
                TextButton(
                    onClick = { vm.deselectAllPlaces() },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Сброс", fontSize = 11.sp, color = GrayText)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(places, key = { it.id }) { place ->
                val isSelected = place.id in selected
                val catColor = placeCategory(place.name)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) AmberBg else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Amber else GrayBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { vm.togglePlace(place.id) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(catColor, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        place.name,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        color = DarkText
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { vm.togglePlace(place.id) },
                        modifier = Modifier.size(20.dp),
                        colors = CheckboxDefaults.colors(checkedColor = Amber)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Вернуться к началу", fontSize = 12.sp, color = DarkText)
            Switch(
                checked = returnHome,
                onCheckedChange = { vm.setReturnToStart(it) },
                modifier = Modifier.height(28.dp),
                colors = SwitchDefaults.colors(checkedTrackColor = Amber)
            )
        }

        Spacer(Modifier.height(6.dp))

        if (error != null) {
            Text(
                error!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Button(
            onClick = { vm.buildTspRoute() },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = selected.isNotEmpty() && startPt != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Amber, contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Построить маршрут (${selected.size})", fontSize = 13.sp)
        }
    }
}

@Composable
private fun CoworkingSelectPanel(vm: AntViewModel) {
    val startPt by vm.startPoint
    val students by vm.studentCount
    val selected by vm.selectedCoworkingIds
    val error by vm.errorMsg
    val selectedCapacity = remember(selected) {
        CoworkingDatabase.locations.filter { it.id in selected }.sumOf { it.capacity }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { vm.requestStartPoint() },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (startPt != null) Green else OrangeDark,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(
                if (startPt != null) "Точка группы установлена" else "Поставить точку группы",
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Количество студентов:",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = DarkText
        )
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    RoundedCornerShape(8.dp)
                )
                .border(1.dp, GrayBorder, RoundedCornerShape(8.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            FilledIconButton(
                onClick = { vm.setStudentCount(students - 10) },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = AmberLight)
            ) { Text("-10", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberDark) }

            Spacer(Modifier.width(4.dp))

            FilledIconButton(
                onClick = { vm.setStudentCount(students - 1) },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = AmberLight)
            ) { Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberDark) }

            Text(
                "$students",
                modifier = Modifier.widthIn(min = 48.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DarkText
            )

            FilledIconButton(
                onClick = { vm.setStudentCount(students + 1) },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = AmberLight)
            ) { Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberDark) }

            Spacer(Modifier.width(4.dp))

            FilledIconButton(
                onClick = { vm.setStudentCount(students + 10) },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = AmberLight)
            ) { Text("+10", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberDark) }
        }

        if (selected.isNotEmpty() && students > selectedCapacity) {
            Spacer(Modifier.height(3.dp))
            Text(
                "Мест может не хватить (всего $selectedCapacity)",
                fontSize = 11.sp,
                color = OrangeDark
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Локации для поиска:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DarkText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { vm.selectAllCoworkingLocations() },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) { Text("Все", fontSize = 11.sp, color = Amber) }
                TextButton(
                    onClick = { vm.deselectAllCoworkingLocations() },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) { Text("Сброс", fontSize = 11.sp, color = GrayText) }
            }
        }

        Spacer(Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(CoworkingDatabase.locations, key = { it.id }) { loc ->
                val isSelected = loc.id in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) AmberBg else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Amber else GrayBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { vm.toggleCoworkingLocation(loc.id) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(loc.name, fontSize = 12.sp, color = DarkText)
                        Text(
                            "Вместимость: ${loc.capacity}  |  Комфорт: ${"%.1f".format(loc.comfort)}",
                            fontSize = 10.sp,
                            color = GrayText
                        )
                    }
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { vm.toggleCoworkingLocation(loc.id) },
                        modifier = Modifier.size(20.dp),
                        colors = CheckboxDefaults.colors(checkedColor = Amber)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (error != null) {
            Text(
                error!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Button(
            onClick = { vm.buildCoworkingRoute() },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = startPt != null && students > 0 && selected.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Amber, contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Найти места (${selected.size})", fontSize = 13.sp)
        }
    }
}

@Composable
private fun ComputingPanel(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Amber,
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Муравьи исследуют маршруты...",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = AmberDark
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Оптимизация колонии",
            fontSize = 11.sp,
            color = GrayText
        )
        Spacer(Modifier.height(12.dp))
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
private fun TspResultPanel(vm: AntViewModel) {
    val places by vm.resultPlaces
    val dist by vm.resultDistance

    Column(modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())) {
        Text(
            "Маршрут построен!",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Green
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${"%.0f".format(dist)} м  |  ${places.size} точек",
            fontSize = 12.sp,
            color = DarkText
        )
        Spacer(Modifier.height(8.dp))

        places.forEachIndexed { idx, place ->
            val color = Color(AntViewModel.LEG_COLORS[idx % AntViewModel.LEG_COLORS.size])
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(6.dp)
                    )
                    .border(1.dp, GrayBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${idx + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(place.name, fontSize = 13.sp, color = DarkText)
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.resetResult() },
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

@Composable
private fun CoworkingResultPanel(vm: AntViewModel) {
    val cwResult by vm.coworkingResult
    val result = cwResult ?: return

    val occupied = result.assignments.filter { it.studentsAssigned > 0 }

    Column(modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())) {
        Text(
            "Места распределены!",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Green
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Ср. комфорт: ${"%.1f".format(result.avgComfort)}  |  ${occupied.size} локаций",
            fontSize = 12.sp,
            color = DarkText
        )
        Spacer(Modifier.height(8.dp))

        result.assignments.forEachIndexed { idx, a ->
            if (a.studentsAssigned > 0) {
                val color = Color(AntViewModel.LEG_COLORS[idx % AntViewModel.LEG_COLORS.size])
                val fillRatio = a.studentsAssigned.toFloat() / a.location.capacity.coerceAtLeast(1)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, GrayBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            a.location.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = DarkText,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${a.studentsAssigned}/${a.location.capacity}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (a.studentsAssigned > a.location.capacity) MaterialTheme.colorScheme.error else color
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { fillRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (fillRatio > 1f) MaterialTheme.colorScheme.error else color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Комфорт: ${"%.1f".format(a.location.comfort)}",
                            fontSize = 10.sp,
                            color = GrayText
                        )
                        Text(
                            "${"%.0f".format(a.distance)} м",
                            fontSize = 10.sp,
                            color = GrayText
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.resetResult() },
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

private fun placeCategory(name: String): Color = when {
    "родник" in name.lowercase() || "Родник" in name -> Color(0xFF3B82F6)
    "памятник" in name.lowercase() || "Памятник" in name -> Color(0xFF9CA3AF)
    "сад" in name.lowercase() || "роща" in name.lowercase() || "озеро" in name.lowercase()
        -> Color(0xFF22C55E)

    "корпус" in name.lowercase() || "центр" in name.lowercase()
        -> Color(0xFFF59E0B)

    else -> Color(0xFF8B5CF6)
}
