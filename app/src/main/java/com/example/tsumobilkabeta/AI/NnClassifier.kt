package com.example.tsumobilkabeta.AI

import android.content.Context

class NnClassifier(private val context: Context) {
    private val INPUT_SIZE = 2500
    private val HIDDEN1 = 128
    private val HIDDEN2 = 64
    private val OUTPUT_SIZE = 10

    private var w1: FloatArray? = null
    private var b1: FloatArray? = null
    private var w2: FloatArray? = null
    private var b2: FloatArray? = null
    private var w3: FloatArray? = null
    private var b3: FloatArray? = null
    private var initialized = false
    companion object {
        private const val WEIGHT_FILE = "nn_weights.bin"
    }

    fun loadWeights(): Boolean {
        if (initialized) return true
        return try {
            val bytes = context.assets.open(WEIGHT_FILE).use { it.readBytes() }
            var pos = 0

            val nLayers = readInt(bytes, pos); pos += 4
            val h1 = readInt(bytes, pos); pos += 4
            val h2 = readInt(bytes, pos); pos += 4
            if (nLayers != 3 || h1 != HIDDEN1 || h2 != HIDDEN2) return false

            w1 = FloatArray(INPUT_SIZE * HIDDEN1) { readFloat(bytes, pos + it * 4) }; pos += INPUT_SIZE * HIDDEN1 * 4
            b1 = FloatArray(HIDDEN1) { readFloat(bytes, pos + it * 4) }; pos += HIDDEN1 * 4
            w2 = FloatArray(HIDDEN1 * HIDDEN2) { readFloat(bytes, pos + it * 4) }; pos += HIDDEN1 * HIDDEN2 * 4
            b2 = FloatArray(HIDDEN2) { readFloat(bytes, pos + it * 4) }; pos += HIDDEN2 * 4
            w3 = FloatArray(HIDDEN2 * OUTPUT_SIZE){ readFloat(bytes, pos + it * 4) }; pos += HIDDEN2 * OUTPUT_SIZE * 4
            b3 = FloatArray(OUTPUT_SIZE) { readFloat(bytes, pos + it * 4) }

            initialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun classifyWithConfidence(input: FloatArray): Pair<Int, Float> {
        if (!initialized || input.size != INPUT_SIZE) return Pair(-1, 0f)
        val z1 = matVecMul(w1!!, input, INPUT_SIZE, HIDDEN1)
        for (j in z1.indices) z1[j] += b1!![j]
        val a1 = relu(z1)
        val z2 = matVecMul(w2!!, a1, HIDDEN1, HIDDEN2)
        for (j in z2.indices) z2[j] += b2!![j]
        val a2 = relu(z2)
        val z3 = matVecMul(w3!!, a2, HIDDEN2, OUTPUT_SIZE)
        for (j in z3.indices) z3[j] += b3!![j]
        val probs = softmax(z3)
        val digit = argmax(probs)
        return Pair(digit, probs[digit])
    }
    private fun readInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    private fun readFloat(bytes: ByteArray, offset: Int): Float =
        Float.fromBits(readInt(bytes, offset))

    private fun matVecMul(W: FloatArray, x: FloatArray, rows: Int, cols: Int): FloatArray {
        val y = FloatArray(cols)
        for (i in 0 until rows) {
            val xi = x[i]
            val base = i * cols
            for (j in 0 until cols) y[j] += W[base + j] * xi
        }
        return y
    }
    private fun relu(v: FloatArray): FloatArray =
        FloatArray(v.size) { if (v[it] > 0f) v[it] else 0f }

    private fun softmax(v: FloatArray): FloatArray {
        val max = v.max()
        val exp = FloatArray(v.size) { Math.exp((v[it] - max).toDouble()).toFloat() }
        val sum = exp.sum()
        return FloatArray(exp.size) { exp[it] / sum }
    }

    private fun argmax(v: FloatArray): Int {
        var best = 0
        for (i in 1 until v.size) if (v[i] > v[best]) best = i
        return best
    }
}
