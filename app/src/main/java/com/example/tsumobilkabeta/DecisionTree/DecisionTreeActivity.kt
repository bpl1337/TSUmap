package com.example.tsumobilkabeta.DecisionTree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.example.tsumobilkabeta.ui.theme.TSUMapTheme

class DecisionTreeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TSUMapTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DecisionTreeScreen()
                }
            }
        }
    }
}

private enum class Phase { INPUT, TREE, CLASSIFY }

@Composable
fun DecisionTreeScreen() {
    val context = LocalContext.current
    var phase       by remember { mutableStateOf(Phase.INPUT) }
    var csvText     by remember { mutableStateOf("") }
    var dataset     by remember { mutableStateOf<Dataset?>(null) }
    var tree        by remember { mutableStateOf<TreeNode?>(null) }
    var pruned      by remember { mutableStateOf<TreeNode?>(null) }
    var usePruned   by remember { mutableStateOf(false) }
    var error       by remember { mutableStateOf<String?>(null) }

    val activeTree = if (usePruned && pruned != null) pruned!! else tree

    when (phase) {
        Phase.INPUT -> InputScreen(
            csvText = csvText,
            error = error,
            onCsvChange = { csvText = it; error = null },
            onSample = { csvText = DecisionTreeData.loadSampleCsv(context); error = null },
            onBuild = {
                try {
                    val ds = DecisionTreeData.parseCsv(csvText)
                    dataset = ds
                    val built = DecisionTreeBuilder.build(ds)
                    tree = built
                    pruned = TreePruner.prune(built, ds.rows, ds.target)
                    error = null
                    phase = Phase.TREE
                } catch (e: Exception) {
                    error = e.message ?: "Ошибка разбора CSV"
                }
            }
        )
        Phase.TREE -> TreeScreen(
            tree = tree!!,
            pruned = pruned,
            usePruned = usePruned,
            onTogglePruned = { usePruned = it },
            onClassify = { phase = Phase.CLASSIFY },
            onBack = { phase = Phase.INPUT }
        )
        Phase.CLASSIFY -> ClassifyScreen(
            tree = activeTree!!,
            dataset = dataset!!,
            usePruned = usePruned,
            onBack = { phase = Phase.TREE }
        )
    }
}

