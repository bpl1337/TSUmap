package com.example.tsumobilkabeta.AStar

import kotlin.math.abs
import kotlin.math.max

const val kStep = 5.0

class GridNode(val x: Int, val y: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GridNode) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        return x * 31 + y
    }

    override fun toString(): String = "GridNode($x, $y)"
}

class WalkabilityGrid(
    val width: Int,
    val height: Int,
    val minX: Double,
    val minY: Double,
    val step: Double = kStep,
    val walkable: Array<BooleanArray>,
    val uniqueX: List<Double> = emptyList(),
    val uniqueY: List<Double> = emptyList()
) {
    private val dynamicBarriers = mutableSetOf<GridNode>()

    fun addBarrier(node: GridNode) { dynamicBarriers.add(node) }
    fun removeBarrier(node: GridNode) { dynamicBarriers.remove(node) }
    fun clearBarriers() { dynamicBarriers.clear() }
    fun isBarrier(node: GridNode): Boolean = node in dynamicBarriers
    fun barrierSnapshot(): Set<GridNode> = dynamicBarriers.toSet()

    fun inBounds(node: GridNode): Boolean =
        node.x in 0 until width && node.y in 0 until height

    fun isWalkable(node: GridNode): Boolean =
        inBounds(node) && walkable[node.y][node.x] && !dynamicBarriers.contains(node)

    fun neighbors(node: GridNode): List<GridNode> {
        val result = mutableListOf<GridNode>()

        val orthogonal = listOf(
            GridNode(node.x, node.y - 1),
            GridNode(node.x - 1, node.y),
            GridNode(node.x + 1, node.y),
            GridNode(node.x, node.y + 1)
        )

        for (next in orthogonal) {
            if (isWalkable(next)) result.add(next)
        }

        val diagonals = listOf(
            GridNode(node.x - 1, node.y - 1),
            GridNode(node.x + 1, node.y - 1),
            GridNode(node.x - 1, node.y + 1),
            GridNode(node.x + 1, node.y + 1)
        )

        for (next in diagonals) {
            if (!isWalkable(next)) continue

            val dx = next.x - node.x
            val dy = next.y - node.y
            val sideA = GridNode(node.x + dx, node.y)
            val sideB = GridNode(node.x, node.y + dy)

            if (isWalkable(sideA) || isWalkable(sideB)) {
                result.add(next)
            }
        }

        return result
    }

    fun nearestWalkable(node: GridNode, maxRadius: Int = 20): GridNode? {
        if (isWalkable(node)) return node

        for (radius in 1..maxRadius) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (max(abs(dx), abs(dy)) != radius) continue
                    val candidate = GridNode(node.x + dx, node.y + dy)
                    if (isWalkable(candidate)) return candidate
                }
            }
        }

        return null
    }

}