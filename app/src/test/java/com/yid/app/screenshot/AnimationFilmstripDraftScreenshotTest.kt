package com.yid.app.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.yid.app.ui.component.AppBarWithSearch
import com.yid.app.ui.component.Avatar
import com.yid.app.ui.component.BannerAction
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.BoldIconButton
import com.yid.app.ui.component.FolderSwitch
import com.yid.app.ui.component.GlassTarget
import com.yid.app.ui.component.LoadingMark
import com.yid.app.ui.component.SearchCapsuleHint
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.ui.glass.LocalGlass
import com.yid.app.ui.glass.glassGround
import com.yid.app.ui.glass.glassZone
import com.yid.app.ui.glass.rememberGlassLook
import com.yid.app.ui.icon.YidIcons
import com.yid.app.ui.theme.YidTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DRAFT of requests.md point 18 (every animation smooth, the app's own part): a filmstrip per case, motion shown in a still.
 * Top row, "today": the piece with its plain swap; bottom row, "proposed": the same piece with the fix (the footer has a
 * today and a proposed row per change). Frames are taken with the test clock stopped (mainClock.autoAdvance = false),
 * advanced to each instant, then captured with captureToImage, as the compose testing guide does for animations
 * https://developer.android.com/develop/ui/compose/testing/time-manipulation
 * and joined in one bitmap saved with Roborazzi's Bitmap capture. Nothing of app/src/main changes. Pictures:
 * anim_follow_draft, anim_alert_draft, anim_footer_draft, anim_folders_draft, each _light and _dark.
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#AnimationFilmstripDraftScreenshotTest
 *
 * Ports (the sample of each mechanism the proposals use, names kept: visible, enter, exit, contentState):
 * https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedVisibilitySamples.kt#AnimatedVisibilityWithBooleanVisibleParamNoReceiver
 * (fadeIn, fadeOut + scaleOut on a `visible` flag: the alert)
 * https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedContentSamples.kt#SimpleAnimatedContentSample
 * (AnimatedContent on a state: Follow's label, the footer, Folders' count, label and check)
 * not ported: AnimatedVisibility's receiver variants and the Transition animateDp of the sample, the sample's
 * Foo/Bar/Baz contents and its pointerInput (the test drives the state instead).
 * not ported: LinkedOut's own cases like_end, holdmenu_close, newdot, zoom, video_bubble, rows, clear, state, count, pill,
 * viewer, placeholder and their functions and tests LikeBurst, HoldClose, NameRow, Photo, Zoom, Bubble, CardMini, likeEnd,
 * holdmenuClose, newdot, zoom, videoBubble, Rows, ClearField, ListContent, ListSwap, AccountLine, FiltersPill, Counts,
 * CheckPill, Viewer, PlayBlock, Placeholder, HeldPhoto, PhotoStandIn, VideoStandIn, Account (Yiḍ has no jobs, challenge pill,
 * viewer, link card or document, and points 12 to 17 take the other kit pieces; the rows, the Clear icon and the small counts
 * are the second batch on the user's word), its private PersonAdd icon (YidIcons has it), MarkStandIn and MarkStandInAt
 * (Yiḍ's LoadingMark draws on Robolectric, held at one frame), the AnimatableAnimateToGenericsType port, and its own
 * rule and glass set-up beyond what [row] needs (ScreenshotBase's rule, YidTheme and rememberGlassLook are used).
 *
 * Not driven as in the app: Follow (FollowButton of feature/feed/ProfileHeader.kt is private, its layout redrawn here; the
 * press corner morph and the click are left out), the list footers (TimelineScreen.kt TimelineFooter and FeedScreen.kt's
 * footer item, redrawn from their lines), the folder row (FoldersScreen.kt FolderZone, redrawn without its hold and rows
 * of other accounts). The alert is the app's own AppBarWithSearch, SearchCapsuleHint, FolderSwitch and BannerAction as
 * TimelineScreen's banner composes them.
 */
@OptIn(ExperimentalRoborazziApi::class, ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class AnimationFilmstripDraftScreenshotTest : ScreenshotBase() {

    private val standard = listOf(0, 50, 100, 150, 200, 300, 450)

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f)
    )

    private var pictures = 0

    // One row: the piece composed on a fixed stage, the change triggered ([go] true), a frame at each instant.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#row
    private fun row(
        night: Boolean,
        times: List<Int>,
        width: Int,
        height: Int,
        content: @Composable (go: Boolean) -> Unit
    ): List<Bitmap> {
        val go = mutableStateOf(false)
        val picture = pictures++
        compose.mainClock.autoAdvance = false
        compose.activity.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                YidTheme(darkTheme = night, dynamicColor = false) {
                    // The app's own look, quiet glass, as MainActivity and ScreenshotBase set it.
                    val look = rememberGlassLook(MaterialTheme.colorScheme, true, quiet = true)
                    CompositionLocalProvider(
                        LocalGlass provides look,
                        LocalContentColor provides MaterialTheme.colorScheme.onBackground
                    ) {
                        androidx.compose.runtime.key(picture) {
                            Box(
                                Modifier
                                    .testTag("stage")
                                    .size(width.dp, height.dp)
                                    .clipToBounds()
                                    .glassGround(look, MaterialTheme.colorScheme.background)
                            ) { content(go.value) }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        // The first composition and its enter animations settle on the stopped clock before the change.
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        compose.runOnIdle { go.value = true }
        val frames = mutableListOf<Bitmap>()
        var elapsed = 0
        times.forEach { t ->
            compose.mainClock.advanceTimeBy((t - elapsed).toLong())
            elapsed = t
            frames += compose.onNodeWithTag("stage").captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        }
        compose.mainClock.autoAdvance = true
        return frames
    }

    // The two rows joined, a label per row, the ms under each frame.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#sheet
    private fun sheet(name: String, night: Boolean, times: List<Int>, today: List<Bitmap>, proposed: List<Bitmap>) =
        sheet(name, night, times, listOf("today" to today, "proposed" to proposed))

    private fun sheet(name: String, night: Boolean, times: List<Int>, rows: List<Pair<String, List<Bitmap>>>) {
        val scale = 0.6f
        val w = (rows[0].second[0].width * scale).toInt()
        val h = (rows[0].second[0].height * scale).toInt()
        val labelW = 230
        val ms = 40
        val gap = 12
        val sheet = Bitmap.createBitmap(labelW + (w + gap) * times.size + gap, (h + ms) * rows.size + gap, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(if (night) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        val ink = if (night) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 26f }
        val smooth = Paint(Paint.FILTER_BITMAP_FLAG)
        rows.forEachIndexed { r, (label, frames) ->
            val top = r * (h + ms) + gap
            canvas.drawText(label, 10f, top + h / 2f, text)
            frames.forEachIndexed { i, frame ->
                val left = labelW + gap + i * (w + gap)
                canvas.drawBitmap(frame, null, AndroidRect(left, top, left + w, top + h), smooth)
                canvas.drawText("${times[i]} ms", left + 4f, top + h + 28f, text)
            }
        }
        sheet.captureRoboImage("src/test/screenshots/anim_${name}_draft_${if (night) "dark" else "light"}.png", roborazziOptions = options)
    }

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#film
    private fun film(name: String, width: Int, height: Int, times: List<Int> = standard,
                     today: @Composable (Boolean) -> Unit, proposed: @Composable (Boolean) -> Unit) {
        RuntimeEnvironment.setQualifiers("w411dp-h914dp-420dpi")
        listOf(false, true).forEach { night ->
            val a = row(night, times, width, height, today)
            val b = row(night, times, width, height, proposed)
            sheet(name, night, times, a, b)
        }
    }

    // One sheet of several rows, a today and a proposed row per change: [film]'s two rows are not enough where a piece
    // has two changes (the footer).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#case
    private fun case(label: String, content: @Composable (Boolean) -> Unit) = label to content

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#filmRows
    private fun filmRows(name: String, width: Int, height: Int, times: List<Int> = standard,
                         cases: List<Pair<String, @Composable (Boolean) -> Unit>>) {
        RuntimeEnvironment.setQualifiers("w411dp-h914dp-420dpi")
        listOf(false, true).forEach { night ->
            sheet(name, night, times, cases.map { (label, content) -> label to row(night, times, width, height, content) })
        }
    }

    // A swap through nothing: the old content out first (90 ms), the new one in after it (110 ms, 90 ms later), never
    // clipped while the size changes (SizeTransform, AnimatedContentSamples.kt). Material 3's fade through.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#fadeThrough
    // https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedContentSamples.kt#SimpleAnimatedContentSample
    private fun <S> fadeThrough(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (fadeIn(tween(110, delayMillis = 90)) togetherWith fadeOut(tween(90))).using(SizeTransform(clip = false))
    }

    // The proposed swap of a small piece: today the content changes at once, proposed it goes through AnimatedContent.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#Swap
    @Composable
    private fun <S> Swap(contentState: S, proposed: Boolean, label: String, content: @Composable (S) -> Unit) {
        if (proposed) {
            AnimatedContent(targetState = contentState, transitionSpec = fadeThrough(), label = label) { content(it) }
        } else {
            content(contentState)
        }
    }

    // 1. FeedScreen's Follow and Following: today an `if` between an outlined BoldButton "Following" and a filled
    // BoldButton "Follow", so label, fill and width jump. Proposed: Yiḍ's FollowButton (feature/feed/ProfileHeader.kt), one
    // button, the primary fill animated by alpha only (a fade to Color.Transparent would pass through grey), the icon and
    // label crossing in AnimatedContent, the width animated.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#Follow
    // https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedContentSamples.kt#SimpleAnimatedContentSample
    // not ported: the pressed corner morph of FollowButton, its ripple and its click (a picture is not pressed)
    @Composable
    private fun Follow(following: Boolean, proposed: Boolean) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!proposed) {
                if (following) {
                    BoldButton(onClick = {}) { Text("Following") }
                } else {
                    BoldButton(filled = true, onClick = {}) { Text("Follow") }
                }
            } else {
                val colors = MaterialTheme.colorScheme
                val glass = LocalGlass.current!!
                // The fill is read in the draw block: no recomposition per frame. Same hue, alpha only.
                val container = animateFloatAsState(if (following) 0f else 1f, tween(200), label = "followContainer")
                val contentColor by animateColorAsState(
                    if (following) colors.primary else colors.onPrimary, tween(200), label = "followContent"
                )
                GlassTarget(Modifier) {
                    Row(
                        Modifier
                            .heightIn(min = ButtonMediumContainerHeight)
                            .glassZone(CircleShape, glass, lens = 1f)
                            .drawBehind {
                                val outline = CircleShape.createOutline(size, layoutDirection, this)
                                drawOutline(outline, colors.primary, alpha = container.value)
                            }
                            .padding(horizontal = ButtonMediumLeadingSpace),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(targetState = following, transitionSpec = fadeThrough(), label = "followLabel") { isFollowing ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isFollowing) YidIcons.Check else YidIcons.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonMediumIconSize),
                                    tint = contentColor
                                )
                                Spacer(Modifier.width(ButtonMediumIconLabelSpace))
                                Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.titleMedium, color = contentColor)
                            }
                        }
                    }
                }
            }
        }
    }

    // ButtonMediumTokens, as ProfileHeader.kt keeps them
    private val ButtonMediumContainerHeight = 56.dp
    private val ButtonMediumIconLabelSpace = 8.dp
    private val ButtonMediumIconSize = 24.dp
    private val ButtonMediumLeadingSpace = 24.dp

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#follow
    @Test
    fun follow() = film(
        "follow", 240, 100,
        today = { Follow(it, proposed = false) }, proposed = { Follow(it, proposed = true) }
    )

    // 2. TimelineScreen's alert in the capsule's actions. Today a plain `if` (it pops in); proposed AnimatedVisibility,
    // fadeIn + scaleIn in, fadeOut + scaleOut out, with the room opening and closing so the hint does not jump
    // (expandHorizontally and shrinkHorizontally).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#Alert
    // https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedVisibilitySamples.kt#AnimatedVisibilityWithBooleanVisibleParamNoReceiver
    @Composable
    private fun Alert(visible: Boolean, proposed: Boolean) {
        val alert: @Composable () -> Unit = {
            BannerAction(
                icon = YidIcons.Warning,
                label = "What went wrong",
                onClick = {},
                container = MaterialTheme.colorScheme.errorContainer,
                tint = MaterialTheme.colorScheme.error
            )
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            AppBarWithSearch(
                inputField = { SearchCapsuleHint(YidIcons.Search, listOf("Search saved posts"), onClick = {}) },
                navigationIcon = {
                    FolderSwitch(
                        icon = YidIcons.Folder,
                        trailingIcon = YidIcons.ExpandMore,
                        label = "All",
                        expanded = false,
                        onClick = {}
                    )
                },
                actions = if (proposed) ({
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.6f) + expandHorizontally(tween(200)),
                        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.6f) + shrinkHorizontally(tween(200))
                    ) { alert() }
                }) else ({
                    if (visible) alert()
                })
            )
        }
    }

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#alert
    @Test
    fun alert() = film(
        "alert", 360, 72,
        today = { Alert(it, proposed = false) }, proposed = { Alert(it, proposed = true) }
    )

    // 3. TimelineFooter and FeedScreen's footer item: LoadingMark, "Load older posts" and "No older posts available."
    // replace one another at once. Proposed: AnimatedContent keyed on the footer state, the Box's height animated by
    // animateContentSize (the one animation of the size: AnimatedContent's own snaps). Two changes shown: the page
    // arrived (the mark gives way to the button), the last page arrived (the button gives way to the text).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#Footer
    // https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedContentSamples.kt#SimpleAnimatedContentSample
    private enum class FooterState { Loading, More, End }

    // LoadingMark draws on Robolectric: the real mark, held at one frame (not running, the star lit by progress).
    @Composable
    private fun FooterContent(contentState: FooterState) {
        when (contentState) {
            FooterState.Loading -> LoadingMark(size = 28.dp, running = false, progress = { 0.6f })
            FooterState.More -> BoldButton(onClick = {}) { Text("Load older posts") }
            FooterState.End -> Text(
                "No older posts available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun Footer(from: FooterState, to: FooterState, go: Boolean, proposed: Boolean) {
        val contentState = if (go) to else from
        Column(Modifier.fillMaxWidth()) {
            ZoneSurface(
                shape = ShapeDefaults.ExtraLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("The last post of the page", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
            if (!proposed) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    FooterContent(contentState)
                }
            } else {
                Box(Modifier.fillMaxWidth().animateContentSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = contentState,
                        transitionSpec = {
                            (fadeIn(tween(110, delayMillis = 90)) togetherWith fadeOut(tween(90)))
                                .using(SizeTransform(clip = false) { _, _ -> snap() })
                        },
                        label = "footerContent"
                    ) { FooterContent(it) }
                }
            }
        }
    }

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#footer
    @Test
    fun footer() = filmRows(
        "footer", 320, 170,
        cases = listOf(
            case("today, loaded") { Footer(FooterState.Loading, FooterState.More, it, proposed = false) },
            case("proposed, loaded") { Footer(FooterState.Loading, FooterState.More, it, proposed = true) },
            case("today, the end") { Footer(FooterState.More, FooterState.End, it, proposed = false) },
            case("proposed, the end") { Footer(FooterState.More, FooterState.End, it, proposed = true) }
        )
    )

    // 4. FoldersScreen's FolderZone, a folder's row open on one account: the count text ("Empty" to "1 account"), the
    // "Open/Close" label and the check (the folder's name to the check) change at once. Proposed: AnimatedContent, fade
    // through, on those three. The panel's own AnimatedVisibility is the app's and is shown open in both rows.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#FolderRow
    // https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedContentSamples.kt#SimpleAnimatedContentSample
    @Composable
    private fun FolderRow(filed: Boolean, proposed: Boolean) {
        val count = if (filed) 1 else 0
        ZoneSurface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Friends",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Swap(count, proposed, "folderCount") { shown ->
                            Text(
                                when (shown) {
                                    0 -> "Empty"
                                    1 -> "1 account"
                                    else -> "$shown accounts"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    BoldButton(onClick = {}) { Text("Rename") }
                    BoldIconButton(onClick = {}) {
                        Icon(YidIcons.Delete, contentDescription = "Delete Friends", modifier = Modifier.size(20.dp))
                    }
                    BoldButton(onClick = {}) {
                        Swap(filed, proposed, "folderOpen") { open -> Text(if (open) "Close" else "Open") }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp)
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(url = null, name = "Ada Lovelace", size = 28.dp)
                    Text(
                        "Ada Lovelace",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f).padding(start = 10.dp)
                    )
                    Swap(filed, proposed, "folderCheck") { here ->
                        if (here) {
                            Icon(
                                YidIcons.Check,
                                contentDescription = "Filed here",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                "Main",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AnimationFilmstripDraftScreenshotTest.kt#folders
    @Test
    fun folders() = film(
        "folders", 360, 160,
        today = { FolderRow(it, proposed = false) }, proposed = { FolderRow(it, proposed = true) }
    )
}
