package com.yid.app.screenshot

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.ui.component.LoadingMark
import com.yid.app.ui.component.SetupTile
import com.yid.app.ui.component.SetupTileGrid
import com.yid.app.ui.component.SyncFrequency
import com.yid.app.ui.icon.YidIcons
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DRAFT for the user's yes (requests.md points 6, 7 and 8, MTGA's points 14, 26, 45 and 46), nothing of app/src/main
 * changes: the app's opening screen. The guide's block (mark, "Bluesky reader", the three tiles, Start) centred in the
 * window's height, Start as an 80 dp cookie in the phone's primary that morphs to a circle when pressed, 40 dp under the
 * tiles, nothing under it, no "Still needed" line, every refusal on its own tile (error flag). The tiles are Save media (never
 * required), New posts, and the new Refresh at launch (the Settings tile's icon and "1 h", a preset so done, not required),
 * their state in the name's size (TileFace, point 8). Frames: rest, pressed (progress 1, scale .86, a quarter turn, the
 * arrow upright), refused (notifications blocked on their tile in the error colour), and guide_refresh_draft_dialog: the
 * dialog the Refresh tile opens (the Settings one, LaunchRefreshDraftScreenshotTest's RefreshDialogPane) over the opening
 * screen, at 1 h.
 * Layout ported from https://github.com/SvenTiigi/WhatsNewKit/blob/main/Sources/View/WhatsNewView.swift#body
 * through LinkedOut's
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/WelcomeOpeningDraftScreenshotTest.kt#WelcomeOpeningDraftScreenshotTest
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/GuideRefreshDraftScreenshotTest.kt#GuideRefreshDraftScreenshotTest
 * The cookie's clip is Android's own recipe
 * https://github.com/android/snippets/blob/main/compose/snippets/src/main/java/com/example/compose/snippets/graphics/ShapesSnippets.kt#MorphPolygonShape
 * The stops are Feeder's SyncFrequency, the app's own (ui/component/LaunchRefresh.kt):
 * https://github.com/spacecowboy/Feeder/blob/master/app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt#SyncFrequency
 *
 * Yiḍ's LoadingMark draws on Robolectric, so [MarkStandIn] is the real mark, held at one frame (not running, the star lit by
 * a progress of .6) at 160 dp: it shows where the running mark sits and keeps the picture the same on every run.
 * not ported: WhatsNewView's footer band, secondary action sheet, version store, title and feature helper views (the app's
 * line and SetupTileGrid take their place); MTGA's WelcomeGuideDraft pinned footer and its 88 dp bottom reserve (Start ends
 * the block here); MTGA's MorphShape and CookieStartDraft, which cite Voice's MorphShape.kt and PlayButton.kt (GPL-3.0,
 * Yiḍ is MIT: MorphPolygonShape and a plain clickable Box take their place, no haptic in a picture); MTGA's springs, pressed
 * state and turns counter; LinkedOut's Updates and LinkedIn links tiles (Yiḍ has no Updates tile, Store installs it, and its
 * links are Settings'), its refusedLate (Yiḍ's refusal is the blocked notifications), its tilesRefused media and updates
 * refusals; GuideRefreshDraftScreenshotTest's draft and edge (the opening screen with the Refresh tile is welcome_opening_draft
 * and its edges), its GuideMarkStandIn, GuideContentDraft (today's guide layout, replaced by this opening screen) and its
 * "LinkedIn reader" line in bodyLarge
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class WelcomeOpeningDraftScreenshotTest : ScreenshotBase() {

    private fun tile(
        id: String, name: String, icon: ImageVector, state: String, summary: String = "",
        done: Boolean, error: Boolean = false, onClick: () -> Unit = {}
    ) = SetupTile(
        id = id, name = name, title = name, icon = icon, state = state, summary = summary, done = done,
        required = false, error = error, onClick = onClick
    )

    private val refreshSummary =
        "When the app opens, an account is read again once its last read is older than this. Tap to choose."

    /** The first run as the guide will draw it, nothing refused. */
    private val tilesRest = listOf(
        tile("media", "Save media", YidIcons.Download, "Never", done = true),
        tile("notify", "New posts", YidIcons.Bell, "Off", done = true),
        tile("refresh", "Refresh at launch", DraftIcons.Timer, SyncFrequency.EVERY_1_HOURS.tile, refreshSummary, done = true)
    )

    /**
     * What the phone refused: the refusal is the tile's error colour and its state, no message line. The blocked New posts
     * tile's tap opens Android's notification page of the app (nothing runs in a picture: the tap is empty here).
     */
    private fun tilesRefused(notifyState: String) = listOf(
        tile("media", "Save media", YidIcons.Download, "On Wi-Fi only", done = true),
        tile("notify", "New posts", YidIcons.Bell, notifyState, done = false, error = true),
        tile("refresh", "Refresh at launch", DraftIcons.Timer, SyncFrequency.EVERY_1_HOURS.tile, refreshSummary, done = true)
    )

    @Test
    fun rest() = capture("welcome_opening_draft") { WelcomeOpeningDraft(tilesRest, onStart = {}) }

    @Test
    fun pressed() = capture("welcome_opening_pressed_draft") {
        WelcomeOpeningDraft(tilesRest, onStart = {}, morphProgress = 1f, scale = 0.86f, rotation = 90f)
    }

    @Test
    fun refused() = capture("welcome_opening_refused_draft") {
        WelcomeOpeningDraft(tilesRefused("Notifications blocked"), onStart = {})
    }

    // The dialog the Refresh tile opens, over the opening screen (GuideRefreshDraftScreenshotTest#dialog).
    @Test
    fun guideDialog() = capture("guide_refresh_draft_dialog") { Guide(open = true) }

    // Edge: the rest frame at 200 % text and on a 360 dp phone (ScreenshotBase#captureEdge).
    @Test
    fun edge() = captureEdge("welcome_opening_draft") { WelcomeOpeningDraft(tilesRest, onStart = {}) }

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/GuideRefreshDraftScreenshotTest.kt#Guide
    @Composable
    private fun Guide(open: Boolean) {
        Box(Modifier.fillMaxSize()) {
            WelcomeOpeningDraft(tilesRest, onStart = {})
            if (open) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)), contentAlignment = Alignment.Center) {
                    RefreshDialogPane(SyncFrequency.EVERY_1_HOURS)
                }
            }
        }
    }
}

