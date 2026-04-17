package com.example.tsumobilkabeta.Ant

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import java.io.File

data class Result(
    val allPlaces: List<Places>,
    val order: IntArray,
    val gridPath: List<GridPoint>,
    val test: List<Pair<Double, Double>>,
    val dist: Double
)

data class Places(
    val x: Double,
    val y: Double,
    val id: Int,
    val name: String
)

data class GridPoint(
    val col: Int,
    val row: Int
)

data class Tour(
    val vertex: IntArray,
    val dist: Double
)

data class Path(
    val points: List<GridPoint>,
    val dist: Double
)

data class MapPlace(
    val plase: Places,
    val truePoint: GridPoint,
    val snapPoint: GridPoint
)

class GridMap(
    val minX: Double,
    val minY: Double,
    val stepX: Double,
    val stepY: Double,
    val passable: Map<GridPoint, Boolean>
) {
    val directions = listOf(
        GridPoint(1, 0),
        GridPoint(0, -1),
        GridPoint(0, 1),
        GridPoint(-1, 0)
    )

    fun QGisToGrid(x: Double, y: Double): GridPoint {
        val col = ((x - minX) / stepX).roundToInt()
        val row = ((y - minY) / stepY).roundToInt()
        return GridPoint(col, row)
    }

    fun gridToQgis(point: GridPoint): Pair<Double, Double> {
        val x = minX + point.col * stepX
        val y = minY + point.row * stepY
        return x to y
    }

    fun isPassable(point: GridPoint): Boolean {
        return passable[point] == true
    }

    fun contains(point: GridPoint): Boolean {
        return passable.containsKey(point)
    }

    fun findNeighbors(point: GridPoint): List<GridPoint> {
        return directions.map { GridPoint(point.col + it.col, point.row + it.row) }
            .filter { contains(it) && isPassable(it) }
    }

    fun snapToNear(start: GridPoint, maxRadius: Int = 20): GridPoint? {
        if (isPassable(start)) return start

        val queue = ArrayDeque<GridPoint>()
        val visited = mutableSetOf<GridPoint>()

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val radius = abs(current.col - start.col) + abs(current.row - start.row)
            if (radius > maxRadius) continue

            if (contains(current) && isPassable(current)) {
                return current
            }

            for (direct in directions) {
                val newCurrent = GridPoint(current.col + direct.col, current.row + direct.row)
                if (newCurrent !in visited && contains(newCurrent)) {
                    visited.add(newCurrent)
                    queue.add(newCurrent)
                }
            }
        }
        return null
    }

    fun bfs(start: GridPoint, end: GridPoint): Path? {
        if (!contains(start) || !contains(end)) return null
        if (!isPassable(start) || !isPassable(end)) return null
        if (start == end) return Path(listOf(start), 0.0)

        val queue = ArrayDeque<GridPoint>()
        val visited = mutableSetOf<GridPoint>()
        val parent = mutableMapOf<GridPoint, GridPoint?>()
        queue.add(start)
        visited.add(start)
        parent[start] = null

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (current == end) {
                val points = mutableListOf<GridPoint>()
                var node: GridPoint? = end
                while (node != null) {
                    points.add(node)
                    node = parent[node]
                }
                points.reverse()

                val dist = (points.size - 1) * stepX
                return Path(points, dist)
            }

            for (newCurrent in findNeighbors(current)) {
                if (newCurrent !in visited) {
                    visited.add(newCurrent)
                    parent[newCurrent] = current
                    queue.add(newCurrent)
                }
            }
        }
        return null
    }
}


object Reader {
    fun readAsset(context: Context, fileName: String): List<String> {
        return context.assets.open(fileName).use { input ->
            BufferedReader(InputStreamReader(input)).readLines()
        }.filter { it.isNotBlank() }
    }

    fun readGridMap(context: Context, name: String): GridMap {
        val rows = readAsset(context, name).mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size < 3) return@mapNotNull null
            val x = parts[0].trim().toDoubleOrNull() ?: return@mapNotNull null
            val y = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
            val pass = parts[2].trim().toIntOrNull() ?: return@mapNotNull null
            Triple(x, y, pass)
        }
        val xs = rows.map { it.first }.distinct().sorted()
        val ys = rows.map { it.second }.distinct().sorted()
        val stepX = findStep(xs)
        val stepY = findStep(ys)
        val minX = xs.first()
        val minY = ys.first()

        val passableMap = mutableMapOf<GridPoint, Boolean>()

        for ((x, y, pass) in rows) {
            val col = ((x - minX) / stepX).roundToInt()
            val row = ((y - minY) / stepY).roundToInt()
            passableMap[GridPoint(col, row)] = (pass == 1)
        }

        return GridMap(
            minX = minX,
            minY = minY,
            stepX = stepX,
            stepY = stepY,
            passable = passableMap
        )
    }

    fun findStep(values: List<Double>): Double {
        val diffs = values.zipWithNext()
            .map { (a, b) -> b - a }
            .filter { it > 1e-9 }

        return diffs.min()
    }
}

class RouteBuilder(val gridMap: GridMap) {

    fun buildRoute(
        userPoint: GridPoint,
        places: List<MapPlace>,
        antCount: Int = 20,
        iter: Int = 150,
        alpha: Double = 1.0,
        beta: Double = 4.0,
        evaporation: Double = 0.4,
        q: Double = 100.0,
        end: Boolean = false
    ): Result {

        val points = mutableListOf<GridPoint>()
        points.add(userPoint)
        points.addAll(places.map { it.snapPoint })

        val distMatrix = DistMatrix(points)

        val optimizer = AntColony(
            dist = distMatrix,
            antCount = antCount,
            iter = iter,
            alpha = alpha,
            beta = beta,
            evaporation = evaporation,
            q = q
        )

        val bestTour = optimizer.solve(startIndex = 0, end = end)

        val fullPath = restoreFullRoute(bestTour.vertex, points)!!
        val line = fullPath.points.map { gridMap.gridToQgis(it) }

        val orderedPlaces = bestTour.vertex.filter { it != 0 }.map { places[it - 1].plase }

        return Result(
            allPlaces = orderedPlaces,
            order = bestTour.vertex,
            gridPath = fullPath.points,
            test = line,
            dist = fullPath.dist
        )

    }