@Composable
private fun InputScreen(
    csvText: String,
    error: String?,
    onCsvChange: (String) -> Unit,
    onSample: () -> Unit,
    onBuild: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 52.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text("Дерево решений", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Куда пойти на обед?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedButton(onClick = onSample, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Загрузить учебный пример")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = csvText,
            onValueChange = onCsvChange,
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("Данные CSV") },
            placeholder = { Text("location,budget,...,recommended_place") },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            shape = RoundedCornerShape(12.dp),
            isError = error != null
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onBuild,
            enabled = csvText.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Построить дерево", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun TreeScreen(
    tree: TreeNode,
    pruned: TreeNode?,
    usePruned: Boolean,
    onTogglePruned: (Boolean) -> Unit,
    onClassify: () -> Unit,
    onBack: () -> Unit
) {
    val active = if (usePruned && pruned != null) pruned else tree
    val savings = if (pruned != null && tree != pruned)
        100 - nodeCount(pruned) * 100 / nodeCount(tree) else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 52.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) { Text("← Назад") }
            Text("Дерево решений", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    StatCell("Глубина", treeDepth(active).toString())
                    StatCell("Узлов", nodeCount(active).toString())
                    if (savings > 0) StatCell("Сжатие", "-$savings%")
                }
                if (pruned != null && tree != pruned) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Switch(checked = usePruned, onCheckedChange = onTogglePruned)
                        Text(
                            if (usePruned) "Оптимизированное дерево" else "Полное дерево",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            LegendItem(Color(0xFF3A8EFF), "Вопрос")
            LegendItem(Color(0xFF2ECC71), "Заведение")
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                TreeCanvas(active)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onClassify,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Классифицировать →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private const val NODE_W  = 116f
private const val NODE_H  = 52f
private const val V_STEP  = 140f
private const val H_STEP  = 140f
private const val PADDING = 20f

private data class LayoutNode(
    val cx: Float,
    val cy: Float,
    val label: String,
    val isLeaf: Boolean,
    val children: List<Pair<String, LayoutNode>>
)

private fun buildLayout(node: TreeNode, depth: Int = 0, offset: Float = 0f): Pair<LayoutNode, Float> {
    val cy = depth * V_STEP + NODE_H / 2f
    return when (node) {
        is TreeNode.Leaf -> {
            LayoutNode(offset + H_STEP / 2f, cy, node.label, true, emptyList()) to H_STEP
        }
        is TreeNode.Split -> {
            var cur = offset
            val kids = mutableListOf<Pair<String, LayoutNode>>()
            var totalW = 0f
            for ((v, child) in node.children) {
                val (ln, w) = buildLayout(child, depth + 1, cur)
                kids += v to ln
                cur += w; totalW += w
            }
            val cx = if (kids.isEmpty()) offset + H_STEP / 2f
                     else (kids.first().second.cx + kids.last().second.cx) / 2f
            LayoutNode(cx, cy, node.attribute, false, kids) to maxOf(totalW, H_STEP)
        }
    }
}

@Composable
private fun TreeCanvas(tree: TreeNode) {
    val (root, totalW) = remember(tree) { buildLayout(tree) }
    val depth          = remember(tree) { treeDepth(tree) }
    val textMeasurer   = rememberTextMeasurer()
    val density        = LocalDensity.current

    val canvasW = with(density) { (totalW + PADDING * 2).dp }
    val canvasH = with(density) { ((depth - 1) * V_STEP + NODE_H + PADDING * 2).dp }

    val colorNode = Color(0xFF3A8EFF)
    val colorLeaf = Color(0xFF2ECC71)
    val colorLine = Color(0xFF9E9E9E)

    val nodeStyle = TextStyle(color = Color(0xFF1A6FFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    val edgeStyle = TextStyle(color = Color(0xFF1A6FFF), fontSize = 11.sp, textAlign = TextAlign.Center)

    Canvas(modifier = Modifier.size(canvasW, canvasH)) {
        val pad = PADDING.dp.toPx()
        val nW  = NODE_W.dp.toPx()
        val nH  = NODE_H.dp.toPx()

        fun drawNode(node: LayoutNode) {
            val cx = node.cx.dp.toPx() + pad
            val cy = node.cy.dp.toPx() + pad

            if (node.children.isNotEmpty()) {
                val parentBottom = cy + nH / 2f
                val firstChildTop = node.children.first().second.cy.dp.toPx() + pad - nH / 2f
                val forkY = parentBottom + (firstChildTop - parentBottom) * 0.38f

                drawLine(colorLine, Offset(cx, parentBottom), Offset(cx, forkY), strokeWidth = 2.dp.toPx())

                if (node.children.size > 1) {
                    val leftX  = node.children.first().second.cx.dp.toPx() + pad
                    val rightX = node.children.last().second.cx.dp.toPx() + pad
                    drawLine(colorLine, Offset(leftX, forkY), Offset(rightX, forkY), strokeWidth = 2.dp.toPx())
                }

                for ((edgeLabel, child) in node.children) {
                    val ccx   = child.cx.dp.toPx() + pad
                    val cTop  = child.cy.dp.toPx() + pad - nH / 2f
                    val arrow = 6.dp.toPx()

                    drawLine(colorLine, Offset(ccx, forkY), Offset(ccx, cTop), strokeWidth = 2.dp.toPx())

                    drawPath(
                        path = Path().apply {
                            moveTo(ccx, cTop)
                            lineTo(ccx - arrow, cTop - arrow * 1.4f)
                            lineTo(ccx + arrow, cTop - arrow * 1.4f)
                            close()
                        },
                        color = colorLine,
                        style = Fill
                    )

                    val edgeText   = DecisionTreeData.valueLabel(edgeLabel)
                    val edgeMeasured = textMeasurer.measure(
                        edgeText, edgeStyle,
                        constraints = Constraints(maxWidth = (H_STEP.dp.toPx()).toInt())
                    )
                    val labelMidY = (forkY + cTop) / 2f
                    drawText(
                        edgeMeasured,
                        topLeft = Offset(
                            ccx - edgeMeasured.size.width / 2f,
                            labelMidY - edgeMeasured.size.height / 2f
                        )
                    )
                }
            }

            drawRoundRect(
                color = if (node.isLeaf) colorLeaf else colorNode,
                topLeft = Offset(cx - nW / 2f, cy - nH / 2f),
                size = Size(nW, nH),
                cornerRadius = CornerRadius(12.dp.toPx())
            )

            val displayText = if (node.isLeaf) DecisionTreeData.placeLabel(node.label)
                              else DecisionTreeData.featureLabel(node.label)
            val measured = textMeasurer.measure(
                displayText, nodeStyle,
                constraints = Constraints(maxWidth = nW.toInt())
            )
            drawText(
                measured,
                topLeft = Offset(
                    cx - measured.size.width / 2f,
                    cy - measured.size.height / 2f
                )
            )

            for ((_, child) in node.children) drawNode(child)
        }

        drawNode(root)
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ClassifyScreen(
    tree: TreeNode,
    dataset: Dataset,
    usePruned: Boolean,
    onBack: () -> Unit
) {
    val availableValues = remember {
        dataset.attributes.associateWith { attr ->
            dataset.rows.map { it[attr]!! }.distinct().sorted()
        }
    }
    val selection = remember { mutableStateMapOf<String, String>() }
    var result by remember { mutableStateOf<ClassificationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 52.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) { Text("← Назад") }
            Text(
                if (usePruned) "Классификация (оптим.)" else "Классификация",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            dataset.attributes.forEach { attr ->
                val values = availableValues[attr] ?: return@forEach
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            DecisionTreeData.featureLabel(attr),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            values.forEach { v ->
                                val selected = selection[attr] == v
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) Color(0xFF3A8EFF).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                        .border(1.5.dp, if (selected) Color(0xFF3A8EFF) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                        .clickable { selection[attr] = v; result = null }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        DecisionTreeData.valueLabel(v),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color(0xFF3A8EFF) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(visible = result != null, enter = fadeIn() + slideInVertically { it }) {
            result?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2ECC71).copy(alpha = 0.12f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Рекомендация", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2ECC71))
                        Text(
                            DecisionTreeData.placeLabel(res.label),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2ECC71),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Путь по дереву:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        res.path.forEachIndexed { i, (attr, v) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                                Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF3A8EFF)), Alignment.Center) {
                                    Text("${i + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${DecisionTreeData.featureLabel(attr)}  →  ${DecisionTreeData.valueLabel(v)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { result = classifyWithPath(tree, selection.toMap()) },
            enabled = dataset.attributes.all { selection[it]?.isNotEmpty() == true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A8EFF))
        ) {
            Text("Определить место", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