/**
 * FabMediumTokens: ContainerHeight 80 dp, IconSize 28 dp (the container is as wide as it is high).
 * https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/FabMediumTokens.kt#FabMediumTokens
 * not ported: ContainerShape (the cookie here), ContainerWidth, container colours, focus, hover, pressed and icon
 * colours, elevations (a draft).
 */
private val FabMediumContainerSize: Dp = 80.dp
private val FabMediumIconSize: Dp = 28.dp

/**
 * arrow_forward, Material Symbols Rounded 24px: viewport 960, mirrored in right-to-left (autoMirror) as the app's icons
 * are.
 * https://github.com/google/material-design-icons/blob/master/symbols/android/arrow_forward/materialsymbolsrounded/arrow_forward_24px.xml#arrow_forward
 * not ported: the tint attribute (the icon takes the colour of its use).
 */
private val ArrowForward: ImageVector by lazy {
    ImageVector.Builder(
        name = "ArrowForward",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = true
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M647,520L200,520Q183,520 171.5,508.5Q160,497 160,480Q160,463 171.5,451.5Q183,440 200,440L647,440" +
                    "L451,244Q439,232 439.5,216Q440,200 452,188Q464,177 480,176.5Q496,176 508,188L772,452" +
                    "Q778,458 780.5,465Q783,472 783,480Q783,488 780.5,495Q778,502 772,508L508,772" +
                    "Q497,783 480.5,783Q464,783 452,772Q440,760 440,743.5Q440,727 452,715L647,520Z"
            ).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }.build()
}

