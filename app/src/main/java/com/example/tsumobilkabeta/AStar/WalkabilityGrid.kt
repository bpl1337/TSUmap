package AStar

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
    fun inBounds(node: GridNode): Boolean =
        node.x in 0 until width && node.y in 0 until height

    fun isWalkable(node: GridNode): Boolean =
        inBounds(node) && walkable[node.y][node.x]

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
                    if (kotlin.math.max(kotlin.math.abs(dx), kotlin.math.abs(dy)) != radius) continue
                    val candidate = GridNode(node.x + dx, node.y + dy)
                    if (isWalkable(candidate)) return candidate
                }
            }
        }

        return null
    }

    fun hasLineOfSight(from: GridNode, to: GridNode): Boolean {
        if (!isWalkable(from) || !isWalkable(to)) return false

        var x = from.x
        var y = from.y
        val dx = kotlin.math.abs(to.x - from.x)
        val dy = kotlin.math.abs(to.y - from.y)
        val sx = if (from.x < to.x) 1 else -1
        val sy = if (from.y < to.y) 1 else -1
        var err = dx - dy

        while (x != to.x || y != to.y) {
            val e2 = err * 2
            val prevX = x
            val prevY = y

            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }

            val current = GridNode(x, y)
            if (!isWalkable(current)) return false

            val movedDiagonally = (x != prevX) && (y != prevY)
            if (movedDiagonally) {
                val sideA = GridNode(prevX, y)
                val sideB = GridNode(x, prevY)
                if (!isWalkable(sideA) && !isWalkable(sideB)) return false
            }
        }

        return true
    }
}