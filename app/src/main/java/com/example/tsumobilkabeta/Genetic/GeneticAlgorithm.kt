package com.example.tsumobilkabeta.Genetic

import com.example.tsumobilkabeta.AStar.GridNode
import kotlin.random.Random

data class Route(
    val establishments: List<FoodEstablishment>,
    val fitness: Double,
    val totalDistMeters: Double,
    val totalTimeMin: Double,
    val pathSegments: List<List<GridNode>>
)

class GeneticAlgorithm(
    private val data: PrecomputedData,
    private val requiredItems: Set<String>,
    val totalGenerations: Int = 250,
    private val populationSize: Int = 120,
    private val mutationRate: Double = 0.18,
    private val eliteCount: Int = 12,
    private val walkingSpeedKmH: Double = 5.0,
    private val startHour: Int = 9,
    private val startMinute: Int = 0
) {
    var bestRoute: Route? = null
        private set
    var currentGeneration: Int = 0
        private set
    var onGenerationUpdate: ((Route, Int) -> Unit)? = null
    private val n = data.establishments.size

    fun run(): Route? {
        if (n == 0) return null

        var population: List<IntArray> = List(populationSize) {
            IntArray(n) { it }.also { arr -> arr.shuffle() }
        }

        var best = population.mapNotNull { decode(it) }.minByOrNull { it.fitness }
            ?: return null
        bestRoute = best
        onGenerationUpdate?.invoke(best, 0)

        for (gen in 1..totalGenerations) {
            currentGeneration = gen

            val evaluated = population
                .mapNotNull { chr -> decode(chr)?.let { chr to it } }
                .sortedBy { it.second.fitness }

            if (evaluated.isEmpty()) break

            val genBest = evaluated.first().second
            if (genBest.fitness < best.fitness) {
                best = genBest
                bestRoute = best
            }
            onGenerationUpdate?.invoke(best, gen)

            val nextPop = mutableListOf<IntArray>()
            evaluated.take(eliteCount).forEach { nextPop.add(it.first.copyOf()) }

            while (nextPop.size < populationSize) {
                val p1 = tournament(evaluated)
                val p2 = tournament(evaluated)
                var child = ox1Crossover(p1, p2)
                if (Random.nextDouble() < mutationRate) child = mutate(child)
                nextPop.add(child)
            }

            population = nextPop
        }

        return bestRoute
    }

    private fun decode(chromosome: IntArray): Route? {
        val covered = mutableSetOf<String>()
        val visitedIdx = mutableListOf<Int>()

        for (estIdx in chromosome) {
            if (covered.containsAll(requiredItems)) break
            val est = data.establishments[estIdx]
            val newItems = est.menu.map { it.name }.filter { it in requiredItems && it !in covered }
            if (newItems.isNotEmpty()) {
                visitedIdx.add(estIdx)
                covered.addAll(newItems)
            }
        }

        if (!covered.containsAll(requiredItems)) return null

        val nodeSeq = listOf(0) + visitedIdx.map { it + 1 }

        var totalDist = 0.0
        var fitness = 0.0
        var currentTimeMin = startHour * 60 + startMinute
        val segments = mutableListOf<List<GridNode>>()

        for (i in 0 until nodeSeq.size - 1) {
            val from = nodeSeq[i];
            val to = nodeSeq[i + 1]
            val d = data.distMatrix[from][to]
            val unreachable = d >= Double.MAX_VALUE / 2
            totalDist += if (unreachable) 500_000.0 else d
            if (unreachable) fitness += 300_000.0

            val path = data.pathMatrix[from][to]
            segments.add(path ?: listOf(data.cells[from], data.cells[to]))

            val travelMin = if (unreachable) 60
            else ((d / 1000.0 / walkingSpeedKmH) * 60.0).toInt()
            currentTimeMin += travelMin

            val est = data.establishments[visitedIdx[i]]
            val arrHour = (currentTimeMin / 60) % 24
            val arrMin = currentTimeMin % 60

            if (!est.isOpenAt(arrHour, arrMin)) {
                fitness += 200_000.0
            } else {
                val minLeft = est.minutesUntilClose(arrHour, arrMin)
                if (minLeft < 15) fitness += 50_000.0
            }

            currentTimeMin += 3
        }

        val totalTimeMin = (totalDist / 1000.0 / walkingSpeedKmH) * 60.0 + visitedIdx.size * 3.0
        fitness += totalDist

        return Route(
            visitedIdx.map { data.establishments[it] },
            fitness,
            totalDist,
            totalTimeMin,
            segments
        )
    }

    private fun tournament(evaluated: List<Pair<IntArray, Route>>, k: Int = 7): IntArray {
        return (0 until k)
            .map { evaluated[Random.nextInt(evaluated.size)] }
            .minByOrNull { it.second.fitness }!!.first
    }

    private fun ox1Crossover(p1: IntArray, p2: IntArray): IntArray {
        val a = Random.nextInt(n);
        val b = Random.nextInt(n)
        val lo = minOf(a, b);
        val hi = maxOf(a, b)
        val child = IntArray(n) { -1 }
        for (i in lo..hi) child[i] = p1[i]
        val inChild = child.toHashSet()
        var pos = (hi + 1) % n
        for (v in p2) {
            if (v !in inChild) {
                child[pos] = v; inChild.add(v); pos = (pos + 1) % n
            }
        }
        return child
    }

    private fun mutate(chromosome: IntArray): IntArray {
        val c = chromosome.copyOf()
        return when (Random.nextInt(3)) {
            0 -> {
                val i = Random.nextInt(n);
                var j = Random.nextInt(n)
                while (j == i) j = Random.nextInt(n)
                c[i] = c[j].also { c[j] = c[i] }; c
            }

            1 -> {
                val a = Random.nextInt(n);
                val b = Random.nextInt(n)
                val lo = minOf(a, b);
                val hi = maxOf(a, b)
                var l = lo;
                var r = hi
                while (l < r) {
                    c[l] = c[r].also { c[r] = c[l] }; l++; r--
                }; c
            }

            else -> {
                val i = Random.nextInt(n);
                val gene = c[i]
                val arr = c.toMutableList(); arr.removeAt(i)
                arr.add(Random.nextInt(arr.size + 1), gene); arr.toIntArray()
            }
        }
    }
}
