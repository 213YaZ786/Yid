package com.yid.app.screenshot

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.common.AppError
import com.yid.app.core.common.present
import com.yid.app.core.model.Post
import com.yid.app.core.model.PostStats
import com.yid.app.di.appModule
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.AppBarWithSearch
import com.yid.app.ui.component.BannerAction
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.FolderSwitch
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.component.QuietButton
import com.yid.app.ui.component.SearchCapsuleHint
import com.yid.app.ui.component.dialogGlass
import com.yid.app.ui.component.failureMessage
import com.yid.app.ui.glass.LocalGlass
import com.yid.app.ui.icon.YidIcons
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * DRAFT for the user's yes (requests.md point 12), nothing of app/src/main changes: Home after a tap on the red triangle.
 * One pane of the dialog glass (ZoneAlertDialog's, dialogGlass) centred both ways over Home blurred behind it, as the
 * kit's FailureDialog draws it (ui/component/FailureDialog.kt): the red rounded triangle, a title from the Blame most
 * failed accounts share, a count, Close and Try again, no account listed. In the app it is ZoneAlertDialog
 * (BasicAlertDialog, BlurBehind(radiusDp = 24) and rememberCrossWindowBlur, AOSP's window blur) with the dialog's width
 * (280 to 560 dp) and padding (24 dp):
 * https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AlertDialog.kt#BasicAlertDialog
 * Ported from LinkedOut's draft of the same point:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AlertDraftScreenshotTest.kt#AlertDraftScreenshotTest
 *
 * Robolectric draws no window blur and no RenderEffect, so the picture does what the window does: the screen is
 * captured to a bitmap (Roborazzi's own capture, captureToImage), blurred by Mario Klingemann's stack blur, dimmed by
 * BlurBehind's dimAmount (0.23 by day, 0.56 by night), and the pane drawn over it.
 * https://github.com/wasabeef/glide-transformations/blob/main/transformations/src/main/java/jp/wasabeef/glide/transformations/internal/FastBlur.java#blur
 * https://github.com/takahirom/roborazzi/blob/main/sample-android/src/test/java/com/github/takahirom/roborazzi/sample/WindowCaptureTest.kt#WindowCaptureTest
 * not ported: WindowCaptureTest's composeBottomSheet, dialog and fragment tests and captureScreenRoboImage (the picture
 * is one screen through ScreenshotBase; Robolectric's window capture of a Dialog is not needed because the pane is drawn
 * in the same composition). FastBlur has the one method, blur, ported whole.
 * not ported: LinkedOut's HeaderRow variant c (a slot, the feed pill with "Updated", a slot): Yiḍ's Home top is point 10's
 * capsule (TopCapsuleDraftScreenshotTest's A2Capsule), the alert trailing in it
 * not ported: LinkedOut's Zone and its LinkedIn posts (Yiḍ's pane is the dialog glass of ZoneDialog.kt, its posts are
 * Bluesky's)
 *
 * The message is chosen by the Blame shared by most failed accounts (core/common/AppError.kt, Blame, read by the kit's
 * detectKnownError by name); the titles are failureMessage's own, called here, not retyped:
 *   DEVICE   "You are offline"
 *   NETWORK  "Your connection does not reach Bluesky"
 *   SERVICE  "Bluesky is holding back guests"
 *   APP      "Yiḍ could not read some pages"
 * Pictures: alert_centre_host (SERVICE) and alert_centre_offline (DEVICE), the kit's FailureDialog; alert_now is today's
 * private FailureDialog of TimelineScreen.kt (the title counts the accounts, each @handle with its explanation, five at
 * most and "and N more"); the red triangle in the capsule is top_capsule_home_capsule_alert, not drawn again.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class AlertDraftScreenshotTest : ScreenshotBase() {

    // PostCard reads the post marks through Koin.
    @Before
    fun startKoinOnce() {
        startKoin {
            androidContext(compose.activity.applicationContext)
            modules(appModule)
        }
    }

    @After
    fun stopKoinAgain() = stopKoin()

    // The card writes the age against the clock, so the posts are dated from the fixed instant of ScreenshotBase.
    private val now = FIXED_NOW

    private val unread = Post(
        id = "at://did:plc:draftada/app.bsky.feed.post/3kdraft0001",
        authorHandle = "ada.bsky.social",
        authorName = "Ada Lindqvist",
        text = "Finished the first draft of the trail guide. Photos of the ridge come next week.",
        publishedAtMillis = now - 2 * 60 * 60 * 1000,
        permalink = "https://bsky.app/profile/ada.bsky.social/post/3kdraft0001",
        stats = PostStats(replies = 12, reposts = 41, quotes = 3, likes = 930)
    )

    private val read = Post(
        id = "at://did:plc:draftbram/app.bsky.feed.post/3kdraft0002",
        authorHandle = "bram.example.com",
        authorName = "Bram Okafor",
        text = "Three things we learned running a small server for a hundred friends.",
        publishedAtMillis = now - 26 * 60 * 60 * 1000,
        permalink = "https://bsky.app/profile/bram.example.com/post/3kdraft0002",
        stats = PostStats(replies = 4, reposts = 9, quotes = 1, likes = 210)
    )

    // Today's list: each failed account with its error (TimelineScreen's state.errors), seven of them so "and 2 more" shows.
    private val failedToday = listOf(
        "ada.bsky.social" to AppError.RateLimited("public.api.bsky.app", 30),
        "bram.example.com" to AppError.RateLimited("public.api.bsky.app", 30),
        "cleo.bsky.social" to AppError.Timeout("public.api.bsky.app", 15_000),
        "dara.example.org" to AppError.AccountUnavailable("dara.example.org", "suspended"),
        "eli.bsky.social" to AppError.ServerError("public.api.bsky.app", 503),
        "fay.bsky.social" to AppError.RateLimited("public.api.bsky.app", 30),
        "gus.example.net" to AppError.AccountNotFound("gus.example.net")
    )

    // The blurred screen of the picture being drawn: filled by [blurBackdrop] once the screen has settled.
    private class Backdrop {
        var image: ImageBitmap? by mutableStateOf(null)
    }

    private var backdrop: Backdrop? = null

    // Blame.SERVICE: most failed accounts met Bluesky's rate limit.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AlertDraftScreenshotTest.kt#alertCentreHost
    @Test
    fun alertCentreHost() = capture("alert_centre_host", interact = ::blurBackdrop) {
        val errors = List(12) { AppError.RateLimited("public.api.bsky.app", 30) }
        val (title, text) = failureMessage(errors, 40, "Bluesky")!!
        AlertOverScreen { KitPane(title, text) }
    }

    // Blame.DEVICE: no network at all, no account is to blame.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AlertDraftScreenshotTest.kt#alertCentreOffline
    @Test
    fun alertCentreOffline() = capture("alert_centre_offline", interact = ::blurBackdrop) {
        val errors = List(40) { AppError.Offline }
        val (title, text) = failureMessage(errors, 40, "Bluesky")!!
        AlertOverScreen { KitPane(title, text) }
    }

    // Today's dialog over the same screen, for the comparison.
    @Test
    fun alertNow() = capture("alert_now", interact = ::blurBackdrop) { AlertOverScreen { TodayPane() } }

    // Edge: the host message at 200 % text and on a 360 dp phone.
    @Test
    fun edgeAlertCentreHost() = captureEdge("alert_centre_host", interact = ::blurBackdrop) {
        val errors = List(12) { AppError.RateLimited("public.api.bsky.app", 30) }
        val (title, text) = failureMessage(errors, 40, "Bluesky")!!
        AlertOverScreen { KitPane(title, text) }
    }

    // The window's blur, done by hand: capture what is drawn, blur it as wide as BlurBehind(radiusDp = 24) at this
    // density. blurBehindRadius is a Skia radius: SurfaceFlinger takes sigma = 0.57735 * radius + 0.5
    // (SkBlurMask::ConvertRadiusToSigma), and a stack blur of radius r has the sigma of its triangular kernel,
    // r / sqrt(6), so r = sigma * sqrt(6). The bitmap is not scaled down: the stack blur costs the same at any radius.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AlertDraftScreenshotTest.kt#blurBackdrop
    private fun blurBackdrop() {
        val shot = compose.onNodeWithTag("screen").captureToImage().asAndroidBitmap()
        val density = RuntimeEnvironment.getApplication().resources.displayMetrics.density
        val radiusPx = 24 * density
        val sigma = 0.57735f * radiusPx + 0.5f
        val blurred = blur(shot, (sigma * sqrt(6f)).roundToInt(), false)!!
        backdrop?.image = blurred.asImageBitmap()
        compose.waitForIdle()
    }

    // The screen, then (once it is captured) its blur, its dim and the pane over it.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AlertDraftScreenshotTest.kt#AlertOverScreen
    @Composable
    private fun AlertOverScreen(pane: @Composable () -> Unit) {
        val mine = remember { Backdrop().also { backdrop = it } }
        val dark = LocalGlass.current?.dark ?: false
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().testTag("screen")) { Screen() }
            mine.image?.let { picture ->
                Image(picture, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                // BlurBehind's dimAmount with the blur on.
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dark) 0.56f else 0.23f)))
                Box(Modifier.fillMaxSize().padding(horizontal = DialogMargin), contentAlignment = Alignment.Center) {
                    pane()
                }
            }
        }
    }

    // The kit's FailureDialog as it draws: ZoneAlertContent's layout (24 dp, 16 dp under the icon and the title, 24 dp under
    // the text, all centred), the red rounded triangle, the title (headlineSmall), the text (bodyMedium), Close then
    // Try again sharing the width. Over the blur the pane is dialogGlass's tint.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AlertDraftScreenshotTest.kt#AlertPane
    @Composable
    private fun KitPane(title: String, text: String) {
        DialogGlass {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    DraftIcons.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Buttons()
            }
        }
    }

    // Today's FailureDialog (TimelineScreen.kt): no icon, the count as the title, each failed @handle in titleSmall with
    // its explanation, five shown, then "and N more"; Close and Try again as quiet buttons.
    // https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/timeline/TimelineScreen.kt#FailureDialog
    @Composable
    private fun TodayPane() {
        DialogGlass {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Box(Modifier.padding(bottom = 16.dp).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Text(
                        "${failedToday.size} accounts could not be updated",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                Column(Modifier.padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    failedToday.take(FAILURES_SHOWN).forEach { (handle, error) ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("@$handle", style = MaterialTheme.typography.titleSmall)
                            Text(
                                error.present().explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (failedToday.size > FAILURES_SHOWN) {
                        Text(
                            "and ${failedToday.size - FAILURES_SHOWN} more",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f), propagateMinConstraints = true) { QuietButton(onClick = {}) { Text("Close") } }
                    Box(Modifier.weight(1f), propagateMinConstraints = true) { QuietButton(onClick = {}) { Text("Try again") } }
                }
            }
        }
    }

    // The kit's two buttons: Close (quiet) and Try again (bold, filled), sharing the width.
    @Composable
    private fun Buttons() {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f), propagateMinConstraints = true) { QuietButton(onClick = {}) { Text("Close") } }
            Box(Modifier.weight(1f), propagateMinConstraints = true) { BoldButton(onClick = {}, filled = true) { Text("Try again") } }
        }
    }

    // The pane: ZoneAlertDialog's glass over the blur (dialogGlass), Material's dialog width, the content colour of a dialog.
    @Composable
    private fun DialogGlass(content: @Composable () -> Unit) {
        val look = LocalGlass.current
        val shape = ShapeDefaults.ExtraLarge
        Box(
            Modifier
                .widthIn(min = DialogMinWidth, max = DialogMaxWidth)
                .then(if (look != null) Modifier.dialogGlass(shape, look, blur = true) else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, shape))
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
        }
    }

    // The capsule of point 10 with the alert trailing: BannerAction in errorContainer and error with the red rounded
    // triangle, as TopCapsuleDraftScreenshotTest's AlertAction.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AlertDraftScreenshotTest.kt#AlertAction
    @Composable
    private fun AlertAction() {
        BannerAction(
            icon = DraftIcons.Warning,
            label = "What went wrong",
            onClick = {},
            container = MaterialTheme.colorScheme.errorContainer,
            tint = MaterialTheme.colorScheme.error
        )
    }

    // The header, then the posts (the first of them is the list's first item).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AlertDraftScreenshotTest.kt#Screen
    @Composable
    private fun Screen() {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = LocalReadableInset.current)) {
            HeaderRow()
            Spacer(Modifier.height(4.dp))
            PostCard(post = unread, onClick = {}, onOpenLink = {}, onDownload = {}, unread = true)
            PostCard(post = read, onClick = {}, onOpenLink = {}, onDownload = {})
        }
    }

    // Home's capsule: the folder switch, "Search saved posts", the alert trailing.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AlertDraftScreenshotTest.kt#HeaderRow
    @Composable
    private fun HeaderRow() {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            AppBarWithSearch(
                inputField = { SearchCapsuleHint(YidIcons.Search, listOf("Search saved posts"), onClick = {}) },
                navigationIcon = {
                    FolderSwitch(
                        icon = YidIcons.Folder,
                        trailingIcon = DraftIcons.ExpandMore,
                        label = "All",
                        expanded = false,
                        onClick = {}
                    )
                },
                actions = { AlertAction() }
            )
        }
    }

    // Stack Blur v1.0 by Mario Klingemann (http://incubator.quasimondo.com), Android port by Yahel Bouaziz, as
    // Wasabeef's FastBlur.blur: a compromise between a Gaussian and a box blur, a stack of colours moved along each row
    // then each column. Apache-2.0.
    // https://github.com/wasabeef/glide-transformations/blob/main/transformations/src/main/java/jp/wasabeef/glide/transformations/internal/FastBlur.java#blur
    private fun blur(sentBitmap: Bitmap, radius: Int, canReuseInBitmap: Boolean): Bitmap? {
        val bitmap = if (canReuseInBitmap) sentBitmap else sentBitmap.copy(Bitmap.Config.ARGB_8888, true)

        if (radius < 1) {
            return null
        }

        val w = bitmap.width
        val h = bitmap.height

        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = (i / divsum)
            i++
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            rinsum = 0; ginsum = 0; binsum = 0; routsum = 0; goutsum = 0; boutsum = 0; rsum = 0; gsum = 0; bsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + min(wm, max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[(stackpointer) % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            rinsum = 0; ginsum = 0; binsum = 0; routsum = 0; goutsum = 0; boutsum = 0; rsum = 0; gsum = 0; bsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = max(0, yp) + x

                sir = stack[i + radius]

                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]

                rbs = r1 - Math.abs(i)

                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs

                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }

                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000.toInt() and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
                y++
            }
            x++
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)

        return bitmap
    }

    // Material's dialog: 280 to 560 dp wide (BasicAlertDialog's DialogMinWidth and DialogMaxWidth), 24 dp from the sides.
    private val DialogMargin = 24.dp
    private val DialogMinWidth = 280.dp
    private val DialogMaxWidth = 560.dp

    // TimelineScreen.kt's FAILURES_SHOWN (private there).
    private val FAILURES_SHOWN = 5
}
