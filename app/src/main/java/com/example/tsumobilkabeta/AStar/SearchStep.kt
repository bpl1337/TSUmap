package com.example.tsumobilkabeta.AStar

data class SearchStep(
    val current: GridNode,
    val openSet: Set<GridNode>,
    val closedSet: Set<GridNode>,
    val path: List<GridNode>
)
