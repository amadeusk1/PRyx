package com.amadeusk.liftlog

// Canvas + touch input
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures

// Compose layouts
import androidx.compose.foundation.layout.*

// Material styling
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

// Compose runtime
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Drawing helpers
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

// Input + density
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

// Text helpers
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Math
import kotlin.math.hypot
import kotlin.math.roundToInt

// Holds a computed point on the chart (with original payload for selection)
private data class ChartPoint<T>(
    val x: Float,       // x position in pixels
    val y: Float,       // y position in pixels
    val value: Double,  // numeric value (already in display units)
    val label: String,  // x-axis label (date text, etc.)
    val payload: T      // original item for callbacks
)

// Generic line chart composable (works for PRs and bodyweight)
@Composable
fun <T> ProfessionalLineChart(
    title: String,                 // Chart title text
    items: List<T>,                // Data items (must be in display order)
    selected: T?,                  // Currently selected item (for tooltip)
    onSelected: (T) -> Unit,       // Callback when user taps a point
    getValue: (T) -> Double,       // Extract numeric y-value from item
    getLabel: (T) -> String,       // Extract x-axis label from item
    formatValue: (Double) -> String, // Format values for labels / tooltip
    modifier: Modifier = Modifier,
    showAxisLabels: Boolean = true,
    showTitle: Boolean = true
) {
    // Need at least 2 points to draw a line
    if (items.size < 2) {
        Column(modifier = modifier) {
            Text("Not enough data to draw a graph yet.", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    // Items are assumed already sorted upstream (kept as-is)
    val sorted = remember(items) { items }

    // Theme colors
    val colors = MaterialTheme.colorScheme
    val lineColor = colors.primary

    // Gridline color (subtle)
    val gridColor = colors.onSurface.copy(alpha = 0.12f)

    // Text and point colors
    val axisTextColor = colors.onSurfaceVariant
    val pointOuter = colors.surface
    val pointInner = colors.onSurface
    val selectedColor = colors.primary

    // Layout constants
    val chartHeight = 220.dp
    val leftGutter = 68.dp     // space for Y-axis labels
    val bottomGutter = 46.dp   // space for X-axis labels
    val topGutter = 14.dp
    val rightGutter = 16.dp

    // Density used to convert dp -> px for hit-testing
    val density = LocalDensity.current
    val hitRadiusPx = with(density) { 18.dp.toPx() }

    Column(modifier = modifier) {
        // Chart title
        if (showTitle && title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Card-like surface behind the chart
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Stores computed chart points (used for tooltip + tap detection)
                var chartPoints by remember { mutableStateOf<List<ChartPoint<T>>>(emptyList()) }

                // Find which point is selected (index in list)
                val selectedIndex = remember(sorted, selected) {
                    if (selected == null) -1 else sorted.indexOfFirst { it == selected }
                }

                // Tooltip bubble shown near the selected point
                if (selectedIndex in chartPoints.indices) {
                    val p = chartPoints[selectedIndex]
                    TooltipBubble(
                        text = "${formatValue(p.value)} • ${p.label}",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            // Convert px -> dp to position tooltip
                            .offset(
                                x = with(density) { (p.x / density.density).dp } - 6.dp,
                                y = with(density) { (p.y / density.density).dp } - 36.dp
                            )
                    )
                }

                // Canvas for drawing the chart
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        // Tap detection to select nearest point
                        .pointerInput(sorted) {
                            detectTapGestures { tap ->
                                val hit = chartPoints.minByOrNull { pt ->
                                    hypot(pt.x - tap.x, pt.y - tap.y)
                                }
                                if (hit != null) {
                                    val d = hypot(hit.x - tap.x, hit.y - tap.y)
                                    if (d <= hitRadiusPx) onSelected(hit.payload)
                                }
                            }
                        }
                ) {
                    // Convert items into a list of values
                    val values = sorted.map(getValue)
                    val maxV = values.maxOrNull() ?: 0.0
                    val minV = values.minOrNull() ?: 0.0

                    // Add padding so line doesn't touch the edges
                    val range = (maxV - minV).takeIf { it != 0.0 } ?: 1.0
                    val paddedMin = minV - range * 0.08
                    val paddedMax = maxV + range * 0.08
                    val paddedRange = (paddedMax - paddedMin).takeIf { it != 0.0 } ?: 1.0

                    // Define the plotting area inside the canvas
                    val plotLeft = with(density) { leftGutter.toPx() }
                    val plotTop = with(density) { topGutter.toPx() }
                    val plotRight = size.width - with(density) { rightGutter.toPx() }
                    val plotBottom = size.height - with(density) { bottomGutter.toPx() }

                    val plotW = (plotRight - plotLeft).coerceAtLeast(1f)
                    val plotH = (plotBottom - plotTop).coerceAtLeast(1f)

                    // Horizontal spacing between points
                    val stepX = plotW / (sorted.size - 1).coerceAtLeast(1)

                    // Build chart points (x,y in pixels)
                    val pts = sorted.mapIndexed { i, item ->
                        val v = getValue(item)
                        val x = plotLeft + stepX * i

                        // Normalize value into 0..1 range
                        val t = ((v - paddedMin) / paddedRange).toFloat()

                        // Flip y so larger values are higher on screen
                        val y = plotTop + (1f - t) * plotH

                        ChartPoint(
                            x = x,
                            y = y,
                            value = v,
                            label = getLabel(item),
                            payload = item
                        )
                    }

                    // Save points so taps/tooltip can use them
                    chartPoints = pts

                    // Subtle background inside plot area
                    drawRoundRect(
                        color = colors.surfaceVariant.copy(alpha = 0.25f),
                        topLeft = Offset(plotLeft, plotTop),
                        size = androidx.compose.ui.geometry.Size(plotW, plotH),
                        cornerRadius = CornerRadius(18f, 18f)
                    )

                    // --- GRIDLINES + Y LABELS ---
                    val gridLines = 5
                    for (g in 0..gridLines) {
                        val frac = g / gridLines.toFloat()
                        val y = plotTop + frac * plotH

                        // Horizontal line
                        drawLine(
                            color = gridColor,
                            start = Offset(plotLeft, y),
                            end = Offset(plotRight, y),
                            strokeWidth = 1.5f
                        )

                        if (showAxisLabels) {
                            // Value label on the left
                            val v = paddedMax - (paddedRange * frac)
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    textSize = 28f
                                    color = axisTextColor.toArgb()
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                                drawText(
                                    formatValue(v),
                                    plotLeft - 22f, // padding from plot area
                                    y + 10f,
                                    paint
                                )
                            }
                        }
                    }

                    // --- VERTICAL GRIDLINES ---
                    val vLines = 4
                    for (g in 0..vLines) {
                        val frac = g / vLines.toFloat()
                        val x = plotLeft + frac * plotW

                        drawLine(
                            color = gridColor,
                            start = Offset(x, plotTop),
                            end = Offset(x, plotBottom),
                            strokeWidth = 1.5f
                        )
                    }

                    // --- X LABELS ---
                    if (showAxisLabels) {
                        val targetLabels = 4
                        val stride = (sorted.size / targetLabels).coerceAtLeast(1)
                        val xLabelY = plotBottom + 40f // pushed down for readability

                        pts.forEachIndexed { i, p ->
                            // Label every N points, and always label the last point
                            if (i % stride == 0 || i == pts.lastIndex) {
                                drawContext.canvas.nativeCanvas.apply {
                                    val paint = android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        textSize = 26f
                                        color = axisTextColor.toArgb()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                    drawText(
                                        p.label,
                                        p.x,
                                        xLabelY,
                                        paint
                                    )
                                }
                            }
                        }
                    }

                    // Build the line path through all points
                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                    }

                    // Draw the line
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw points (highlight selected point)
                    pts.forEachIndexed { i, p ->
                        val isSelected = (selectedIndex == i)

                        // Outer ring
                        drawCircle(
                            color = pointOuter,
                            radius = if (isSelected) 12f else 10f,
                            center = Offset(p.x, p.y)
                        )

                        // Inner dot
                        drawCircle(
                            color = if (isSelected) selectedColor else pointInner,
                            radius = if (isSelected) 8f else 6f,
                            center = Offset(p.x, p.y)
                        )
                    }
                }
            }
        }
    }
}

// Small tooltip bubble shown when a chart point is selected
@Composable
private fun TooltipBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            textAlign = TextAlign.Center
        )
    }
}

// Converts a Compose Color into Android ARGB Int (for nativeCanvas Paint)
private fun androidx.compose.ui.graphics.Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )
