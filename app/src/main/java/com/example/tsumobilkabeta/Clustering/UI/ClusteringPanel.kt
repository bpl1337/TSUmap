package com.example.tsumobilkabeta.Clustering.UI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tsumobilkabeta.Clustering.model.ClusterComparisonItem
import com.example.tsumobilkabeta.Clustering.model.ClusteringMode

@Composable
fun ClusteringPanel(viewModel: ClusteringViewModel?) {
    if (viewModel == null) return

    val changedItems = viewModel.comparisonItems.filter { it.changed }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Кластеризация",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text("Количество кластеров: ${viewModel.clusterCount}")
        Text("Режим: ${modeLabel(viewModel.mode)}")

        Button(
            onClick = { viewModel.increaseClusterCount() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("K +")
        }

        Button(
            onClick = { viewModel.decreaseClusterCount() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("K -")
        }

        Button(
            onClick = { viewModel.runClustering() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Запустить")
        }

        Button(
            onClick = { viewModel.changeMode(ClusteringMode.EUCLIDEAN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("По прямой")
        }

        Button(
            onClick = { viewModel.changeMode(ClusteringMode.WALKING) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("По дорожкам")
        }

        Button(
            onClick = { viewModel.changeMode(ClusteringMode.COMPARISON) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сравнение")
        }

        OutlinedButton(
            onClick = { viewModel.resetClustering() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сброс")
        }

        if (viewModel.mode == ClusteringMode.COMPARISON) {
            ChangedPointsCard(changedItems)
        }
    }
}

@Composable
fun ChangedPointsCard(items: List<ClusterComparisonItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Изменившиеся точки: ${items.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (items.isEmpty()) {
                Text(
                    text = "Все заведения остались в тех же кластерах.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                items.forEach { item ->
                    ChangedPointRow(item)
                }
            }
        }
    }
}

@Composable
fun ChangedPointRow(item: ClusterComparisonItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFE53935), RoundedCornerShape(50))
                )
                Text(
                    text = item.establishment.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClusterBadge(
                    title = "По прямой",
                    clusterIndex = item.euclideanCluster
                )
                ClusterBadge(
                    title = "По дорожкам",
                    clusterIndex = item.walkingCluster
                )
            }
        }
    }
}

@Composable
fun ClusterBadge(
    title: String,
    clusterIndex: Int
) {
    val clusterColor = clusterColor(clusterIndex)

    Column(
        modifier = Modifier
            .background(
                color = clusterColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Кластер ${clusterIndex + 1}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = clusterColor
        )
    }
}

fun clusterColor(index: Int): Color {
    val colors = listOf(
        Color(0xFFE53935),
        Color(0xFF1E88E5),
        Color(0xFF43A047),
        Color(0xFFFB8C00),
        Color(0xFF8E24AA),
        Color(0xFF00897B)
    )
    return colors[index.mod(colors.size)]
}

fun modeLabel(mode: ClusteringMode): String = when (mode) {
    ClusteringMode.EUCLIDEAN -> "По прямой"
    ClusteringMode.WALKING -> "По дорожкам"
    ClusteringMode.COMPARISON -> "Сравнение"
}
