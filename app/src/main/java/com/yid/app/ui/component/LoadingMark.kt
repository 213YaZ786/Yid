package com.yid.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yid.app.R
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Yiḍ's loading mark, the launcher icon at rest: the bat hangs still under
 * its gold crescent and only the gold star on its wings twinkles, a glint
 * crossing it at its brightest.
 *
 * The bat is a grey bitmap in soft relief multiplied by its colour: the
 * wallpaper's accent, as deep and as vivid as the dark violet it was drawn
 * in, a lighter tone of it on a dark page so it never sinks into the page.
 *
 * [progress] from 0 to 1 lights the star as far as a gesture has gone.
 * While [running] it twinkles on its own.
 */
@Composable
fun LoadingMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    running: Boolean = true,
    progress: Float = 0f
) = LoadingMark(modifier, size, running) { progress }

/**
 * The same mark with [progress] read while drawing, so a gesture moving it does not recompose its caller.
 *
 * Ported from LinkedOut's LoadingMark, which keeps one Animatable clock (its pen's loop; the star's twinkle here):
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/ui/component/LoadingMark.kt#LoadingMark
 * not ported: STROKES, SPANS, FRAME, FRAME_CENTRE, PEN, WRITTEN, FADE_FROM, ease, the saveLayer fade and the pen's
 * Path drawing (LinkedOut's handwritten "out"; Yiḍ keeps its bat and its star, drawn as they were)
 */
@Composable
fun LoadingMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    running: Boolean = true,
    progress: () -> Float
) {
    // The clock below is the source's, the drawing after it is Yiḍ's own, unchanged.
    val bat = ImageBitmap.imageResource(R.drawable.yid_mark_bat)
    val sheen = ImageBitmap.imageResource(R.drawable.yid_mark_sheen)
    val crescent = ImageBitmap.imageResource(R.drawable.yid_mark_crescent)
    val shadow = ImageBitmap.imageResource(R.drawable.yid_mark_shadow)
    val star = ImageBitmap.imageResource(R.drawable.yid_mark_star)
    val scheme = MaterialTheme.colorScheme
    val darkPage = scheme.background.luminance() < 0.5f
    // The launcher icon's own tones of the wallpaper's accent: 700 on a
    // light page, 300 on a dark one, so the mark and the icon are one bat.
    val colour = colorResource(if (darkPage) android.R.color.system_accent1_300 else android.R.color.system_accent1_700)
    val tint = remember(colour) { ColorFilter.tint(colour, BlendMode.Modulate) }

    // One time source for both ways: the clock continues from where it stands, and a gesture's progress glides from
    // there, so the star never jumps when `running` changes. Nothing runs at rest. A gesture lights the star along
    // the first part of its turn, up to its brightest (GLOW_PEAK).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/ui/component/LoadingMark.kt#LoadingMark
    // and the Animatable remembered and moved by animateTo in a coroutine:
    // https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation-core/samples/src/main/java/androidx/compose/animation/core/samples/AnimatableSamples.kt#AnimatableAnimateToGenericsType
    // not ported: AnimatableAnimateToGenericsType's tap detection and Offset (the target is the loop's time or the pull, not a touch)
    val currentProgress by rememberUpdatedState(progress)
    val clock = remember { Animatable(if (running) 0f else currentProgress().coerceIn(0f, 1f) * GLOW_PEAK) }
    // Whether the loop has run since the gesture last drove the clock: a stop then finishes the turn first.
    val ran = remember { BooleanArray(1) }
    LaunchedEffect(running) {
        if (running) {
            ran[0] = true
            // The infinite frame clock, as InfiniteTransition runs on: a test waiting for idle does not wait for it.
            var last = withInfiniteAnimationFrameMillis { it }
            while (true) {
                val now = withInfiniteAnimationFrameMillis { it }
                val next = clock.value + (now - last).toFloat() / TWINKLE_MILLIS
                clock.snapTo(if (next >= 1f) next - 1f else next)
                last = now
            }
        } else {
            if (ran[0]) {
                // The star ends its turn at the loop's own pace (it is dark by then), so it does not cut off.
                clock.animateTo(1f, tween(((1f - clock.value) * TWINKLE_MILLIS).roundToInt(), easing = LinearEasing))
                clock.snapTo(0f)
                ran[0] = false
            }
            snapshotFlow { currentProgress().coerceIn(0f, 1f) * GLOW_PEAK }.collectLatest { clock.animateTo(it, tween(90, easing = LinearEasing)) }
        }
    }

    // The clock is read while drawing, never in composition.
    Canvas(modifier.size(size)) {
        val twinkle = clock.value
        val side = this.size.minDimension
        val box = IntSize(side.roundToInt(), side.roundToInt())
        drawImage(shadow, dstSize = box, filterQuality = FilterQuality.Medium)
        drawImage(crescent, dstSize = box, filterQuality = FilterQuality.Medium)
        drawImage(bat, dstSize = box, colorFilter = tint, filterQuality = FilterQuality.Medium)
        drawImage(sheen, dstSize = box, filterQuality = FilterQuality.Medium)
        STARS.forEachIndexed { i, (x, y, reach) ->
            val glow = glowAt(twinkle, i)
            val centre = Offset((x - FROM) / SPAN * side, (y - FROM) / SPAN * side)
            drawStar(star, centre, reach / SPAN * side, glow)
        }
    }
}

