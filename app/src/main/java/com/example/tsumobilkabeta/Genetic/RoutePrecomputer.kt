package com.example.tsumobilkabeta.Genetic

import com.example.tsumobilkabeta.AStar.AStarPathfinder
import com.example.tsumobilkabeta.AStar.GridNode
import com.example.tsumobilkabeta.AStar.GridProjection
import com.example.tsumobilkabeta.AStar.WalkabilityGrid

data class PrecomputedData(
    val establishments: List<FoodEstablishment>,
    val cells: List<GridNode>,
    val distMatrix: Array<DoubleArray>,
    val pathMatrix: Array<Array<List<GridNode>?>>
)

object RoutePrecomputer {

    fun compute(
        grid: WalkabilityGrid,
        startNode: GridNode,
        candidates: List<FoodEstablishment>,
        onProgress: ((Int, Int) -> Unit)? = null
    ): PrecomputedData? {

        val validEsts = mutableListOf<FoodEstablishment>()
        val estNodes = mutableListOf<GridNode>()

        for (est in candidates) {
            val raw = GridProjection.pointToNode(est.location, grid)
            val wk = grid.nearestWalkable(raw) ?: continue
            validEsts.add(est)
            estNodes.add(wk)
        }
        if (validEsts.isEmpty()) return null

        val cells: List<GridNode> = listOf(startNode) + estNodes
        val n = cells.size
        val distMatrix = Array(n) { DoubleArray(n) { Double.MAX_VALUE / 2 } }
        val pathMatrix: Array<Array<List<GridNode>?>> = Array(n) { arrayOfNulls(n) }

        val total = n * (n - 1) / 2
        var done = 0

        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val path = AStarPathfinder.findPath(grid, cells[i], cells[j])
                val dist = if (path != null) AStarPathfinder.pathDistanceMeters(path)
                else Double.MAX_VALUE / 2

                pathMatrix[i][j] = path
                pathMatrix[j][i] = path?.reversed()
                distMatrix[i][j] = dist
                distMatrix[j][i] = dist

                done++
                onProgress?.invoke(done, total)
            }
        }

        return PrecomputedData(validEsts, cells, distMatrix, pathMatrix)
    }
}
