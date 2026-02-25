package com.amadeusk.liftlog.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun BenchPressLoading(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val drawColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.primary
    val infinite = rememberInfiniteTransition(label = "bench")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bar"
    )
    val barY = t

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = drawColor
            val w = size.width
            val h = size.height
            val stroke = 4f
            val benchH = h * 0.5f
            val benchW = w * 0.9f
            val benchLeft = (w - benchW) / 2f
            val benchTop = benchH

            drawRect(
                color = c.copy(alpha = 0.3f),
                topLeft = Offset(benchLeft, benchTop),
                size = Size(benchW, h * 0.12f)
            )

            val torsoW = w * 0.35f
            val torsoH = h * 0.2f
            val torsoLeft = (w - torsoW) / 2f
            val torsoTop = benchTop + 4f
            drawRect(
                color = c,
                topLeft = Offset(torsoLeft, torsoTop),
                size = Size(torsoW, torsoH)
            )

            val headR = h * 0.08f
            drawCircle(
                color = c,
                radius = headR,
                center = Offset(w / 2f, benchTop - headR - 2f)
            )

            val barUpY = h * 0.18f
            val barDownY = h * 0.42f
            val currentBarY = barUpY + (barDownY - barUpY) * barY

            val barLen = w * 0.75f
            val barLeft = (w - barLen) / 2f
            drawLine(
                color = c,
                start = Offset(barLeft, currentBarY),
                end = Offset(barLeft + barLen, currentBarY),
                strokeWidth = stroke * 1.8f,
                cap = StrokeCap.Round
            )
            drawCircle(c, radius = stroke * 1.2f, center = Offset(barLeft, currentBarY))
            drawCircle(c, radius = stroke * 1.2f, center = Offset(barLeft + barLen, currentBarY))

            val shoulderY = torsoTop + 8f
            val shoulderLeft = torsoLeft + 6f
            val shoulderRight = torsoLeft + torsoW - 6f
            drawLine(
                color = c,
                start = Offset(shoulderLeft, shoulderY),
                end = Offset(barLeft + barLen * 0.25f, currentBarY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = c,
                start = Offset(shoulderRight, shoulderY),
                end = Offset(barLeft + barLen * 0.75f, currentBarY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
