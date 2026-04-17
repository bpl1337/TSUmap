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
        CoworkingLocation(1, "Научная библиотека ТГУ",       9456530.0, 7652203.0, 80, 4.5),
        CoworkingLocation(2, "Коворкинг 4 корпуса ТГУ»",    9455717.0, 7652516.0, 40, 5.0),
        CoworkingLocation(3, "IDO 2 Корпус",             9456064.0, 7652289.0, 50, 4.0),
        CoworkingLocation(4, "Аудитория 228",                9456356.0, 7652446.0, 30, 3.0),
        CoworkingLocation(5, "Центральный корпус ТГУ",       9456218.0, 7652392.0, 25, 4.2),
        CoworkingLocation(6, "Коворкинг 3 корпуса ТГУ",           9456624.0, 7652057.0, 20, 3.5),
        CoworkingLocation(7, "Абрикос Зона Еды",              9455618.0, 7652869.0, 15, 3.8)
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
            val solutions = mutableListOf<Pair<IntArray, Double>>()

            repeat(antCount) {
                val allocation = IntArray(n)

                repeat(studentCount) {
                    val chosen = chooseLocation(allocation)
                    allocation[chosen]++
                }

                val score = evaluateAllocation(allocation)
                solutions.add(allocation.clone() to score)

                if (score > bestScore) {
                    bestScore = score
                    bestAllocation = allocation.clone()
                }
            }

            evaporatePheromones()

            val bestSolutions = solutions
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }
                .take(maxOf(1, antCount / 4))

            bestSolutions.forEach { (allocation, score) ->
                depositPheromones(allocation, score)
            }
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