    fun mapPlaces(places: List<Places>, maxRadius: Int = 20): List<MapPlace> {
        return places.mapNotNull { place ->
            val orig = gridMap.QGisToGrid(place.x, place.y)
            val snap = gridMap.snapToNear(orig, maxRadius)
            if (snap == null) {
                null
            } else {
                MapPlace(place, orig, snap)
            }
        }
    }

    fun DistMatrix(points: List<GridPoint>): Array<DoubleArray> {
        val n = points.size
        val matrix = Array(n) { DoubleArray(n) { Double.POSITIVE_INFINITY } }

        for (i in 0 until n) {
            matrix[i][i] = 0.0
            for (j in i + 1 until n) {
                val path = gridMap.bfs(points[i], points[j])
                if (path != null) {
                    matrix[i][j] = path.dist
                    matrix[j][i] = path.dist
                }
            }
        }
        return matrix
    }

    fun restoreFullRoute(order: IntArray, points: List<GridPoint>): Path? {
        if (order.isEmpty()) return null

        val fullPoints = mutableListOf<GridPoint>()
        var totalDistance = 0.0

        for (i in 0 until order.size - 1) {
            val from = points[order[i]]
            val to = points[order[i + 1]]
            val path = gridMap.bfs(from, to) ?: return null

            if (i == 0) {
                fullPoints.addAll(path.points)
            } else {
                fullPoints.addAll(path.points.drop(1))
            }
            totalDistance += path.dist
        }
        return Path(fullPoints, totalDistance)
    }

    fun restoreSegments(order: IntArray, points: List<GridPoint>): List<Path>? {
        if (order.isEmpty()) return null
        val segments = mutableListOf<Path>()
        for (i in 0 until order.size - 1) {
            val from = points[order[i]]
            val to = points[order[i + 1]]
            val path = gridMap.bfs(from, to) ?: return null
            segments.add(path)
        }
        return segments
    }
}

class AntColony(
    val dist: Array<DoubleArray>,
    val antCount: Int = 10,
    val iter: Int = 100,
    val alpha: Double = 1.0,
    val beta: Double = 4.0,
    val evaporation: Double = 0.5,
    val q: Double = 100.0
) {
    val n = dist.size
    val pheromones = Array(n) { DoubleArray(n) { 1.0 } }
    val random = Random(System.currentTimeMillis())

    fun solve(startIndex: Int = 0, end: Boolean = false): Tour {
        var bestOrder = IntArray(0)
        var bestDist = Double.POSITIVE_INFINITY
        repeat(iter) {
            val tours = mutableListOf<Tour>()
            repeat(antCount) {
                val order = buildTour(startIndex, end)
                val dist = calculDist(order)
                val tour = Tour(order, dist)
                tours.add(tour)
                if (dist < bestDist) {
                    bestDist = dist
                    bestOrder = order
                }
            }
            evaporatePheromones()
            depositPheromones(tours)
        }
        return Tour(bestOrder, bestDist)
    }

    fun buildTour(startIndex: Int, end: Boolean): IntArray {
        val visited = BooleanArray(n)
        val order = mutableListOf<Int>()
        var current = startIndex
        order.add(current)
        visited[current] = true

        while (order.size < n) {
            val newCurrent = chooseNext(current, visited)
            order.add(newCurrent)
            visited[newCurrent] = true
            current = newCurrent
        }

        if (end) {
            order.add(startIndex)
        }
        return order.toIntArray()
    }

    fun chooseNext(current: Int, visited: BooleanArray): Int {
        val candit = mutableListOf<Pair<Int, Double>>()
        var total = 0.0

        for (next in 0 until n) {
            if (!visited[next] && dist[current][next].isFinite() && dist[current][next] > 0.0) {
                val tau = pheromones[current][next].pow(alpha)
                val eta = (1.0 / dist[current][next]).pow(beta)
                val value = tau * eta
                candit.add(next to value)
                total += value
            }
        }
        var r = random.nextDouble() * total
        for ((vertex, probability) in candit) {
            r -= probability
            if (r <= 0.0) {
                return vertex
            }
        }
        return candit.last().first
    }

    fun calculDist(order: IntArray): Double {
        var sum = 0.0
        for (i in 0 until order.size - 1) {

            val additDist = dist[order[i]][order[i + 1]]
            if (!additDist.isFinite()) return Double.POSITIVE_INFINITY
            sum += additDist
        }
        return sum
    }

    fun evaporatePheromones() {
        for (i in 0 until n) {
            for (j in 0 until n) {
                pheromones[i][j] *= (1.0 - evaporation)
                if (pheromones[i][j] < 0.0001) {
                    pheromones[i][j] = 0.0001
                }
            }
        }
    }

    fun depositPheromones(tours: List<Tour>) {
        for (tour in tours) {
            if (!tour.dist.isFinite() || tour.dist <= 0.0) {
                continue
            }
            val delta = q / tour.dist
            for (i in 0 until tour.vertex.size - 1) {
                val a = tour.vertex[i]
                val b = tour.vertex[i + 1]
                pheromones[a][b] += delta
                pheromones[b][a] += delta
            }
        }
    }
}
