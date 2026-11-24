package com.yid.app.screenshot

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.model.FollowedAccount
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.BarTitle
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.BoldIconButton
import com.yid.app.ui.component.FloatingAction
import com.yid.app.ui.component.FloatingTopBar
import com.yid.app.ui.component.TopActionSize
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.ui.glass.LocalGlass
import com.yid.app.ui.glass.LocalGlassBackdrop
import com.yid.app.ui.glass.glassSource
import com.yid.app.ui.glass.rememberGlassBackdrop
import com.yid.app.ui.icon.YidIcons
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * DRAFT for the user's yes (requests.md point 13, MTGA's point 40 and LinkedOut's point 24), nothing of app/src/main
 * changes: the Folders page without its capsule and its back arrow, the title "Folders" alone, bold and centred on the
 * page, scrolling with the list, and the + as the glass FloatingAction, bottom right, 16 dp from the edges, movable, its
 * place kept. Ported from LinkedOut's draft (validated by the user), itself MTGA's:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/FoldersFloatingAddDraftScreenshotTest.kt#FoldersFloatingAddDraftScreenshotTest
 * Four frames: folders_floating_add_draft_today (capsule with back, title and +), folders_floating_add_draft_rest
 * (proposed, bottom right), folders_floating_add_draft_lifted (being dragged: a little larger, a soft shadow, mid
 * screen), folders_floating_add_draft_snapped (released on the left half: at the left edge, same height), and the
 * edge pictures of the proposed page (200 % text, 360 dp phone).
 * The snap is PipSnapAlgorithm's, the closest edge of the movement bounds:
 * https://github.com/LineageOS/android_frameworks_base/blob/lineage-24.0/libs/WindowManager/Shell/src/com/android/wm/shell/common/pip/PipSnapAlgorithm.java#snapRectToClosestEdge
 * and the drag is Compose's draggable sample, a state moved by the finger's delta, the offset read by the child:
 * https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/samples/src/main/java/androidx/compose/foundation/samples/DraggableSamples.kt#DraggableSample
 * The page is FoldersScreen's (feature/accounts/FoldersScreen.kt: its LazyColumn of folder zones, drawn closed: a local
 * copy, FolderZone is private there).
 * not ported: PipSnapAlgorithm's getSnapFraction and applySnapFraction (the place kept as a fraction: the draft draws no
 * saved state; the app would write it in settings), the stash types' use (STASH_TYPE_NONE only, no stashing), the
 * rest of the class (its constructor, movement bounds from the display, the snap animation);
 * the sample's seekbar track and its red square (the glass + takes their place)
 * not ported: LinkedOut's BarAction (feature package of LinkedOut; Yiḍ's bar draws a transparent Surface target)
 * Robolectric draws no gesture: the four frames are placed by the same code a release would run, not dragged.
 * The real app would snap to the left or right edge only; the draft keeps the algorithm whole (four edges).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class FoldersFloatingAddDraftScreenshotTest : ScreenshotBase() {

    private val folders = listOf(
        FollowedAccount.MAIN to 4,
        "Friends" to 3,
        "Writers" to 1,
        "Maintainers" to 2
    )

    // FoldersScreen.kt today: the capsule with back, the title and the + over the list.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/FoldersFloatingAddDraftScreenshotTest.kt#today
    @Test
    fun today() = capture("folders_floating_add_draft_today") {
        Column(Modifier.fillMaxSize()) {
            FloatingTopBar(
                title = { Text("Folders") },
                navigationIcon = { BarAction(YidIcons.ArrowBack, "Back", {}) },
                actions = { BarAction(YidIcons.Add, "New folder", {}) }
            )
            FolderList(withTitle = false)
        }
    }

    @Test
    fun rest() = capture("folders_floating_add_draft_rest") { Page(Frame.Rest) }

    @Test
    fun lifted() = capture("folders_floating_add_draft_lifted") { Page(Frame.Lifted) }

    @Test
    fun snapped() = capture("folders_floating_add_draft_snapped") { Page(Frame.Snapped) }

    // Edge: the proposed page at rest, at 200 % text and on a 360 dp phone.
    @Test
    fun edge() = captureEdge("folders_floating_add_draft") { Page(Frame.Rest) }

    private enum class Frame { Rest, Lifted, Snapped }

    // The page: the list recorded as the glass backdrop, the + floating over it as a sibling.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/FoldersFloatingAddDraftScreenshotTest.kt#Page
    @Composable
    private fun Page(frame: Frame) {
        val look = LocalGlass.current
        val backdrop = rememberGlassBackdrop()
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop.takeIf { look != null }) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().then(if (look != null) Modifier.glassSource(backdrop, look) else Modifier)) {
                    FolderList(withTitle = true)
                }
                MovableAdd(frame)
            }
        }
    }

    // The list with the title as its first item: bold, centred, on the page with no zone, scrolling away with the list.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/FoldersFloatingAddDraftScreenshotTest.kt#FolderList
    @Composable
    private fun FolderList(withTitle: Boolean) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp + LocalReadableInset.current,
                end = 16.dp + LocalReadableInset.current,
                top = if (withTitle) 0.dp else 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (withTitle) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() } + 16.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) { BarTitle("Folders") }
                    }
                }
            }
            items(folders, key = { it.first }) { (name, count) -> FolderZone(name, count) }
        }
    }

    // A faithful copy of FoldersScreen's FolderZone, closed (its Open button, Rename and the bin for a folder that is not Main).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/FoldersFloatingAddDraftScreenshotTest.kt#FolderZone
    @Composable
    private fun FolderZone(name: String, count: Int) {
        ZoneSurface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when (count) {
                                0 -> "Empty"
                                1 -> "1 account"
                                else -> "$count accounts"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (name != FollowedAccount.MAIN) {
                        BoldButton(onClick = {}) { Text("Rename") }
                        BoldIconButton(onClick = {}) {
                            Icon(YidIcons.Delete, contentDescription = "Delete $name", modifier = Modifier.size(20.dp))
                        }
                    }
                    BoldButton(onClick = {}) { Text("Open") }
                }
            }
        }
    }

    // The bar's round targets: a transparent Surface of 48 dp, what IconButton is (the guard refuses it in app code).
    @Composable
    private fun BarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
        Surface(onClick = onClick, shape = CircleShape, color = Color.Transparent) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            }
        }
    }

    // The glass + that the finger moves: DraggableSample's state and draggable (both axes, two modifiers), released at
    // the closest edge. [frame] places it as a rest, a lift or a release would.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/FoldersFloatingAddDraftScreenshotTest.kt#MovableAdd
    @Composable
    private fun MovableAdd(frame: Frame) {
        val density = LocalDensity.current
        var container by remember { mutableStateOf(IntSize.Zero) }
        val offsetPosition = remember { mutableStateOf(Offset.Zero) }
        var lifted by remember { mutableStateOf(frame == Frame.Lifted) }
        val buttonPx = with(density) { TopActionSize.roundToPx() }
        val margin = with(density) { 16.dp.roundToPx() }
        val top = WindowInsets.statusBars.getTop(density)
        val bottom = WindowInsets.navigationBars.getBottom(density)
        // The positions the top left of the button may take: 16 dp from every edge, the bars apart.
        val movementBounds = IntRect(
            left = margin,
            top = top + margin,
            right = max(margin, container.width - margin - buttonPx),
            bottom = max(top + margin, container.height - bottom - margin - buttonPx)
        )
        val minPx = Offset(movementBounds.left.toFloat(), movementBounds.top.toFloat())
        val maxPx = Offset(movementBounds.right.toFloat(), movementBounds.bottom.toFloat())
        LaunchedEffect(container, frame) {
            if (container == IntSize.Zero) return@LaunchedEffect
            val held = IntRect(
                IntOffset((container.width * 0.22f).roundToInt(), (container.height * 0.5f).roundToInt()),
                IntSize(buttonPx, buttonPx)
            )
            offsetPosition.value = when (frame) {
                Frame.Rest -> Offset(maxPx.x, maxPx.y)
                Frame.Lifted -> Offset(held.left.toFloat(), held.top.toFloat())
                Frame.Snapped -> {
                    val out = snapRectToClosestEdge(held, movementBounds, STASH_TYPE_NONE)
                    Offset(out.left.toFloat(), out.top.toFloat())
                }
            }
        }
        Box(Modifier.fillMaxSize().onSizeChanged { container = it }) {
            if (container != IntSize.Zero) {
                Box(
                    Modifier
                        .offset { IntOffset(offsetPosition.value.x.roundToInt(), offsetPosition.value.y.roundToInt()) }
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                lifted = true
                                val newValue = offsetPosition.value.x + delta
                                offsetPosition.value = offsetPosition.value.copy(x = newValue.coerceIn(minPx.x, maxPx.x))
                            },
                            onDragStopped = {
                                lifted = false
                                val held = IntRect(
                                    IntOffset(offsetPosition.value.x.roundToInt(), offsetPosition.value.y.roundToInt()),
                                    IntSize(buttonPx, buttonPx)
                                )
                                val out = snapRectToClosestEdge(held, movementBounds, STASH_TYPE_NONE)
                                offsetPosition.value = Offset(out.left.toFloat(), out.top.toFloat())
                            }
                        )
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                lifted = true
                                val newValue = offsetPosition.value.y + delta
                                offsetPosition.value = offsetPosition.value.copy(y = newValue.coerceIn(minPx.y, maxPx.y))
                            }
                        )
                        // Lifted: a little larger, a soft shadow.
                        .graphicsLayer {
                            val scale = if (lifted) 1.12f else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .shadow(if (lifted) 10.dp else 0.dp, CircleShape)
                ) {
                    FloatingAction(icon = YidIcons.Add, label = "New folder", onClick = {})
                }
            }
        }
    }

    private val STASH_TYPE_NONE = 0
    private val STASH_TYPE_LEFT = 1
    private val STASH_TYPE_RIGHT = 2

    /**
     * Snaps the stackBounds to the closest edge of the movementBounds and returns the new bounds (boundsOut).
     * https://github.com/LineageOS/android_frameworks_base/blob/lineage-24.0/libs/WindowManager/Shell/src/com/android/wm/shell/common/pip/PipSnapAlgorithm.java#snapRectToClosestEdge
     */
    private fun snapRectToClosestEdge(stackBounds: IntRect, movementBounds: IntRect, stashType: Int): IntRect {
        var leftEdge = stackBounds.left
        if (stashType == STASH_TYPE_LEFT) {
            leftEdge = movementBounds.left
        } else if (stashType == STASH_TYPE_RIGHT) {
            leftEdge = movementBounds.right
        }
        val boundedLeft = max(movementBounds.left, min(movementBounds.right, leftEdge))
        val boundedTop = max(movementBounds.top, min(movementBounds.bottom, stackBounds.top))
        var boundsOut = stackBounds

        // Otherwise, just find the closest edge
        val fromLeft = abs(leftEdge - movementBounds.left)
        val fromTop = abs(stackBounds.top - movementBounds.top)
        val fromRight = abs(movementBounds.right - leftEdge)
        val fromBottom = abs(movementBounds.bottom - stackBounds.top)
        val shortest = min(min(fromLeft, fromRight), min(fromTop, fromBottom))
        boundsOut = if (shortest == fromLeft) {
            IntRect(IntOffset(movementBounds.left, boundedTop), stackBounds.size)
        } else if (shortest == fromTop) {
            IntRect(IntOffset(boundedLeft, movementBounds.top), stackBounds.size)
        } else if (shortest == fromRight) {
            IntRect(IntOffset(movementBounds.right, boundedTop), stackBounds.size)
        } else {
            IntRect(IntOffset(boundedLeft, movementBounds.bottom), stackBounds.size)
        }
        return boundsOut
    }
}
