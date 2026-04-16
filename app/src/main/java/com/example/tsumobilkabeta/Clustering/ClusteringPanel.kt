package com.example.tsumobilkabeta.Clustering

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ClusteringPanel(viewModel: ClusteringViewModel?) {
    if (viewModel == null) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Кластеризация")
        Text("Количество кластеров: ${viewModel.clusterCount}")

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

        OutlinedButton(
            onClick = { viewModel.resetClustering() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сброс")
        }
    }
}
