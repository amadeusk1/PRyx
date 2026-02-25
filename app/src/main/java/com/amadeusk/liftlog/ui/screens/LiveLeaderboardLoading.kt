package com.amadeusk.liftlog.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

// Jagged "leaderboard loading" curve – ranks climbing
private val LOADING_GRAPH_VALUES = listOf(
    5.0, 4.0, 3.0, 4.0, 3.0, 2.0, 3.0, 2.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0
)

@Composable
fun LiveLeaderboardLoading(modifier: Modifier = Modifier) {
    var lineVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { lineVisible = true }

    val lineProgress by animateFloatAsState(
        targetValue = if (lineVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1400, delayMillis = 280),
        label = "leaderboardLineProgress"
    )

    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Live Leaderboard",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loading board…",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val plotLeft = 24f
                    val plotTop = 16f
                    val plotRight = size.width - 24f
                    val plotBottom = size.height - 16f
                    val plotW = (plotRight - plotLeft).coerceAtLeast(1f)
                    val plotH = (plotBottom - plotTop).coerceAtLeast(1f)

                    val values = LOADING_GRAPH_VALUES
                    val minV = values.minOrNull() ?: 0.0
                    val maxV = values.maxOrNull() ?: 1.0
                    val range = (maxV - minV).takeIf { it != 0.0 } ?: 1.0
                    val paddedMin = minV - range * 0.05
                    val paddedMax = maxV + range * 0.05
                    val paddedRange = (paddedMax - paddedMin).takeIf { it != 0.0 } ?: 1.0

                    val stepX = plotW / (values.size - 1).coerceAtLeast(1)
                    val pts = values.mapIndexed { i, v ->
                        val t = ((v - paddedMin) / paddedRange).toFloat()
                        val x = plotLeft + stepX * i
                        val y = plotTop + t * plotH
                        Offset(x, y)
                    }

                    val cumLen = FloatArray(pts.size).apply {
                        this[0] = 0f
                        for (i in 1 until size) {
                            this[i] = this[i - 1] + hypot(
                                pts[i].x - pts[i - 1].x,
                                pts[i].y - pts[i - 1].y
                            )
                        }
                    }
                    val totalLen = cumLen.lastOrNull() ?: 1f
                    val targetDist = (lineProgress * totalLen).coerceIn(0f, totalLen)

                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        var currentLen = 0f
                        for (i in 1 until pts.size) {
                            val segLen = hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
                            if (currentLen + segLen <= targetDist) {
                                lineTo(pts[i].x, pts[i].y)
                                currentLen += segLen
                            } else {
                                val frac = if (segLen > 0) (targetDist - currentLen) / segLen else 1f
                                lineTo(
                                    pts[i - 1].x + (pts[i].x - pts[i - 1].x) * frac,
                                    pts[i - 1].y + (pts[i].y - pts[i - 1].y) * frac
                                )
                                break
                            }
                        }
                    }

                    drawPath(
                        path = path,
                        color = colors.primary,
                        style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    pts.forEachIndexed { i, p ->
                        val pointRevealed = totalLen <= 0 || (lineProgress * totalLen >= cumLen[i])
                        val pointAlpha = if (pointRevealed) 1f else 0.3f
                        drawCircle(
                            color = colors.primary.copy(alpha = pointAlpha),
                            radius = 6f,
                            center = p
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
