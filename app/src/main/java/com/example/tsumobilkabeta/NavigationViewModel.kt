package com.example.tsumobilkabeta

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.yandex.mapkit.geometry.Point

class NavigationViewModel : ViewModel() {
    val startPoint: MutableState<Point?> = mutableStateOf(null)
    val endPoint: MutableState<Point?> = mutableStateOf(null)

    var gridLoaded by mutableStateOf(false)
        private set

    fun loadGrid(context: Context) {
        if (gridLoaded) return
        context.applicationContext
        gridLoaded = true
    }

    fun setStartPoint(point: Point) {
        startPoint.value = point
    }

    fun setEndPoint(point: Point) {
        endPoint.value = point
    }

    fun resetPoints() {
        startPoint.value = null
        endPoint.value = null
    }
}

