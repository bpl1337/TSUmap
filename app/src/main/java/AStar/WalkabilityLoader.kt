package AStar

import android.content.Context

object WalkabilityCsvLoader {
    fun load(context: Context, assetName: String = "walkability.csv"): WalkabilityGrid {
        val rows = context.assets.open(assetName).bufferedReader().useLines { sequence ->
            sequence.drop(1).mapNotNull { line ->
                val p = line.split(',')
                if (p.size < 3) return@mapNotNull null
                val xCoordinate = p[0].toDoubleOrNull() ?: return@mapNotNull null
                val yCoordinate = p[1].toDoubleOrNull() ?: return@mapNotNull null
                val walkability = p[2].trim().toIntOrNull() ?: return@mapNotNull null
                Triple(xCoordinate, yCoordinate, walkability)
            }.toList()
        }

        if (rows.isEmpty()) {
            return WalkabilityGrid(width = 0, height = 0, minX = 0.0, minY = 0.0, walkable = arrayOf())
        }

        val uniqueX = rows.map { it.first }.distinct().sorted()
        val uniqueY = rows.map { it.second }.distinct().sorted()

        val minX = uniqueX.first()
        val minY = uniqueY.first()
        val width = uniqueX.size
        val height = uniqueY.size
        val stepX = inferStep(uniqueX)
        val stepY = inferStep(uniqueY)
        val step = when {
            stepX > 0.0 && stepY > 0.0 -> minOf(stepX, stepY)
            stepX > 0.0 -> stepX
            stepY > 0.0 -> stepY
            else -> kStep
        }

        val grid = Array(height) { BooleanArray(width) { false } }

        val xToIndex = uniqueX.withIndex().associate { it.value to it.index }
        val yToIndex = uniqueY.withIndex().associate { it.value to it.index }

        for ((x, y, w) in rows) {
            val gridX = xToIndex[x] ?: continue
            val gridY = yToIndex[y] ?: continue

            grid[gridY][gridX] = (w == 1)
        }

        return WalkabilityGrid(
            width = width,
            height = height,
            minX = minX,
            minY = minY,
            step = step,
            walkable = grid,
            uniqueX = uniqueX,
            uniqueY = uniqueY
        )
    }

    private fun inferStep(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        var minPositiveDelta = Double.MAX_VALUE

        for (i in 1 until values.size) {
            val delta = values[i] - values[i - 1]
            if (delta > 0.0 && delta < minPositiveDelta) {
                minPositiveDelta = delta
            }
        }

        return if (minPositiveDelta == Double.MAX_VALUE) 0.0 else minPositiveDelta
    }
}