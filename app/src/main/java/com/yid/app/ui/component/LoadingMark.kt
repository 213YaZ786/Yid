package com.yid.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Yiḍ's loading mark, the launcher icon at night in motion: the moth
 * slowly closes and opens its wings, and two four point stars twinkle in
 * turn beside it.
 *
 * [progress] from 0 to 1 opens the wings and lights the stars as far as a
 * gesture has gone. While [running] they play on their own.
 */
@Composable
fun LoadingMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    running: Boolean = true,
    progress: Float = 0f
) {
    val moth = MaterialTheme.colorScheme.primary
    val star = lerp(moth, MaterialTheme.colorScheme.surface, 0.25f)
    val parts = remember { MothParts() }

    val transition = rememberInfiniteTransition(label = "fluttering moth")
    val beat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BEAT_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "beat"
    )
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(TWINKLE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle"
    )

    // Wings at full width when open, a third of it when closed: seen from
    // above, closing wings narrow rather than vanish.
    val open = if (running) 1f - CLOSED * ease((1f - cos(2f * PI.toFloat() * beat)) / 2f) else 1f - CLOSED * (1f - progress)
    val glows = STARS.indices.map { i ->
        if (running) sin(PI.toFloat() * ((twinkle + i / STARS.size.toFloat()) % 1f)) else progress
    }

    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 100f
        STARS.forEachIndexed { i, (x, y, reach) ->
            val glow = glows[i]
            if (glow > 0.01f) drawSparkle(Offset(x * unit, y * unit), reach * unit * (0.4f + 0.6f * glow), star.copy(alpha = glow))
        }
        // The moth sits a little low, under the stars, as in the icon.
        translate(50f * unit - 54f, 52f * unit - 48f) {
            scale(MOTH_SCALE * unit, pivot = Offset(54f, 48f)) {
                scale(open, 1f, pivot = Offset(54f, 48f)) {
                    drawPath(parts.left, moth)
                    drawPath(parts.right, moth)
                }
                drawPath(parts.body, moth)
            }
        }
    }
}

private class MothParts {
    val left = path(LEFT_WING)
    val right = path(RIGHT_WING)
    val body = path(BODY)

    private fun path(data: String): Path =
        PathParser().parsePathString(data).toPath().apply { fillType = PathFillType.EvenOdd }
}

private fun DrawScope.drawSparkle(centre: Offset, reach: Float, colour: Color) {
    val (x, y) = centre
    val sparkle = Path().apply {
        moveTo(x, y - reach)
        quadraticTo(x, y, x + reach, y)
        quadraticTo(x, y, x, y + reach)
        quadraticTo(x, y, x - reach, y)
        quadraticTo(x, y, x, y - reach)
        close()
    }
    drawPath(sparkle, colour)
}

private fun ease(t: Float) = if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).let { it * it } / 2f

// The moth of the launcher icon, wings apart from the body so they can move.
private const val LEFT_WING = "M47.69,41 C36,42.5 22,52 16,70 C26,74.5 38,74 47.8,68.5 C47.68,67.58 47.27,64.92 47.08,63 C46.89,61.08 46.75,59 46.65,57 C46.55,55 46.51,52.83 46.5,51 C46.49,49.17 46.4,47.67 46.6,46 C46.8,44.33 47.51,41.83 47.69,41 Z M30,61 a4.5,4.5 0 1 0 9,0 a4.5,4.5 0 1 0 -9,0 Z"
private const val RIGHT_WING = "M60.31,41 C72,42.5 86,52 92,70 C82,74.5 70,74 60.2,68.5 C60.32,67.58 60.73,64.92 60.92,63 C61.11,61.08 61.25,59 61.35,57 C61.45,55 61.49,52.83 61.5,51 C61.51,49.17 61.6,47.67 61.4,46 C61.2,44.33 60.49,41.83 60.31,41 Z M69,61 a4.5,4.5 0 1 0 9,0 a4.5,4.5 0 1 0 -9,0 Z"
private const val BODY = "M54,31 C55.6,31 57.8,40 59,46 C59.6,60 57.4,78 54,78 C50.6,78 48.4,60 49,46 C50.2,40 52.4,31 54,31 Z M51,32 C46,31.5 39.5,27 37.5,17 C44,19 49.5,24 51,32 Z M57,32 C62,31.5 68.5,27 70.5,17 C64,19 58.5,24 57,32 Z"

/** Where the stars twinkle and how far their points reach, in hundredths of the mark. */
private val STARS = listOf(Triple(18f, 24f, 7f), Triple(84f, 30f, 5.5f))

private const val MOTH_SCALE = 0.62f
private const val CLOSED = 0.7f
private const val BEAT_MILLIS = 1400
private const val TWINKLE_MILLIS = 1800
