package com.yid.app.screenshot

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.model.Post
import com.yid.app.core.model.PostStats
import com.yid.app.di.appModule
import com.yid.app.feature.timeline.FolderButton
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.AppBarWithSearch
import com.yid.app.ui.component.Avatar
import com.yid.app.ui.component.BannerAction
import com.yid.app.ui.component.BoldIconButton
import com.yid.app.ui.component.EmptyZone
import com.yid.app.ui.component.FolderSwitch
import com.yid.app.ui.component.GlassSearchField
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.component.ScreenBanner
import com.yid.app.ui.component.SearchCapsuleField
import com.yid.app.ui.component.SearchCapsuleHint
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.ui.component.relativeTime
import com.yid.app.ui.glass.LocalGlass
import com.yid.app.ui.glass.LocalGlassBackdrop
import com.yid.app.ui.glass.glassSource
import com.yid.app.ui.glass.rememberGlassBackdrop
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
 * DRAFT for the user's yes (requests.md points 10 and 11, MTGA's points 41 and 47, LinkedOut's 10 to 12, 23 and 43), nothing
 * of app/src/main changes: Home's and Accounts' top as one glass capsule the width of the page, nothing under it, drawn with
 * the kit pieces already in main (ui/component/FeedBanner.kt: AppBarWithSearch, FolderSwitch, SearchCapsuleHint,
 * SearchCapsuleField; BannerAction in ScreenBanner.kt), as LinkedOut's draft and screens build it:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/TopCapsuleDraftScreenshotTest.kt#TopCapsuleDraftScreenshotTest
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/HomeEmptyLinkedOutDraftScreenshotTest.kt#HomeEmptyLinkedOutDraftScreenshotTest
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/accounts/AccountsScreen.kt#AccountsScreen
 * https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/SearchBar.kt#AppBarWithSearch
 * Home: the folder switch at the start, "Search saved posts" in the middle, the alert trailing on a failure only; no
 * "Home" word and no "Updated" line (the pull's mark is the app's own and has no place for it). Nobody followed: the capsule
 * with "All" alone over the empty zone. Accounts: the folder switch, the field, then Clear (typed text only) and Paste;
 * nobody followed: the field alone.
 * Pictures (prefix top_capsule): home_capsule, home_capsule_alert, home_capsule_folder, home_capsule_empty, accounts_capsule,
 * accounts_capsule_typed, accounts_capsule_empty, each on phone and tablet, light and dark, plus the 200 % text and 360 dp
 * edges; today's tops are home_today (HomeHeader, FolderButton) and accounts_today (ScreenBanner and the field), the same
 * data, and home_empty_today (nobody followed, no header at all, TimelineScreen's `followedCount == 0` branch).
 * not ported: LinkedOut's TopArrangementDraftScreenshotTest today, A, B arrangements, the pull and blur drafts, the menu
 * pictures (MenuUnderCapsuleDraftScreenshotTest draws them: Robolectric draws no Popup), HomeEmptyLinkedOutDraftScreenshotTest's
 * NowScreen banner with only the word "Home" (Yiḍ's today shows no header at all when nobody is followed, drawn in
 * [HomeEmptyToday])
 * not ported: TimelineScreen's feedMenu and its GlassMenu, showFailures and FailureDialog, onOpenSearch, the AnimatedVisibility
 * of the alert, FeedBanner for nobody followed, the list, ReadMarks; AccountsScreen's viewModel, GlassMenu with its
 * counts and Manage folders, paste() and its clipboard read, open(), the candidate card, the notice, HoldMenu, EmptyState,
 * the nobody-followed field's `rows.isEmpty()` item
 * not ported: AppBarWithSearch's SearchBarState, the expanded search, the colours of the scroll, elevation, scrollBehavior
 * and windowInsets (the app's AppBarWithSearch is its port)
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class TopCapsuleDraftScreenshotTest : ScreenshotBase() {

    // PostCard and FolderButton read the post marks and the settings through Koin.
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

    // The first two posts of PullOriginalDraftScreenshotTest.
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
        )
    )

    // AccountsScreen's rows: a name, its handle, the last post, the folder it is filed in.
    private class Sample(val name: String, val handle: String, val hoursAgo: Long, val folder: String)

    private val rows = listOf(
        Sample("Ada Lindqvist", "ada.bsky.social", 2, "Main"),
        Sample("Bram Okafor", "bram.example.com", 36, "Main"),
        Sample("Cleo Marsh", "cleo.bsky.social", 84, "Friends"),
        Sample("Dara Whitfield", "dara.example.org", 120, "Friends")
    )

    private val homeHints = listOf("Search saved posts")
    private val accountsHints = listOf("Search 12 accounts or open a handle", "Search 12 accounts")
    private val longFolder = "Journalists and researchers in Europe"
    private val typed = "Ada"

    @Test
    fun homeA2() = capture("top_capsule_home_capsule") { HomeScreen { A2Capsule("All", alert = false) } }

    @Test
    fun homeA2Alert() = capture("top_capsule_home_capsule_alert") { HomeScreen { A2Capsule("All", alert = true) } }

    @Test
    fun homeA2Folder() = capture("top_capsule_home_capsule_folder") { HomeScreen { A2Capsule(longFolder, alert = false) } }

    // Nobody followed: the capsule with the folder switch "All" alone, no hint, then the empty zone.
    @Test
    fun homeA2Empty() = capture("top_capsule_home_capsule_empty") { HomeEmptyDraft() }

    @Test
    fun accountsA2() = capture("top_capsule_accounts_capsule") { AccountsScreen("") }

    @Test
    fun accountsA2Typed() = capture("top_capsule_accounts_capsule_typed") { AccountsScreen(typed) }

    // Nobody followed: the field alone at the top, then the empty zone.
    @Test
    fun accountsA2Empty() = capture("top_capsule_accounts_capsule_empty") { AccountsEmptyDraft() }

    // Today's tops, for the comparison.
    @Test
    fun homeToday() = capture("top_capsule_home_today") { HomeToday() }

    @Test
    fun accountsToday() = capture("top_capsule_accounts_today") { AccountsToday() }

    @Test
    fun homeEmptyToday() = capture("top_capsule_home_empty_today") { HomeEmptyToday() }

    @Test
    fun edgeHomeA2() = captureEdge("top_capsule_home_capsule") { HomeScreen { A2Capsule("All", alert = false) } }

    @Test
    fun edgeHomeA2Alert() = captureEdge("top_capsule_home_capsule_alert") { HomeScreen { A2Capsule("All", alert = true) } }

    @Test
    fun edgeHomeA2Folder() = captureEdge("top_capsule_home_capsule_folder") { HomeScreen { A2Capsule(longFolder, alert = false) } }

    @Test
    fun edgeHomeA2Empty() = captureEdge("top_capsule_home_capsule_empty") { HomeEmptyDraft() }

    @Test
    fun edgeAccountsA2() = captureEdge("top_capsule_accounts_capsule") { AccountsScreen("") }

    @Test
    fun edgeAccountsA2Typed() = captureEdge("top_capsule_accounts_capsule_typed") { AccountsScreen(typed) }

    @Test
    fun edgeAccountsA2Empty() = captureEdge("top_capsule_accounts_capsule_empty") { AccountsEmptyDraft() }

    // The screen under the top: the capsule, then the sample posts straight under it, in the list's side inset.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/TopCapsuleDraftScreenshotTest.kt#HomeScreen
    @Composable
    private fun HomeScreen(top: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = LocalReadableInset.current)) {
            top()
            posts.forEach { PostCard(post = it, onClick = {}, onOpenLink = {}, onDownload = {}, unread = it === posts.first()) }
        }
    }

    // The screen under the top: the capsule, then the account cards, as AccountsScreen's list pads them.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/TopCapsuleDraftScreenshotTest.kt#AccountsScreen
    @Composable
    private fun AccountsScreen(query: String) {
        val visible = rows.filter { it.name.contains(query, ignoreCase = true) }
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.padding(horizontal = 16.dp + LocalReadableInset.current, vertical = 8.dp)) {
                AccountsCapsule(query)
            }
            Column(
                Modifier.padding(horizontal = 16.dp + LocalReadableInset.current),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visible.forEach { AccountCard(it, chip = false) }
            }
        }
    }

    // TimelineScreen's banner: one glass capsule, nothing under it: the folder switch, "Search saved posts", the alert on a
    // failure only (BannerAction in errorContainer and error, trailing).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/TopCapsuleDraftScreenshotTest.kt#A2Capsule
    @Composable
    private fun A2Capsule(folder: String, alert: Boolean) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            AppBarWithSearch(
                inputField = { SearchCapsuleHint(YidIcons.Search, homeHints, onClick = {}) },
                navigationIcon = { FolderHeading("Home", folder) },
                actions = if (alert) ({ AlertAction() }) else null
            )
        }
    }

    // The alert: Material Symbols Rounded warning (fill 1) in the error colour, as the kit's BannerAction asks.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/TopCapsuleDraftScreenshotTest.kt#AlertAction
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

    // AccountsScreen's capsule: the folder switch, the real field, Clear when text is typed, Paste always (the clipboard is
    // read only on a tap).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/TopCapsuleDraftScreenshotTest.kt#AccountsCapsule
    @Composable
    private fun AccountsCapsule(query: String) {
        AppBarWithSearch(
            inputField = {
                SearchCapsuleField(
                    searchIcon = YidIcons.Search,
                    value = query,
                    onValueChange = {},
                    hints = accountsHints,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions.Default
                )
            },
            actions = {
                if (query.isNotEmpty()) {
                    BoldIconButton(onClick = {}) { Icon(YidIcons.Close, contentDescription = "Clear") }
                }
                BoldIconButton(onClick = {}) { Icon(YidIcons.Paste, contentDescription = "Paste a link or a handle") }
            },
            navigationIcon = { FolderHeading("Accounts", "All") }
        )
    }

    // The wrapper is the screen's heading for TalkBack, the switch inside stays its own button (the menu is not drawn:
    // Robolectric draws no Popup).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/TopCapsuleDraftScreenshotTest.kt#FolderHeading
    @Composable
    private fun FolderHeading(screen: String, folder: String) {
        Box(Modifier.semantics { heading(); contentDescription = screen }) {
            FolderSwitch(
                icon = YidIcons.Folder,
                trailingIcon = DraftIcons.ExpandMore,
                label = folder,
                expanded = false,
                onClick = {}
            )
        }
    }

    // AccountCard's zone, without its hold menu; [chip] draws the filing chip of today's card (the draft's folder is the
    // capsule's switch).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/TopCapsuleDraftScreenshotTest.kt#AccountCard
    @Composable
    private fun AccountCard(sample: Sample, chip: Boolean) {
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
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        relativeTime(now - sample.hoursAgo * 60 * 60 * 1000 - 30 * 60 * 1000),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (chip) FilingChip(sample.folder)
                }
            }
        }
    }

    // Today's filing chip, AssistChip's look drawn from foundation parts: 32 dp high, a 1 dp outline, the folder icon, the
    // name (the guard keeps a new Material control out of the files).
    // https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/accounts/AccountsScreen.kt#AccountCard
    @Composable
    private fun FilingChip(folder: String) {
        Row(
            Modifier
                .widthIn(max = 140.dp)
                .height(32.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(YidIcons.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(folder, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    // A bare 48 dp icon target, today's IconButton in the field's trailing slot.
    @Composable
    private fun FieldIcon(icon: ImageVector, description: String) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = {}), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(24.dp))
        }
    }

    // Home with nobody followed, the draft: the capsule with the folder switch alone, then the empty zone weighted over what is
    // left, as TimelineScreen's EmptyState.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/HomeEmptyLinkedOutDraftScreenshotTest.kt#DraftScreen
    @Composable
    private fun HomeEmptyDraft() {
        EmptyScreen {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AppBarWithSearch(inputField = {}, navigationIcon = { FolderHeading("Home", "All") })
            }
        }
    }

    // Home with nobody followed, today: no header at all, the empty zone alone (TimelineScreen.kt's `followedCount == 0`).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/HomeEmptyLinkedOutDraftScreenshotTest.kt#NowScreen
    @Composable
    private fun HomeEmptyToday() = EmptyScreen {}

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/HomeEmptyLinkedOutDraftScreenshotTest.kt#Screen
    @Composable
    private fun EmptyScreen(banner: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = LocalReadableInset.current)) {
            banner()
            EmptyZone(
                title = "Nothing followed yet",
                message = "Find a few accounts and they will all appear here in one stream.",
                actionLabel = "Open Accounts",
                onAction = {},
                modifier = Modifier.weight(1f).fillMaxSize()
            )
        }
    }

    // Accounts with nobody followed, the draft: the field alone at the top (no folder to switch, nothing to filter), then the
    // empty zone. AccountsScreen's field, as the screen draws it without a capsule.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/accounts/AccountsScreen.kt#AccountsScreen
    @Composable
    private fun AccountsEmptyDraft() {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp + LocalReadableInset.current)) {
            Box(Modifier.size(1.dp).semantics { heading(); contentDescription = "Accounts" })
            GlassSearchField(
                value = "",
                onValueChange = {},
                placeholder = "Search people or paste a link",
                leadingIcon = { Icon(YidIcons.Search, contentDescription = null) },
                trailingIcon = { Row(verticalAlignment = Alignment.CenterVertically) { FieldIcon(YidIcons.Paste, "Paste a link or a handle") } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions.Default,
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp)
            )
            EmptyZone(
                title = "No accounts yet",
                message = "Search a name or paste a profile link above, then follow to build your timeline. " +
                    "The list stays on this phone. Only the reads go to Bluesky.",
                icon = YidIcons.Person,
                modifier = Modifier.weight(1f).fillMaxSize()
            )
        }
    }

    /**
     * Today's Home: HomeHeader's zone as the first item of the list (the alert on a failure only), the centred "Home" with
     * its arrow that opens the folders, "Updated N ago" under it, Search at the end, then the posts, and the floating
     * FolderButton over the list.
     * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/timeline/TimelineScreen.kt#HomeHeader
     * not ported: HomeHeader's state (a fixed folder and age), the failure button (nothing failed here), TimelineScreen's pull
     * and its list
     */
    @Composable
    private fun HomeToday() {
        val look = LocalGlass.current
        val backdrop = rememberGlassBackdrop()
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop.takeIf { look != null }) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().then(if (look != null) Modifier.glassSource(backdrop, look) else Modifier)) {
                    HomeScreen {
                        ZoneSurface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(Modifier.size(48.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable(onClickLabel = "Choose which folder to show", onClick = {})
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Home",
                                            style = MaterialTheme.typography.titleLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Icon(YidIcons.ArrowDown, contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(20.dp))
                                    }
                                    Text(
                                        "Updated " + relativeTime(now - 12 * 60 * 1000) + " ago",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                BoldIconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                                    Icon(YidIcons.Search, contentDescription = "Search saved posts", modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
                FolderButton(folderIcon = YidIcons.Folder, upIcon = YidIcons.ArrowUp, onClick = {}, showBackToTop = false, onBackToTop = {})
            }
        }
    }

    /**
     * Today's Accounts: ScreenBanner("Accounts", "N followed", the Folders action), the GlassSearchField under it with Paste,
     * then the cards, each with its filing chip.
     * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/accounts/AccountsScreen.kt#AccountsScreen
     * not ported: AccountsScreen's view model, paste(), open(), the candidate and person cards, the notice, the filing chip's
     * tap and FolderDialog
     */
    @Composable
    private fun AccountsToday() {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = LocalReadableInset.current)) {
            ScreenBanner(
                title = "Accounts",
                subtitle = "${rows.size} followed",
                trailing = { BannerAction(icon = YidIcons.Folder, label = "Folders", onClick = {}) }
            )
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassSearchField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Search people or paste a link",
                    leadingIcon = { Icon(YidIcons.Search, contentDescription = null) },
                    trailingIcon = { Row(verticalAlignment = Alignment.CenterVertically) { FieldIcon(YidIcons.Paste, "Paste a link or a handle") } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions.Default,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                rows.forEach { AccountCard(it, chip = true) }
            }
        }
    }
}
