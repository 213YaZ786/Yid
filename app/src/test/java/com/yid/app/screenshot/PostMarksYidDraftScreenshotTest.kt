package com.yid.app.screenshot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.model.Post
import com.yid.app.core.model.PostStats
import com.yid.app.di.appModule
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.AppBarWithSearch
import com.yid.app.ui.component.Avatar
import com.yid.app.ui.component.FolderSwitch
import com.yid.app.ui.component.NewDot
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.component.SearchCapsuleHint
import com.yid.app.ui.component.StatsRow
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.ui.component.relativeTime
import com.yid.app.ui.icon.YidIcons
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DRAFT for the user's yes (requests.md points 17 and 21), nothing of app/src/main changes: a post card as it will read
 * once Bluesky's marks are read. post_marks_draft: the author's verified check after the name (Bluesky's blue check, here a
 * filled circle in the phone's primary with its check in onPrimary, never a fixed colour), a #tag and a @mention in the
 * primary inside the text (the `tag` and `mention` facets, https://github.com/bluesky-social/atproto/blob/main/packages/api/lexicons/app/bsky/richtext/facet.json),
 * and under it a plain card of an account that is not verified, drawn by the real PostCard. post_marks_now: today's card,
 * the real PostCard with the same texts, the tag and the mention plain, no check.
 * The verified card is a local redraw of PostCard's layout (the real one draws the name, NewDot and plain text only and
 * the app has no piece for the mark yet): the same zone, avatar, name row, text and numbers, the check and the coloured
 * words added.
 * Ported from LinkedOut's card test, whose fixed posts and Koin start it keeps:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/PostCardScreenshotTest.kt#PostCardScreenshotTest
 * and the check from Bluesky's own component (MIT):
 * https://github.com/bluesky-social/social-app/blob/main/src/components/verification/VerificationCheck.tsx#VerificationCheck
 * its two icons, VerifiedCheck (a filled circle of radius 11.5 and a check) and VerifierCheck (a scalloped seal, a trusted
 * verifier's own mark):
 * https://github.com/bluesky-social/social-app/blob/main/src/components/icons/brands/VerifiedCheck.tsx#VerifiedCheck
 * https://github.com/bluesky-social/social-app/blob/main/src/components/icons/brands/VerifierCheck.tsx#VerifierCheck
 * Bluesky draws both in its blue and the check in white: here the circle (or seal) takes the primary and the check the
 * onPrimary of the phone's palette. The plan of point 17 gives the trusted verifiers the same mark as the others, so the
 * pictures draw VerifiedCheck only; VerifierCheck is ported for the day the user wants Bluesky's second shape.
 * not ported: LinkedOut's `read` and `unread` LinkedIn posts and ScreenBanner("Home", "All accounts") (Yiḍ's Home top is
 * point 10's capsule, its posts are Bluesky's); VerificationCheck's `Props` (Bluesky's icon props: size, fill, style, gradient;
 * a size in dp is the only prop here); the verification state's tooltip and dialog (a tap on the check, not drawn)
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class PostMarksYidDraftScreenshotTest : ScreenshotBase() {

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

    // The card writes the age against the clock, so the posts are dated from it (a field is built before the rule that
    // sets the clock, so the constant is read, not the clock).
    private val now = FIXED_NOW

    private val marked = Post(
        id = "at://did:plc:draftada/app.bsky.feed.post/3kdraft0001",
        authorHandle = "ada.bsky.social",
        authorName = "Ada Lindqvist",
        text = TEXT,
        publishedAtMillis = now - 2 * 60 * 60 * 1000,
        permalink = "https://bsky.app/profile/ada.bsky.social/post/3kdraft0001",
        stats = PostStats(replies = 12, reposts = 41, quotes = 3, likes = 930)
    )

    private val plain = Post(
        id = "at://did:plc:draftbram/app.bsky.feed.post/3kdraft0002",
        authorHandle = "bram.example.com",
        authorName = "Bram Okafor",
        text = "Three things we learned running a small server for a hundred friends.",
        publishedAtMillis = now - 26 * 60 * 60 * 1000,
        permalink = "https://bsky.app/profile/bram.example.com/post/3kdraft0002",
        stats = PostStats(replies = 4, reposts = 9, quotes = 1, likes = 210)
    )

    @Test
    fun marks() = capture("post_marks_draft") {
        Screen {
            MarkedCard(marked, unread = true)
            PostCard(post = plain, onClick = {}, onOpenLink = {}, onDownload = {})
        }
    }

    // Today's cards: the same two posts through the real PostCard.
    @Test
    fun marksNow() = capture("post_marks_now") {
        Screen {
            PostCard(post = marked, onClick = {}, onOpenLink = {}, onDownload = {}, unread = true)
            PostCard(post = plain, onClick = {}, onOpenLink = {}, onDownload = {})
        }
    }

    // Edge: the draft at 200 % text and on a 360 dp phone.
    @Test
    fun edge() = captureEdge("post_marks_draft") {
        Screen {
            MarkedCard(marked, unread = true)
            PostCard(post = plain, onClick = {}, onOpenLink = {}, onDownload = {})
        }
    }

    // Home's capsule (point 10) over the two cards, in the list's side inset.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/PostCardScreenshotTest.kt#cards
    @Composable
    private fun Screen(cards: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = LocalReadableInset.current)) {
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
                    }
                )
            }
            cards()
        }
    }

    // PostCard's zone with the two marks: the check after the name (before NewDot), the tag and the mention in the primary.
    // https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/ui/component/PostCard.kt#PostCard
    @Composable
    private fun MarkedCard(post: Post, unread: Boolean) {
        ZoneSurface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
            shape = ShapeDefaults.ExtraLarge
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(post.avatarUrl, post.authorName, size = 44.dp)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                post.authorName.ifBlank { "@${post.authorHandle}" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // Bluesky's PostMeta puts the badge right after the display name.
                            VerificationCheck(Modifier.padding(start = 4.dp), size = 16.dp)
                            NewDot(unread, Modifier.padding(start = 8.dp))
                        }
                        Text(
                            "@${post.authorHandle}, ${relativeTime(post.publishedAtMillis)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(withFacets(post.text), style = MaterialTheme.typography.bodyLarge)
                post.stats?.let { StatsRow(it) }
            }
        }
    }

    // The text with its facets drawn (withFacets): every #tag and @mention in the primary, as Bluesky's RichText links them.
    // https://github.com/bluesky-social/atproto/blob/main/packages/api/lexicons/app/bsky/richtext/facet.json#main
    @Composable
    private fun withFacets(text: String) = buildAnnotatedString {
        val colour = MaterialTheme.colorScheme.primary
        val facet = Regex("""[#@][\w.]+\w""")
        var from = 0
        facet.findAll(text).forEach { hit ->
            append(text.substring(from, hit.range.first))
            withStyle(SpanStyle(color = colour)) { append(hit.value) }
            from = hit.range.last + 1
        }
        append(text.substring(from))
    }
}

