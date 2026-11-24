package com.yid.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bluesky's check after the name of a verified account (a post's author, a profile): the one mark of a verified account
 * and of a trusted verifier.
 * https://github.com/bluesky-social/social-app/blob/main/src/components/verification/VerificationCheck.tsx#VerificationCheck
 * not ported: VerifierCheck and the `verifier` prop (Bluesky draws a trusted verifier's own scalloped seal; here it shows the same mark)
 * not ported: Props (Bluesky's icon props: fill, style, gradient; the size in dp is the one prop here)
 */
@Composable
fun VerificationCheck(modifier: Modifier = Modifier, size: Dp = 16.dp) = VerifiedCheck(modifier, size)

/** An element of an icon, as createSVG takes them: a circle or a path, with its fill and no stroke. */
private sealed interface Element {
    class Circle(val fill: Color, val cx: Float, val cy: Float, val r: Float) : Element
    class Pathway(val fill: Color, val d: Path) : Element
}

/**
 * VerifiedCheck: a circle (cx 12, cy 12, r 11.5) and a check path (even odd), on a 24 unit viewBox. Bluesky draws the
 * circle in currentColor and the check in white; here the circle is the phone's primary and the check its onPrimary,
 * never a fixed colour. The check is the account's "Verified" for TalkBack. Drawn with Compose's Canvas and
 * DrawScope's drawCircle and drawPath.
 * https://github.com/bluesky-social/social-app/blob/main/src/components/icons/brands/VerifiedCheck.tsx#VerifiedCheck
 * https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Canvas.kt#Canvas
 * https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-graphics/src/commonMain/kotlin/androidx/compose/ui/graphics/drawscope/DrawScope.kt#drawCircle
 * https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui-graphics/src/commonMain/kotlin/androidx/compose/ui/graphics/drawscope/DrawScope.kt#drawPath
 * not ported: createSVG (the icon template: the Canvas draws the elements, there is no svg to build)
 * not ported: canvas (androidx's own Canvas is called, not copied)
 * not ported: drawCircle (androidx's own DrawScope function is called, not copied)
 * not ported: drawPath (androidx's own DrawScope function is called, not copied)
 */
@Composable
fun VerifiedCheck(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val currentColor = MaterialTheme.colorScheme.primary
    val white = MaterialTheme.colorScheme.onPrimary
    val elements = listOf(
        Element.Circle(fill = currentColor, cx = 12f, cy = 12f, r = 11.5f),
        Element.Pathway(
            fill = white,
            d = PathParser().parsePathString(
                "M17.659 8.175a1.36 1.36 0 0 1 0 1.925l-6.224 6.223a1.36 1.36 0 0 1-1.925 0L6.4 13.212a1.361 1.361 0 0 1 " +
                    "1.925-1.925l2.149 2.148 5.26-5.26a1.36 1.36 0 0 1 1.925 0Z"
            ).toPath().apply { fillType = PathFillType.EvenOdd }
        )
    )
    Canvas(modifier.size(size).semantics { contentDescription = "Verified" }) {
        scale(this.size.width / 24f, Offset.Zero) {
            for (element in elements) {
                when (element) {
                    is Element.Circle -> drawCircle(element.fill, radius = element.r, center = Offset(element.cx, element.cy))
                    is Element.Pathway -> drawPath(element.d, element.fill)
                }
            }
        }
    }
}
