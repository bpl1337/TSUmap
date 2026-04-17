package com.example.tsumobilkabeta.DecisionTree

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object DecisionTreeData {

    private const val TARGET = "recommended_place"

    fun parseCsv(csvText: String): Dataset {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        require(lines.size >= 2) { "CSV должен содержать заголовок и хотя бы одну строку данных" }
        val header = lines.first().split(",").map { it.trim() }
        require(header.contains(TARGET)) { "Столбец '$TARGET' не найден в заголовке" }
        val rows = lines.drop(1).map { line ->
            header.zip(line.split(",").map { it.trim() }).toMap()
        }
        return Dataset(header.filter { it != TARGET }, TARGET, rows)
    }

    fun loadSampleCsv(context: Context): String =
        context.assets.open("decision_tree_sample.csv").use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }

    private val featureLabels = mapOf(
        "location" to "Местоположение",
        "budget" to "Бюджет",
        "time_available" to "Время",
        "food_type" to "Тип еды",
        "queue_tolerance" to "Очередь",
        "weather" to "Погода"
    )

    private val valueLabels = mapOf(
        "main_building" to "Главный корпус",
        "second_building" to "Второй корпус",
        "bus_stop" to "Остановка",
        "campus_center" to "Центр кампуса",
        "low" to "Низкий",
        "medium" to "Средний",
        "high" to "Высокий",
        "very_short" to "Очень мало",
        "short" to "Мало",
        "coffee" to "Кофе",
        "pancakes" to "Блины",
        "full_meal" to "Полный обед",
        "snack" to "Перекус",
        "good" to "Хорошая",
        "bad" to "Плохая"
    )

    private val placeLabels = mapOf(
        "Starbooks" to "Starbooks",
        "Siberian_Pancakes" to "Сибирские блины",
        "Main_Cafeteria" to "Главная столовая",
        "Yarche" to "Ярче",
        "Bus_Stop_Coffee" to "Кофе у остановки",
        "Second_Building_Cafe" to "Кафе 2-го корпуса",
        "Vending_Machine" to "Автомат"
    )

    fun featureLabel(key: String) = featureLabels[key] ?: key
    fun valueLabel(key: String) = valueLabels[key] ?: key
    fun placeLabel(key: String) = placeLabels[key] ?: key
}
