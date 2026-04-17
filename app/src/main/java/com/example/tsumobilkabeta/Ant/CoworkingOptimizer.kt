package com.example.tsumobilkabeta.Ant

import kotlin.math.pow
import kotlin.random.Random

data class CoworkingLocation(
    val id: Int,
    val name: String,
    val x: Double,
    val y: Double,
    val capacity: Int,
    val comfort: Double
)

data class CoworkingAssignment(
    val location: CoworkingLocation,
    val studentsAssigned: Int,
    val distance: Double
)

data class CoworkingResult(
    val assignments: List<CoworkingAssignment>,
    val totalDistance: Double,
    val avgComfort: Double
)

object CoworkingDatabase {
    val locations = listOf(
        CoworkingLocation(1, "Научная библиотека ТГУ", 9456380.0, 7652400.0, 80, 4.5),
        CoworkingLocation(2, "Коворкинг «Точка кипения»", 9456520.0, 7652320.0, 40, 5.0),
        CoworkingLocation(3, "Читальный зал НБ", 9456300.0, 7652380.0, 50, 4.0),
        CoworkingLocation(4, "Аудитория 228", 9456460.0, 7652250.0, 30, 3.0),
        CoworkingLocation(5, "Студенческий коворкинг", 9456200.0, 7652150.0, 25, 4.2),
        CoworkingLocation(6, "Компьютерный класс", 9456350.0, 7651950.0, 20, 3.5),
        CoworkingLocation(7, "Кафе «Перемена»", 9456100.0, 7652050.0, 15, 3.8)
    )

    val totalCapacity: Int get() = locations.sumOf { it.capacity }
}

class CoworkingOptimizer(
    private val locations: List<CoworkingLocation>,
    private val distances: DoubleArray,
    private val studentCount: Int,
    private val antCount: Int = 60,
    private val iterations: Int = 120,
    private val alpha: Double = 1.0,
    private val beta: Double = 2.0,
    private val gamma: Double = 1.5,
    private val evaporation: Double = 0.3,
    private val q: Double = 100.0
) {
    private val n = locations.size
    private val pheromones = DoubleArray(n) { 1.0 }
    private val random = Random(System.currentTimeMillis())

    fun solve(): CoworkingResult {
        var bestAllocation: IntArray? = null
        var bestScore = Double.NEGATIVE_INFINITY

        repeat(iterations) {
            val allocation = IntArray(n)

            repeat(studentCount) {
                val chosen = chooseLocation(allocation)
                allocation[chosen]++
            }

            val score = evaluateAllocation(allocation)
            if (score > bestScore) {
                bestScore = score
                bestAllocation = allocation.clone()
            }

            evaporatePheromones()
            depositPheromones(allocation, score)
        }

        return buildResult(bestAllocation ?: IntArray(n))
    }

    private fun chooseLocation(currentAllocation: IntArray): Int {
        val probabilities = DoubleArray(n)
        var total = 0.0

        for (i in 0 until n) {
            if (!distances[i].isFinite() || distances[i] <= 0) continue

            val remaining = (locations[i].capacity - currentAllocation[i]).coerceAtLeast(0)
            val capacityFactor = if (remaining > 0) {
                remaining.toDouble() / locations[i].capacity
            } else {
                0.05
            }

            val tau = pheromones[i].pow(alpha)
            val comfortHeuristic = locations[i].comfort.pow(gamma)
            val distHeuristic = (1.0 / distances[i]).pow(beta)
            val eta = comfortHeuristic * distHeuristic * capacityFactor

            probabilities[i] = tau * eta
            total += probabilities[i]
        }

        if (total <= 0) return random.nextInt(n)

        var r = random.nextDouble() * total
        for (i in 0 until n) {
            r -= probabilities[i]
            if (r <= 0) return i
        }
        return n - 1
    }

    private fun evaluateAllocation(allocation: IntArray): Double {
        var score = 0.0
        for (i in 0 until n) {
            if (allocation[i] == 0) continue
            if (!distances[i].isFinite()) continue

            val usedCapacity = minOf(allocation[i], locations[i].capacity)
            val overflow = maxOf(0, allocation[i] - locations[i].capacity)

            score += locations[i].comfort * usedCapacity / distances[i]
            score -= overflow * 10.0 / distances[i]
        }
        return score
    }

    private fun evaporatePheromones() {
        for (i in 0 until n) {
            pheromones[i] *= (1.0 - evaporation)
            if (pheromones[i] < 0.001) pheromones[i] = 0.001
        }
    }

    private fun depositPheromones(allocation: IntArray, score: Double) {
        if (score <= 0) return
        val delta = q * score
        for (i in 0 until n) {
            if (allocation[i] > 0) {
                pheromones[i] += delta * allocation[i] / studentCount
            }
        }
    }

    private fun buildResult(allocation: IntArray): CoworkingResult {
        val assignments = locations.mapIndexed { idx, loc ->
            CoworkingAssignment(
                location = loc,
                studentsAssigned = allocation[idx],
                distance = if (distances[idx].isFinite()) distances[idx] else 0.0
            )
        }

        val totalDist = assignments.sumOf { it.distance * it.studentsAssigned }
        val occupied = assignments.filter { it.studentsAssigned > 0 }
        val avgComfort = if (occupied.isNotEmpty()) {
            occupied.sumOf { it.location.comfort * it.studentsAssigned } /
                    occupied.sumOf { it.studentsAssigned }.coerceAtLeast(1)
        } else 0.0

        return CoworkingResult(assignments, totalDist, avgComfort)
    }
}