/** A star bright for the first part of its turn, then resting, each at its own moment. */
private fun glowAt(t: Float, i: Int): Float {
    val p = (t + i * 0.37f) % 1f
    return if (p < 0.625f) max(0f, sin(PI.toFloat() * p * 1.6f)) else 0f
}

/** One star of [reach] around [centre]: it grows a little and brightens, and glints at its peak. */
private fun DrawScope.drawStar(star: ImageBitmap, centre: Offset, reach: Float, glow: Float) {
    // The star bitmap holds a reach of STAR_REACH over its whole side of STAR_SIDE.
    val side = STAR_SIDE / STAR_REACH * reach * (0.86f + 0.2f * glow)
    val half = side / 2f
    val offset = IntOffset((centre.x - half).roundToInt(), (centre.y - half).roundToInt())
    val box = IntSize(side.roundToInt().coerceAtLeast(1), side.roundToInt().coerceAtLeast(1))
    drawImage(star, dstOffset = offset, dstSize = box, filterQuality = FilterQuality.Medium)
    if (glow > 0f) drawImage(star, dstOffset = offset, dstSize = box, alpha = 0.3f * glow, blendMode = BlendMode.Plus, filterQuality = FilterQuality.Medium)
    val flare = glow.pow(4)
    if (flare < 0.02f) return
    val ray = reach * 1.9f
    val thick = reach * 0.16f
    rotate(45f, centre) {
        for (turn in 0..1) rotate(90f * turn, centre) {
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Transparent, 0.5f to RAY.copy(alpha = 0.55f * flare), 1f to Color.Transparent,
                    startX = centre.x - ray, endX = centre.x + ray
                ),
                topLeft = Offset(centre.x - ray, centre.y - thick / 2f),
                size = Size(2f * ray, thick),
                blendMode = BlendMode.Plus
            )
        }
    }
    drawCircle(
        Brush.radialGradient(listOf(RAY.copy(alpha = 0.35f * flare), Color.Transparent), centre, reach * 1.6f),
        radius = reach * 1.6f, center = centre, blendMode = BlendMode.Plus
    )
}

/**
 * The star of the icon, on its 108 dp grid: centre and reach,
 * at the centre of the crescent's circle. The mark shows SPAN dp of
 * the grid from FROM.
 */
private val STARS = listOf(Triple(54f, 56.5f, 5f))
private const val FROM = 20f
private const val SPAN = 70f
private const val STAR_REACH = 5f
private const val STAR_SIDE = 16f
private val RAY = Color(0xFFFFFAE6)

private const val TWINKLE_MILLIS = 2600

/** The clock's value at which glowAt is brightest: sin(PI * p * 1.6) is 1 at p = 0.5 / 1.6. */
private const val GLOW_PEAK = 0.5f / 1.6f
