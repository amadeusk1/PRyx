package com.amadeusk.liftlog.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.amadeusk.liftlog.data.DashboardSectionItem
import com.amadeusk.liftlog.data.HomeLiftItem
import com.amadeusk.liftlog.data.defaultDashboardLayout
import com.amadeusk.liftlog.data.defaultHomeLiftLayout
import kotlin.math.roundToInt

private val SettingItemSpacing = 8.dp

private data class ReorderableSettingRow(
    val id: String,
    val label: String,
    val enabled: Boolean
)

@Composable
fun HomeScreenSettingsDialog(
    layout: List<DashboardSectionItem>,
    homeLiftLayout: List<HomeLiftItem>,
    onLayoutChange: (List<DashboardSectionItem>) -> Unit,
    onHomeLiftLayoutChange: (List<HomeLiftItem>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = { Text("Home screen") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Sections",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose what appears. Drag a section to reorder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                ReorderableSettingList(
                    items = layout.map {
                        ReorderableSettingRow(
                            id = it.section.id,
                            label = it.section.label,
                            enabled = it.enabled
                        )
                    },
                    onEnabledChange = { index, enabled ->
                        val updated = layout.toMutableList()
                        updated[index] = layout[index].copy(enabled = enabled)
                        onLayoutChange(updated)
                    },
                    onReorder = { from, to ->
                        val updated = layout.toMutableList()
                        val moved = updated.removeAt(from)
                        updated.add(to, moved)
                        onLayoutChange(updated)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Lifts",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose which lifts appear. Drag a lift to reorder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (homeLiftLayout.isEmpty()) {
                    Text(
                        text = "Log a PR to add more exercises.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ReorderableSettingList(
                        items = homeLiftLayout.map {
                            ReorderableSettingRow(
                                id = it.name,
                                label = it.name,
                                enabled = it.enabled
                            )
                        },
                        minEnabledCount = 1,
                        onEnabledChange = { index, enabled ->
                            val updated = homeLiftLayout.toMutableList()
                            updated[index] = homeLiftLayout[index].copy(enabled = enabled)
                            onHomeLiftLayoutChange(updated)
                        },
                        onReorder = { from, to ->
                            val updated = homeLiftLayout.toMutableList()
                            val moved = updated.removeAt(from)
                            updated.add(to, moved)
                            onHomeLiftLayoutChange(updated)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        onLayoutChange(defaultDashboardLayout())
                        onHomeLiftLayoutChange(defaultHomeLiftLayout())
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Reset to defaults")
                }
            }
        }
    )
}

@Composable
private fun ReorderableSettingList(
    items: List<ReorderableSettingRow>,
    minEnabledCount: Int = 0,
    onEnabledChange: (index: Int, enabled: Boolean) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit
) {
    var dragFromIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val gapPx = with(density) { SettingItemSpacing.toPx() }
    val fallbackStepPx = with(density) { (52.dp + SettingItemSpacing).toPx() }
    var stepPx by remember { mutableFloatStateOf(fallbackStepPx) }

    SideEffect {
        stepPx = if (rowHeightPx > 0f) rowHeightPx + gapPx else fallbackStepPx
    }

    val draggingIndex = if (dragFromIndex >= 0) dragFromIndex else -1
    val lastIndex = items.lastIndex

    val targetIndex = if (draggingIndex >= 0 && lastIndex >= 0) {
        (draggingIndex + (dragOffsetY / stepPx).roundToInt()).coerceIn(0, lastIndex)
    } else {
        -1
    }

    fun finishDrag() {
        val from = dragFromIndex
        if (from < 0 || lastIndex < 0) return
        val to = (from + (dragOffsetY / stepPx).roundToInt()).coerceIn(0, lastIndex)
        dragFromIndex = -1
        dragOffsetY = 0f
        if (from != to) {
            onReorder(from, to)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(SettingItemSpacing)) {
        items.forEachIndexed { index, item ->
            key(item.id) {
                val isDragging = draggingIndex == index
                val dragActive = dragFromIndex >= 0
                val targetOffsetY = itemDragOffsetY(
                    index = index,
                    draggingIndex = draggingIndex,
                    targetIndex = targetIndex,
                    dragOffsetY = dragOffsetY,
                    stepPx = stepPx
                )
                val neighborOffsetY by animateFloatAsState(
                    targetValue = if (dragActive && !isDragging) targetOffsetY else 0f,
                    animationSpec = if (dragActive) {
                        spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    } else {
                        snap()
                    },
                    label = "neighborShift"
                )
                val displayOffsetY = when {
                    !dragActive -> 0f
                    isDragging -> targetOffsetY
                    else -> neighborOffsetY
                }

                ReorderableSettingRow(
                    label = item.label,
                    enabled = item.enabled,
                    isDragging = isDragging,
                    displayOffsetY = displayOffsetY,
                    onRowHeightMeasured = { heightPx ->
                        if (heightPx > rowHeightPx) {
                            rowHeightPx = heightPx
                        }
                    },
                    onEnabledChange = { enabled ->
                        val enabledCount = items.count { it.enabled }
                        if (!enabled && enabledCount <= minEnabledCount) return@ReorderableSettingRow
                        onEnabledChange(index, enabled)
                    },
                    onDragStart = {
                        dragFromIndex = index
                        dragOffsetY = 0f
                    },
                    onDrag = { deltaY ->
                        dragOffsetY += deltaY
                    },
                    onDragEnd = { finishDrag() },
                    itemKey = item.id
                )
            }
        }
    }
}

private fun itemDragOffsetY(
    index: Int,
    draggingIndex: Int,
    targetIndex: Int,
    dragOffsetY: Float,
    stepPx: Float
): Float {
    if (draggingIndex < 0) return 0f
    if (index == draggingIndex) return dragOffsetY
    return when {
        draggingIndex < targetIndex && index in (draggingIndex + 1)..targetIndex -> -stepPx
        targetIndex < draggingIndex && index in targetIndex until draggingIndex -> stepPx
        else -> 0f
    }
}

@Composable
private fun ReorderableSettingRow(
    label: String,
    enabled: Boolean,
    isDragging: Boolean,
    displayOffsetY: Float,
    itemKey: String,
    onRowHeightMeasured: (Float) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onRowHeightMeasured(it.height.toFloat()) }
            .graphicsLayer {
                translationY = displayOffsetY
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(itemKey) {
                detectDragGestures(
                    onDragStart = { currentOnDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount.y)
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = if (isDragging) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isDragging) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        shadowElevation = if (isDragging) 6.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDragging) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}
