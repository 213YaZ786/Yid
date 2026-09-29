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
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yid.app.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
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
) {
    val bat = ImageBitmap.imageResource(R.drawable.yid_mark_bat)
    val sheen = ImageBitmap.imageResource(R.drawable.yid_mark_sheen)
    val crescent = ImageBitmap.imageResource(R.drawable.yid_mark_crescent)
    val shadow = ImageBitmap.imageResource(R.drawable.yid_mark_shadow)
    val star = ImageBitmap.imageResource(R.drawable.yid_mark_star)
    val scheme = MaterialTheme.colorScheme
    val darkPage = scheme.background.luminance() < 0.5f
    val colour = remember(scheme.primary, darkPage) { batColour(scheme.primary, if (darkPage) DARK_PAGE_LIGHTNESS else BODY_LIGHTNESS) }
    val tint = remember(colour) { ColorFilter.tint(colour, BlendMode.Modulate) }

    val transition = rememberInfiniteTransition(label = "twinkling star")
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(TWINKLE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle"
    )

    Canvas(modifier.size(size)) {
        val side = this.size.minDimension
        val box = IntSize(side.roundToInt(), side.roundToInt())
        drawImage(shadow, dstSize = box, filterQuality = FilterQuality.Medium)
        drawImage(crescent, dstSize = box, filterQuality = FilterQuality.Medium)
        drawImage(bat, dstSize = box, colorFilter = tint, filterQuality = FilterQuality.Medium)
        drawImage(sheen, dstSize = box, filterQuality = FilterQuality.Medium)
        STARS.forEachIndexed { i, (x, y, reach) ->
            val glow = if (running) glowAt(twinkle, i) else progress.coerceIn(0f, 1f)
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
 * The bat's colour for this theme: the hue of [primary], at [lightness] in
 * OKLab, with the given violet's vividness, the same share of the most
 * vivid colour that hue allows at that lightness.
 */
private fun batColour(primary: Color, lightness: Float): Color {
    val (_, a, b) = toOklab(primary)
    val hue = atan2(b, a)
    var lo = 0f
    var hi = 0.4f
    repeat(24) {
        val mid = (lo + hi) / 2f
        if (inGamut(lightness, mid, hue)) lo = mid else hi = mid
    }
    return fromOklch(lightness, VIVIDNESS * lo, hue)
}

private fun toOklab(c: Color): Triple<Float, Float, Float> {
    fun lin(v: Float) = if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    val r = lin(c.red); val g = lin(c.green); val b = lin(c.blue)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    return Triple(
        0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
        1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
        0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
    )
}

private fun linear(lightness: Float, chroma: Float, hue: Float): FloatArray {
    val a = chroma * cos(hue); val b = chroma * sin(hue)
    val l = (lightness + 0.3963377774f * a + 0.2158037573f * b).pow(3)
    val m = (lightness - 0.1055613458f * a - 0.0638541728f * b).pow(3)
    val s = (lightness - 0.0894841775f * a - 1.2914855480f * b).pow(3)
    return floatArrayOf(
        4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
        -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
        -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    )
}

private fun inGamut(lightness: Float, chroma: Float, hue: Float) =
    linear(lightness, chroma, hue).all { it in -0.0001f..1.0001f }

private fun fromOklch(lightness: Float, chroma: Float, hue: Float): Color {
    fun srgb(v: Float) = v.coerceIn(0f, 1f).let { if (it <= 0.0031308f) 12.92f * it else 1.055f * it.pow(1f / 2.4f) - 0.055f }
    val (r, g, b) = linear(lightness, chroma, hue).map(::srgb)
    return Color(r, g, b)
}

/**
 * The star of the icon, on its 108 dp grid: centre and reach, halfway
 * between the crescent's top and the ears' tips. The mark shows SPAN dp of
 * the grid from FROM.
 */
private val STARS = listOf(Triple(54f, 55.84f, 5f))
private const val FROM = 20f
private const val SPAN = 70f
private const val STAR_REACH = 5f
private const val STAR_SIDE = 16f
private val RAY = Color(0xFFFFFAE6)

/** The body's lightness, and a lighter one for a dark page, in OKLab. */
private const val BODY_LIGHTNESS = 0.32f
private const val DARK_PAGE_LIGHTNESS = 0.52f
/** How vivid the given dark violet #341539 is, as a share of the most vivid colour of its hue and lightness. */
private const val VIVIDNESS = 0.61f
private const val TWINKLE_MILLIS = 2600
