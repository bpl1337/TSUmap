package com.example.tsumobilkabeta

import android.content.Context
import java.io.File

class KnnClassifier(private val context: Context) {
    private val templates = mutableListOf<Pair<Int, FloatArray>>()
    private val fileName = "knn_storage.txt"

    init {
        checkAndCopyAssets()
        loadFromDisk()
    }

    private fun checkAndCopyAssets() {
        val file = File(context.filesDir, fileName)

        if (!file.exists()) {
            try {
                context.assets.open(fileName).use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTemplate(digit: Int, data: FloatArray) {
        templates.add(digit to data)
        saveToDisk()
    }

    fun classify(userData: FloatArray): Int {
        if (templates.isEmpty()) return -1
        var bestDigit = -1
        var minDistance = Float.MAX_VALUE

        for ((digit, templateData) in templates) {
            var dist = 0f
            for (i in userData.indices) {
                val diff = userData[i] - templateData[i]
                dist += diff * diff
            }
            if (dist < minDistance) {
                minDistance = dist
                bestDigit = digit
            }
        }
        return bestDigit
    }

    fun removeLastTemplate() {
        if (templates.isNotEmpty()) {
            templates.removeAt(templates.size - 1)
            saveToDisk()
        }
    }

    private fun saveToDisk() {
        val file = File(context.filesDir, fileName)
        file.printWriter().use { out ->
            templates.forEach { out.println("${it.first}|${it.second.joinToString(",")}") }
        }
    }

    private fun loadFromDisk() {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return
        try {
            file.forEachLine { line ->
                val parts = line.split("|")
                if (parts.size == 2) {
                    val d = parts[0].toInt()
                    val arr = parts[1].split(",").map { it.toFloat() }.toFloatArray()
                    templates.add(d to arr)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}