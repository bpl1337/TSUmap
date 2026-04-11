package com.example.tsumobilkabeta.AStar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.yandex.mapkit.map.MapWindow

class AStarOverlayView(context: Context) : View(context) {
    var mapWindow: MapWindow? = null
    var grid: WalkabilityGrid? = null
    var barriers: Set<GridNode> = emptySet()
    var closedSet: Set<GridNode> = emptySet()
    var openSet: Set<GridNode> = emptySet()
    var currentNode: GridNode? = null
    var pathNodes: List<GridNode> = emptyList()

    private val barrierPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCDD2222.toInt()
        style = Paint.Style.FILL
    }
    private val closedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x994488FF.toInt()
        style = Paint.Style.FILL
    }
    private val openPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFCC00.toInt()
        style = Paint.Style.FILL
    }
    private val currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF6600.toInt()
        style = Paint.Style.FILL
    }
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00EE44.toInt()
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        val window = mapWindow ?: return
        val g = grid ?: return

        for (node in barriers) {
            val sp = window.worldToScreen(GridProjection.nodeToPoint(node, g)) ?: continue
            canvas.drawRect(sp.x - 7f, sp.y - 7f, sp.x + 7f, sp.y + 7f, barrierPaint)
        }
        for (node in closedSet) {
            val sp = window.worldToScreen(GridProjection.nodeToPoint(node, g)) ?: continue
            canvas.drawCircle(sp.x, sp.y, 5f, closedPaint)
        }
        for (node in openSet) {
            val sp = window.worldToScreen(GridProjection.nodeToPoint(node, g)) ?: continue
            canvas.drawCircle(sp.x, sp.y, 5f, openPaint)
        }
        currentNode?.let { node ->
            val sp = window.worldToScreen(GridProjection.nodeToPoint(node, g)) ?: return@let
            canvas.drawCircle(sp.x, sp.y, 8f, currentPaint)
        }
        for (node in pathNodes) {
            val sp = window.worldToScreen(GridProjection.nodeToPoint(node, g)) ?: continue
            canvas.drawCircle(sp.x, sp.y, 6f, pathPaint)
        }
    }
}