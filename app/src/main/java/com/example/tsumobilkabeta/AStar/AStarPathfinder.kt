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

    fun findPathWithSteps(
        grid: WalkabilityGrid,
        start: GridNode,
        end: GridNode
    ): Pair<List<GridNode>, List<SearchStep>> {
        if (!grid.isWalkable(start) || !grid.isWalkable(end)) return Pair(emptyList(), emptyList())
        if (start == end) return Pair(listOf(start), emptyList())

        val steps = mutableListOf<SearchStep>()
        val openList = PriorityQueue(compareBy<OpenNode> { it.fScore })
        val cameFrom = mutableMapOf<GridNode, GridNode>()
        val gScore = mutableMapOf(start to 0)
        val closedSet = mutableSetOf<GridNode>()
        val openSet = mutableSetOf(start)

        openList.add(OpenNode(start, heuristic(start, end)))

        while (openList.isNotEmpty()) {
            val current = openList.poll()?.node ?: continue
            if (!closedSet.add(current)) continue
            openSet.remove(current)

            if (current == end) {
                val path = reconstructPath(cameFrom, current)
                steps.add(SearchStep(current, openSet.toSet(), closedSet.toSet(), path))
                return Pair(path, steps)
            }

            steps.add(SearchStep(current, openSet.toSet(), closedSet.toSet(), emptyList()))

            val baseScore = gScore[current] ?: continue
            for (neighbor in grid.neighbors(current)) {
                if (neighbor in closedSet) continue
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
                    openSet.add(neighbor)
                }
            }
        }

        steps.add(SearchStep(start, emptySet(), closedSet.toSet(), emptyList()))
        return Pair(emptyList(), steps)
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

