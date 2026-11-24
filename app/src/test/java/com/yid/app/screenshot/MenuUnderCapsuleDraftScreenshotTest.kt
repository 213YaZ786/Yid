package com.yid.app.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.model.Post
import com.yid.app.core.model.PostStats
import com.yid.app.di.appModule
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.AppBarWithSearch
import com.yid.app.ui.component.Avatar
import com.yid.app.ui.component.BoldIconButton
import com.yid.app.ui.component.CapsuleMenuOffset
import com.yid.app.ui.component.DropdownMenuGroup
import com.yid.app.ui.component.DropdownMenuItem
import com.yid.app.ui.component.FolderSwitch
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.component.SearchCapsuleField
import com.yid.app.ui.component.SearchCapsuleHint
import com.yid.app.ui.component.SelectableDropdownMenuItem
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.ui.component.relativeTime
import com.yid.app.ui.glass.LocalGlassBackdrop
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
 * DRAFT for the user's yes (requests.md points 10 and 11), nothing of app/src/main changes: the folder menu of Accounts and
 * the feed menu of Home open 8 dp under the capsule, not on its edge. Two pictures of each screen, the same screen with the
 * menu open: menu_under_capsule_*_now (the menu's top edge 8 dp under the FolderSwitch pill, GlassMenu's default
 * MenuContentOffset, which is the capsule's own bottom edge since the pill sits 8 dp inside it) and
 * menu_under_capsule_*_draft (the menu's top edge CapsuleMenuOffset.y under the pill, which is 8 dp under the capsule: the
 * constant of ui/component/FeedBanner.kt is read, not retyped); Home's are menu_under_capsule_home_now and
 * menu_under_capsule_home_draft, Accounts' menu_under_capsule_accounts_now and menu_under_capsule_accounts_draft. Each on
 * phone and tablet, light and dark, plus the 200 % text and 360 dp edges.
 *
 * GlassMenu is a Popup and Robolectric draws no Popup, so the menu is drawn inline over a scrim, as LinkedOut's draft does,
 * here with the gap as a parameter; the groups and rows are the app's own (ui/component/GlassMenu.kt: DropdownMenuGroup,
 * SelectableDropdownMenuItem with its counts, DropdownMenuItem), the capsule is the app's own AppBarWithSearch with its
 * FolderSwitch. The anchor is the pill's bottom edge and middle, where GlassMenu's AnchorPositionProvider gives it.
 *
 * Ports:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/MenuUnderCapsuleDraftScreenshotTest.kt#MenuUnderCapsuleDraftScreenshotTest
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/HomeTopDraftScreenshotTest.kt#HomeTopDraftScreenshotTest
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/AccountsTopDraftScreenshotTest.kt#AccountsTopDraftScreenshotTest
 * Compared: https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/SearchBar.kt#AppBarWithSearch
 * not ported: the title, age and alert layouts of the older Home and Accounts drafts, the capsule replaced them (their now, a,
 * b, c, d, empty, AlertAction, WarningRounded, HeaderRow, FeedRow, Age, HomeTitle, TopRow, the paste Field and MenuPill with its
 * MenuOverScreen); their Tune and ChevronRight copies (DraftIcons has both)
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class MenuUnderCapsuleDraftScreenshotTest : ScreenshotBase() {

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

    // The cards write the age against the clock, so the data is dated from the fixed instant of ScreenshotBase.
    private val now = FIXED_NOW

    private class Sample(val name: String, val handle: String, val hoursAgo: Long)

    private val samples = listOf(
        Sample("Ada Lindqvist", "ada.bsky.social", 2),
        Sample("Bram Okafor", "bram.example.com", 36),
        Sample("Cleo Marsh", "cleo.bsky.social", 84),
        Sample("Dara Whitfield", "dara.example.org", 120)
    )

    private val posts = listOf(
        Post(
            id = "at://did:plc:draftada/app.bsky.feed.post/3kdraft0001",
            authorHandle = "ada.bsky.social",
            authorName = "Ada Lindqvist",
            text = "Finished the first draft of the trail guide. Photos of the ridge come next week.",
            publishedAtMillis = now - 2 * 60 * 60 * 1000,
            permalink = "https://bsky.app/profile/ada.bsky.social/post/3kdraft0001",
            stats = PostStats(replies = 12, reposts = 41, quotes = 3, likes = 930)
        ),
        Post(
            id = "at://did:plc:draftbram/app.bsky.feed.post/3kdraft0002",
            authorHandle = "bram.example.com",
            authorName = "Bram Okafor",
            text = "Three things we learned running a small server for a hundred friends.",
            publishedAtMillis = now - 26 * 60 * 60 * 1000,
            permalink = "https://bsky.app/profile/bram.example.com/post/3kdraft0002",
            stats = PostStats(replies = 4, reposts = 9, quotes = 1, likes = 210)
        ),
        Post(
            id = "at://did:plc:draftcleo/app.bsky.feed.post/3kdraft0003",
            authorHandle = "cleo.bsky.social",
            authorName = "Cleo Marsh",
            text = "A good week for the people building in the open. Thank you to every maintainer.",
            publishedAtMillis = now - 3 * 24 * 60 * 60 * 1000,
            permalink = "https://bsky.app/profile/cleo.bsky.social/post/3kdraft0003",
            stats = PostStats(replies = 2, reposts = 6, quotes = 0, likes = 145)
        )
    )

    private val folders = listOf("Friends" to 4, "News" to 3, "Local" to 2)

    // GlassMenu's MenuWidth (private there).
    private val MenuWidth = 280.dp

    // GlassMenu's MenuContentOffset (private there): the menu's top edge 8 dp under the pill, on the capsule's edge.
    private val nowGap = 8.dp

    // The kit's constant: 8 dp under the capsule.
    private val draftGap = CapsuleMenuOffset.y

    @Test
    fun accountsNow() = capture("menu_under_capsule_accounts_now") { AccountsMenu(nowGap) }

    @Test
    fun accountsDraft() = capture("menu_under_capsule_accounts_draft") { AccountsMenu(draftGap) }

    @Test
    fun homeNow() = capture("menu_under_capsule_home_now") { HomeMenu(nowGap) }

    @Test
    fun homeDraft() = capture("menu_under_capsule_home_draft") { HomeMenu(draftGap) }

    // Edge: the pictures at 200 % text and on a 360 dp phone (ScreenshotBase#captureEdge).
    @Test
    fun accountsNowEdge() = captureEdge("menu_under_capsule_accounts_now") { AccountsMenu(nowGap) }

    @Test
    fun accountsDraftEdge() = captureEdge("menu_under_capsule_accounts_draft") { AccountsMenu(draftGap) }

    @Test
    fun homeNowEdge() = captureEdge("menu_under_capsule_home_now") { HomeMenu(nowGap) }

    @Test
    fun homeDraftEdge() = captureEdge("menu_under_capsule_home_draft") { HomeMenu(draftGap) }

    // AccountsScreen's capsule and its folders menu (All with its count, the folders, then in a group of its own "Manage
    // folders"), open.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/MenuUnderCapsuleDraftScreenshotTest.kt#AccountsMenu
    @Composable
    private fun AccountsMenu(gap: Dp) {
        var anchor by remember { mutableStateOf(IntOffset.Zero) }
        MenuUnderScreen(
            anchor = anchor,
            gap = gap,
            screen = {
                Column(Modifier.fillMaxSize().systemBarsPadding()) {
                    Box(Modifier.padding(horizontal = 16.dp + LocalReadableInset.current, vertical = 8.dp)) {
                        AppBarWithSearch(
                            inputField = {
                                SearchCapsuleField(
                                    searchIcon = YidIcons.Search,
                                    value = "",
                                    onValueChange = {},
                                    hints = listOf("Search 12 accounts or open a handle", "Search 12 accounts"),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions.Default
                                )
                            },
                            actions = {
                                BoldIconButton(onClick = {}) { Icon(YidIcons.Paste, contentDescription = "Paste a link or a handle") }
                            },
                            navigationIcon = { CapsuleFolder("Accounts", onFolder = { anchor = it }) }
                        )
                    }
                    Column(
                        Modifier.padding(horizontal = 16.dp + LocalReadableInset.current),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        samples.forEach { AccountCard(it) }
                    }
                }
            }
        ) {
            DropdownMenuGroup(index = 0, count = 2) {
                SelectableDropdownMenuItem(selected = true, onClick = {}, text = "All", count = 9)
                folders.forEach { (name, count) -> SelectableDropdownMenuItem(selected = false, onClick = {}, text = name, count = count) }
            }
            DropdownMenuGroup(index = 1, count = 2) {
                DropdownMenuItem(
                    text = "Manage folders",
                    onClick = {},
                    leadingIcon = { Icon(DraftIcons.Tune, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    trailingIcon = {
                        Icon(
                            DraftIcons.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }

    // TimelineScreen's capsule and its feed menu (the folders with their counts, then Liked and Archived), open.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/MenuUnderCapsuleDraftScreenshotTest.kt#HomeMenu
    @Composable
    private fun HomeMenu(gap: Dp) {
        var anchor by remember { mutableStateOf(IntOffset.Zero) }
        MenuUnderScreen(
            anchor = anchor,
            gap = gap,
            screen = {
                Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = LocalReadableInset.current)) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        AppBarWithSearch(
                            inputField = { SearchCapsuleHint(YidIcons.Search, listOf("Search saved posts"), onClick = {}) },
                            navigationIcon = { CapsuleFolder("Home", onFolder = { anchor = it }) }
                        )
                    }
                    posts.forEachIndexed { i, post ->
                        PostCard(post = post, onClick = {}, onOpenLink = {}, onDownload = {}, unread = i == 0)
                    }
                }
            }
        ) {
            DropdownMenuGroup(index = 0, count = 2) {
                SelectableDropdownMenuItem(selected = true, onClick = {}, text = "All", count = 9)
                folders.forEach { (name, count) -> SelectableDropdownMenuItem(selected = false, onClick = {}, text = name, count = count) }
            }
            DropdownMenuGroup(index = 1, count = 2) {
                listOf("Liked", "Archived").forEach { SelectableDropdownMenuItem(selected = false, onClick = {}, text = it) }
            }
        }
    }

    // The capsule's navigation icon as the screens build it: the heading wrapper, the folder pill inside it; [onFolder] gets
    // the point under the pill's middle, where the menu opens.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/MenuUnderCapsuleDraftScreenshotTest.kt#CapsuleFolder
    @Composable
    private fun CapsuleFolder(screen: String, onFolder: (IntOffset) -> Unit) {
        Box(Modifier.semantics { heading(); contentDescription = screen }) {
            FolderSwitch(
                icon = YidIcons.Folder,
                trailingIcon = DraftIcons.ExpandMore,
                label = "All",
                expanded = true,
                onClick = {},
                modifier = Modifier.onGloballyPositioned {
                    val at = it.positionInRoot()
                    onFolder(IntOffset((at.x + it.size.width / 2).toInt(), (at.y + it.size.height).toInt()))
                }
            )
        }
    }

    // MenuOverScreen's body with [gap] as a parameter: a scrim on the screen, then the menu under the anchor, kept 12 dp from
    // the screen's sides as GlassMenu's MenuHorizontalMargin, in no glass backdrop as GlassMenu provides. Menu.kt's
    // DropdownMenu `offset` is the gap.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/MenuUnderCapsuleDraftScreenshotTest.kt#MenuUnderScreen
    // https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Menu.kt#DropdownMenu
    @Composable
    private fun MenuUnderScreen(
        anchor: IntOffset,
        gap: Dp,
        screen: @Composable () -> Unit,
        menu: @Composable ColumnScope.() -> Unit
    ) {
        var origin by remember { mutableStateOf(IntOffset.Zero) }
        var width by remember { mutableStateOf(0) }
        val density = LocalDensity.current
        Box(
            Modifier.fillMaxSize().onGloballyPositioned {
                val at = it.positionInRoot()
                origin = IntOffset(at.x.toInt(), at.y.toInt())
                width = it.size.width
            }
        ) {
            screen()
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)))
            val menuWidth = with(density) { MenuWidth.roundToPx() }
            val margin = with(density) { 12.dp.roundToPx() }
            val below = with(density) { gap.roundToPx() }
            // The menu is centred under the pill: [anchor].x is the pill's middle.
            val x = (anchor.x - origin.x - menuWidth / 2).coerceIn(margin, (width - menuWidth - margin).coerceAtLeast(margin))
            CompositionLocalProvider(LocalGlassBackdrop provides null) {
                Column(
                    Modifier.offset { IntOffset(x, anchor.y - origin.y + below) }.width(MenuWidth),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    content = menu
                )
            }
        }
    }

    // AccountCard's zone, without its hold menu and its filing chip (TopCapsuleDraftScreenshotTest's copy).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/MenuUnderCapsuleDraftScreenshotTest.kt#AccountCard
    @Composable
    private fun AccountCard(sample: Sample) {
        ZoneSurface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Avatar(url = null, name = sample.name, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(sample.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "@${sample.handle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    relativeTime(now - sample.hoursAgo * 60 * 60 * 1000 - 30 * 60 * 1000),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