private const val TEXT = "Finished the first draft of the #trailguide. Photos of the ridge come next week, thanks " +
    "@bram.example.com for the map."

/**
 * Bluesky's VerificationCheck: the trusted verifier's own shape when [verifier], else the verified account's circle.
 * https://github.com/bluesky-social/social-app/blob/main/src/components/verification/VerificationCheck.tsx#VerificationCheck
 */
@Composable
internal fun VerificationCheck(modifier: Modifier = Modifier, size: Dp = 16.dp, verifier: Boolean = false) {
    if (verifier) VerifierCheck(modifier, size) else VerifiedCheck(modifier, size)
}

/**
 * VerifiedCheck: a filled circle (cx 12, cy 12, r 11.5) and a check, on a 24 unit grid, drawn here in the primary and
 * onPrimary of the palette.
 * https://github.com/bluesky-social/social-app/blob/main/src/components/icons/brands/VerifiedCheck.tsx#VerifiedCheck
 */
@Composable
internal fun VerifiedCheck(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val fill = MaterialTheme.colorScheme.primary
    val mark = MaterialTheme.colorScheme.onPrimary
    val check = remember {
        PathParser().parsePathString(
            "M17.659 8.175a1.36 1.36 0 0 1 0 1.925l-6.224 6.223a1.36 1.36 0 0 1-1.925 0L6.4 13.212a1.361 1.361 0 0 1 " +
                "1.925-1.925l2.149 2.148 5.26-5.26a1.36 1.36 0 0 1 1.925 0Z"
        ).toPath().apply { fillType = PathFillType.EvenOdd }
    }
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, Offset.Zero) {
            drawCircle(fill, radius = 11.5f, center = Offset(12f, 12f))
            drawPath(check, mark)
        }
    }
}

/**
 * VerifierCheck: the scalloped seal of a trusted verifier and its check, the same colours.
 * https://github.com/bluesky-social/social-app/blob/main/src/components/icons/brands/VerifierCheck.tsx#VerifierCheck
 */
@Composable
internal fun VerifierCheck(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val fill = MaterialTheme.colorScheme.primary
    val mark = MaterialTheme.colorScheme.onPrimary
    val seal = remember {
        PathParser().parsePathString(
            "M8.792 1.615a4.154 4.154 0 0 1 6.416 0 4.15 4.15 0 0 0 3.146 1.515 4.154 4.154 0 0 1 4 5.017 4.15 4.15 0 0 0 " +
                ".777 3.404 4.154 4.154 0 0 1-1.427 6.255 4.15 4.15 0 0 0-2.177 2.73 4.154 4.154 0 0 1-5.781 2.784 4.15 " +
                "4.15 0 0 0-3.492 0 4.154 4.154 0 0 1-5.78-2.784 4.15 4.15 0 0 0-2.178-2.73A4.154 4.154 0 0 1 .87 11.551a4.15 " +
                "4.15 0 0 0 .776-3.404 4.154 4.154 0 0 1 4-5.017 4.15 4.15 0 0 0 3.146-1.515Z"
        ).toPath().apply { fillType = PathFillType.EvenOdd }
    }
    val check = remember {
        PathParser().parsePathString(
            "M17.861 8.26a1.44 1.44 0 0 1 0 2.033l-6.571 6.571a1.437 1.437 0 0 1-2.033 0L5.97 13.58a1.438 1.438 0 0 1 " +
                "2.033-2.033l2.27 2.269 5.554-5.555a1.437 1.437 0 0 1 2.033 0Z"
        ).toPath().apply { fillType = PathFillType.EvenOdd }
    }
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, Offset.Zero) {
            drawPath(seal, fill)
            drawPath(check, mark)
        }
    }
}
