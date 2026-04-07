package com.example.tsumobilkabeta.AStar

import java.util.PriorityQueue
import kotlin.math.abs

object AStarPathfinder {
    private const val ORTHOGONAL_COST = 10
    private const val DIAGONAL_COST = 14

    private data class OpenNode(
        val node: GridNode,
        val fScore: Int
    )

    fun findPath(
        grid: WalkabilityGrid,
        start: GridNode,
        end: GridNode
    ): List<GridNode> {
        if (!grid.isWalkable(start) || !grid.isWalkable(end)) return emptyList()

        val openList = PriorityQueue(compareBy<OpenNode> { it.fScore })
        val cameFrom = mutableMapOf<GridNode, GridNode>()
        val gScore = mutableMapOf(start to 0)

        openList.add(OpenNode(start, heuristic(start, end)))

        while (openList.isNotEmpty()) {
            val current = openList.poll()?.node ?: continue
            if (current == end) {
                val rawPath = reconstructPath(cameFrom, current)
                return rawPath
            }

            val baseScore = gScore[current] ?: continue

            for (neighbor in grid.neighbors(current)) {
                val stepCost = if (neighbor.x != current.x && neighbor.y != current.y) {
                    DIAGONAL_COST
                } else {
                    ORTHOGONAL_COST
                }

                val tentative = baseScore + stepCost
                if (tentative < gScore.getOrDefault(neighbor, Int.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentative
                    val fScore = tentative + heuristic(neighbor, end)
                    openList.add(OpenNode(neighbor, fScore))
                }
            }
        }

        return emptyList()
    }

    private fun heuristic(a: GridNode, b: GridNode): Int {
        val dx = abs(a.x - b.x)
        val dy = abs(a.y - b.y)
        val minD = minOf(dx, dy)
        val maxD = maxOf(dx, dy)
        return DIAGONAL_COST * minD + ORTHOGONAL_COST * (maxD - minD)
    }

    private fun reconstructPath(
        cameFrom: Map<GridNode, GridNode>,
        end: GridNode
    ): List<GridNode> {
        val path = mutableListOf(end)
        var current = end

        while (true) {
            val prev = cameFrom[current] ?: break
            path.add(prev)
            current = prev
        }

        path.reverse()
        return path
    }

}

