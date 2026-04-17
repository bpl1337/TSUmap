package com.example.tsumobilkabeta.DecisionTree

import kotlin.math.ln

sealed class TreeNode {
    data class Leaf(val label: String, val count: Int) : TreeNode()
    data class Split(
        val attribute: String, val children: Map<String, TreeNode>, val majorityLabel: String
    ) : TreeNode()
}

data class Dataset(
    val attributes: List<String>, val target: String, val rows: List<Map<String, String>>
)

data class ClassificationResult(val label: String, val path: List<Pair<String, String>>)

fun buildTree(data: Dataset): TreeNode = buildNode(data.rows, data.attributes, data.target)

private fun buildNode(
    rows: List<Map<String, String>>, attrs: List<String>, target: String
): TreeNode {
    if (rows.isEmpty()) return TreeNode.Leaf("?", 0)
    val counts = rows.groupingBy { it[target]!! }.eachCount()
    val majority = counts.maxByOrNull { it.value }!!.key
    if (counts.size == 1 || attrs.isEmpty()) return TreeNode.Leaf(majority, rows.size)
    val best = attrs.maxByOrNull { infoGain(rows, it, target) }!!
    val children =
        rows.groupBy { it[best]!! }.mapValues { (_, s) -> buildNode(s, attrs - best, target) }
    return TreeNode.Split(best, children, majority)
}

private fun entropy(rows: List<Map<String, String>>, target: String): Double {
    if (rows.isEmpty()) return 0.0
    val total = rows.size.toDouble()
    return -rows.groupingBy { it[target]!! }.eachCount().values.sumOf { c ->
        val p = c / total; if (p > 0) p * ln(p) / ln(2.0) else 0.0
    }
}

private fun infoGain(rows: List<Map<String, String>>, attr: String, target: String): Double {
    val total = rows.size.toDouble()
    return entropy(
        rows, target
    ) - rows.groupBy { it[attr]!! }.values.sumOf { s -> (s.size / total) * entropy(s, target) }
}

fun prune(node: TreeNode, data: List<Map<String, String>>, target: String): TreeNode {
    if (node is TreeNode.Leaf) return node
    node as TreeNode.Split
    val parts = data.groupBy { it[node.attribute] ?: "" }
    val pruned = TreeNode.Split(
        attribute = node.attribute, children = node.children.mapValues { (v, child) ->
            prune(
                child, parts[v] ?: emptyList(), target
            )
        }, majorityLabel = node.majorityLabel
    )
    return if (data.count { it[target] == node.majorityLabel } >= data.count {
            classify(
                pruned, it
            ) == it[target]
        }) TreeNode.Leaf(node.majorityLabel, data.size) else pruned
}

fun classify(node: TreeNode, record: Map<String, String>): String {
    var cur = node
    while (cur is TreeNode.Split) cur = cur.children[record[cur.attribute] ?: break] ?: break
    return if (cur is TreeNode.Leaf) cur.label else (cur as TreeNode.Split).majorityLabel
}

fun classifyWithPath(node: TreeNode, record: Map<String, String>): ClassificationResult {
    val path = mutableListOf<Pair<String, String>>()
    var cur = node
    while (cur is TreeNode.Split) {
        val v = record[cur.attribute] ?: break
        path += cur.attribute to v
        cur = cur.children[v] ?: break
    }
    return ClassificationResult(
        if (cur is TreeNode.Leaf) cur.label else (cur as TreeNode.Split).majorityLabel, path
    )
}

fun treeDepth(node: TreeNode): Int = when (node) {
    is TreeNode.Leaf -> 1
    is TreeNode.Split -> 1 + (node.children.values.maxOfOrNull { treeDepth(it) } ?: 0)
}

fun nodeCount(node: TreeNode): Int = when (node) {
    is TreeNode.Leaf -> 1
    is TreeNode.Split -> 1 + node.children.values.sumOf { nodeCount(it) }
}