/**
 * Material 3 Expressive's Cookie9Sided: a nine pointed star, inner radius .8, rounding .5, turned -90 degrees. Left
 * at radius 1 around (0, 0), not normalized, which is what [MorphPolygonShape] assumes. Built from graphics-shapes
 * (material3 1.4.0 has no MaterialShapes yet).
 * https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MaterialShapes.kt#cookie9
 * not ported: the other shapes of MaterialShapes and customPolygon (only the cookie and the circle are drawn), the
 * normalization of the getter (the shape is stretched by MorphPolygonShape instead).
 */
private val Cookie9Sided: RoundedPolygon by lazy {
    RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = .8f, rounding = CornerRounding(radius = .5f))
        .transformed { x, y -> TransformResult(y, -x) }
}

/**
 * MaterialShapes' circle, ten vertices as its default, radius 1 around (0, 0).
 * https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MaterialShapes.kt#circle
 */
private val CircleShape: RoundedPolygon by lazy { RoundedPolygon.circle(numVertices = 10) }

/**
 * A morph between two polygons as a clip shape at a progress.
 * https://github.com/android/snippets/blob/main/compose/snippets/src/main/java/com/example/compose/snippets/graphics/ShapesSnippets.kt#MorphPolygonShape
 * Changed: the matrix is reset at each outline (the snippet's field would add up over redraws).
 */
private class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float
) : Shape {

    private val matrix = Matrix()
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // Below assumes that you haven't changed the default radius of 1f, nor the centerX and centerY of 0f
        // By default this stretches the path to the size of the container, if you don't want stretching, use the same size.width for both x and y.
        matrix.reset()
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)

        val path = morph.toPath(progress = percentage).asComposePath()
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

/**
 * Start: an 80 dp cookie that morphs to the circle at `morphProgress`, shrinks to `scale` and turns by `rotation`, the
 * arrow turning back against it so it stays upright. Fixed values here (the picture is still). A clickable Box with a
 * ripple, not a Material control. Shared with StartRevealDraftScreenshotTest, which puts it under the reveal's hole.
 * Idea of the press (sink, quarter turn, circle) from LinkedOut's
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/WelcomeOpeningDraftScreenshotTest.kt#CookieStartDraft
 * not ported: the animations (springs, pressed state, turns counter), the haptic, the size parameter
 * (FabMediumContainerSize).
 */
@Composable
internal fun CookieStartDraft(
    onClick: () -> Unit,
    description: String,
    morphProgress: Float,
    scale: Float,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val morph = remember { Morph(Cookie9Sided, CircleShape) }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(FabMediumContainerSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .clip(MorphPolygonShape(morph, morphProgress.coerceIn(0f, 1f)))
            .background(colors.primary)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.onPrimary),
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberVectorPainter(ArrowForward),
            contentDescription = description,
            colorFilter = ColorFilter.tint(colors.onPrimary),
            modifier = Modifier.size(FabMediumIconSize).graphicsLayer { rotationZ = -rotation }
        )
    }
}

// Yiḍ's own mark at 160 dp: the real LoadingMark, held at one frame (see the class doc).
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/WelcomeOpeningDraftScreenshotTest.kt#MarkStandIn
@Composable
internal fun MarkStandIn() {
    LoadingMark(size = 160.dp, running = false, progress = { 0.6f })
}

/**
 * WhatsNewView's body without the pinned footer: the button ends the content block, which is centred when shorter
 * than the screen, with no bottom reserve.
 * https://github.com/SvenTiigi/WhatsNewKit/blob/main/Sources/View/WhatsNewView.swift#body
 */
@Composable
private fun WelcomeOpeningDraft(
    tiles: List<SetupTile>,
    onStart: () -> Unit,
    morphProgress: Float = 0f,
    scale: Float = 1f,
    rotation: Float = 0f
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screen = maxHeight
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .widthIn(max = 720.dp).fillMaxWidth().heightIn(min = screen)
                    .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 8.dp)
            ) {
                MarkStandIn()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Bluesky reader",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(24.dp))
                SetupTileGrid(tiles) {}
                Spacer(Modifier.height(40.dp))
                CookieStartDraft(
                    onClick = onStart, description = "Start",
                    morphProgress = morphProgress, scale = scale, rotation = rotation
                )
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}
