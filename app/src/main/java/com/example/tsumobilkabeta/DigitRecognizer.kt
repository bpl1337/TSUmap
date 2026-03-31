package com.example.tsumobilkabeta


interface DigitRecognizer {

    fun predict(pixelData: FloatArray): DigitPrediction
    

    fun loadModel(): Boolean
    

    fun release()
}


data class DigitPrediction(

    val digit: Int,

    val confidence: Float,

    val probabilities: FloatArray? = null
)


class NoOpDigitRecognizer : DigitRecognizer {
    override fun predict(pixelData: FloatArray): DigitPrediction {

        return DigitPrediction(digit = -1, confidence = 0f)
    }
    
    override fun loadModel(): Boolean {
        return true
    }
    
    override fun release() {

    }
}

