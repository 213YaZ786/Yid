package com.yid.app.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.yid.app.BuildConfig
import com.yid.app.core.system.BatteryExemption
import com.yid.app.core.update.StoreLink
import com.yid.app.core.update.UpdateMode
import com.yid.app.core.update.Updates
import com.yid.app.data.settings.AutoDownload
import com.yid.app.data.settings.StartTab
import com.yid.app.data.settings.ThemeMode
import com.yid.app.feature.welcome.rememberSetupTiles
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.navigation.WidthClass
import com.yid.app.sync.NewPostNotifier
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.FloatingFrame
import com.yid.app.ui.component.FloatingTop
import com.yid.app.ui.component.GlassRadio
import com.yid.app.ui.component.GlassSlider
import com.yid.app.ui.component.LocalDockPadding
import com.yid.app.ui.component.QuietButton
import com.yid.app.ui.component.SettingsActionTile
import com.yid.app.ui.component.SettingsFoldedSections
import com.yid.app.ui.component.SettingsHeaderSpacer
import com.yid.app.ui.component.SettingsListDetail
import com.yid.app.ui.component.SettingsSection
import com.yid.app.ui.component.SettingsSectionTitle
import com.yid.app.ui.component.SettingsSwitchTile
import com.yid.app.ui.component.SettingsTile
import com.yid.app.ui.component.SettingsTileDialogs
import com.yid.app.ui.component.SettingsTileGrid
import com.yid.app.ui.component.SideDockClearance
import com.yid.app.ui.component.SyncFrequency
import com.yid.app.ui.component.ZoneAlertDialog
import com.yid.app.ui.component.leftText
import com.yid.app.ui.component.rememberHaptics
import com.yid.app.ui.icon.YidIcons
import com.yid.app.ui.theme.TEXT_SCALES
import com.yid.app.ui.theme.textScaleLabel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

private enum class OpenDialog { NONE, THEME, TEXT_SIZE, KEEP, FREQUENCY, CLEAR, START_TAB, AUTO_DOWNLOAD, UPDATES, LAUNCH_REFRESH }

/** The groups of settings: one page after another on a phone, a list and the chosen one's page on a wide window. */
private val APPEARANCE = SettingsSection("appearance", "Appearance", SettingsIcons.Palette)
private val MEDIA = SettingsSection("media", "Media", SettingsIcons.Image)
private val READING = SettingsSection("reading", "Reading", YidIcons.Comment)
private val BACKGROUND = SettingsSection("background", "Background updates", YidIcons.Refresh)
private val STORAGE = SettingsSection("storage", "Storage", YidIcons.Folder)
private val ACCOUNTS_FILE = SettingsSection("accounts", "Accounts file", SettingsIcons.Upload)
private val ABOUT = SettingsSection("about", "About", YidIcons.Info)
private val TROUBLESHOOTING = SettingsSection("troubleshooting", "Troubleshooting", SettingsIcons.Warning)
private val SECTIONS = listOf(APPEARANCE, MEDIA, READING, BACKGROUND, STORAGE, ACCOUNTS_FILE, ABOUT, TROUBLESHOOTING)

private fun updatesLabel(mode: UpdateMode): String = when (mode) {
    UpdateMode.OFF -> "Off"
    UpdateMode.NOTIFY -> "Notify me"
    UpdateMode.INSTALL -> "Install"
}

private fun autoDownloadLabel(value: AutoDownload): String = when (value) {
    AutoDownload.OFF -> "Never"
    AutoDownload.UNMETERED -> "On Wi-Fi only"
    AutoDownload.ANY -> "On Wi-Fi or mobile data"
}

private val KEEP_DAYS = listOf(7, 30, 90, 365, 0)

private fun keepLabel(days: Int): String = when (days) {
    0 -> "Forever"
    7 -> "One week"
    30 -> "One month"
    90 -> "Three months"
    365 -> "One year"
    else -> "$days days"
}

/**
 * Settings as tiles, ported from LinkedOut's SettingsScreen (itself from MTGA's, SMS's and Dialer's, which follow
 * Android's Quick Settings tile):
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/settings/SettingsScreen.kt#SettingsScreen
 * Sections, switches, actions and the page are laid out as there, Settings being a tab of the dock here too, so the header has no
 * back button. Yiḍ's own sections and values are kept, each section's tiles in the order of Yiḍ's rows (the validated picture
 * of requests.md point 4 shows the switches among the choices). The guide tile reads "N left", the "Update by itself" tile is the
 * logic of the kit's UpdateSourceRow (a ListItem row, which does not fit a grid of tiles) over StoreLink.
 * not ported: sectionTiles (one list per section keeps Yiḍ's order, a switch and a choice side by side), the connection
 * check, the browsing data tile and its dialog, the guest cookies and Privacy, the Reads tile and Dns icon (LinkedIn's guest,
 * its jobs and its one host; Yiḍ has no cookies, no connection check and reads Bluesky's public API), LinkedIn links (the
 * Bluesky links tile replaces it), Your list (named Accounts file here), Support (Troubleshooting holds the Activity log),
 * LinkedOut's ChoiceDialog from ui.component (Yiḍ's own radio dialog below stays as it was, only the screen changes),
 * UpdateSourceRow (the kit's ListItem row, its logic lives in the "self" tile below)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDebugLog: () -> Unit,
    onOpenWelcome: () -> Unit,
    onOpenSavedMedia: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val storageBytes by viewModel.storageBytes.collectAsState()
    val imageCacheBytes by viewModel.imageCacheBytes.collectAsState()
    val offlineBytes by viewModel.offlineBytes.collectAsState()
    val offlineCount by viewModel.offlineCount.collectAsState()
    val context = LocalContext.current
    // The top row folds away going down and returns going up, the content full screen, as LinkedOut's SettingsScreen.
    val topBarScroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val statusTop = WindowInsets.statusBars.getTop(LocalDensity.current)
    val uriHandler = LocalUriHandler.current
    val haptics = rememberHaptics()
    // The guide's tile says how many steps are left, read from the system as the guide reads them.
    val setup = rememberSetupTiles()

    // Changed in system settings, so read again each time the screen returns.
    var blueskyLinksOn by remember { mutableStateOf(blueskyLinksEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        blueskyLinksOn = blueskyLinksEnabled(context)
        onPauseOrDispose { }
    }
    // Store can be installed while the app is away, so both are read again when the app comes back.
    var present by remember { mutableStateOf(StoreLink.present(context)) }
    var own by remember { mutableStateOf(StoreLink.selfUpdates(context)) }
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                present = StoreLink.present(context)
                own = StoreLink.selfUpdates(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var dialog by rememberSaveable { mutableStateOf(OpenDialog.NONE) }
    // The tile whose long press is open (its id), "" when none.
    var tileId by rememberSaveable { mutableStateOf("") }
    var sectionId by rememberSaveable { mutableStateOf(APPEARANCE.id) }
    val section = SECTIONS.first { it.id == sectionId }
    // On a phone every section starts folded; the one open stays open across a turn of the screen.
    var openId by rememberSaveable { mutableStateOf("") }
    val openSection = SECTIONS.firstOrNull { it.id == openId }
    // The page's scroll, held here so a drag on the header drives it (FloatingFrame's `scroll`): the folded
    // list on a phone, the chosen section's page on a wide window (it starts at the top again for each section).
    val compactScroll = rememberScrollState()
    val detailScroll = rememberSaveable(sectionId, saver = ScrollState.Saver) { ScrollState(0) }

    LaunchedEffect(Unit) { viewModel.measureStorage() }

    // The system file picker, so no storage permission is ever needed.
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportAccounts) }
    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importAccounts) }

    // Notifications can be blocked in Android at any time, so this is read
    // again whenever the screen comes back, like the Bluesky links state.
    val notifier = remember { NewPostNotifier(context) }
    var notificationsAllowed by remember { mutableStateOf(notifier.canNotify()) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = notifier.canNotify()
        onPauseOrDispose { }
    }
    /*
     * The progress line of automatic saving is a notification like any
     * other, so Android 13 and later want the same permission. Asked at the
     * moment automatic saving is turned on, and a refusal changes nothing to
     * the saving itself, only to seeing how far it is. Same as LinkedOut.
     */
    val downloadNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsAllowed = notifier.canNotify() }
    val askForDownloadNotifications: () -> Unit = {
        if (!notifier.canNotify() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            downloadNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Asked at the moment the reader turns notifications on, never before.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = notifier.canNotify()
        if (granted && notificationsAllowed) {
            viewModel.setNotifyNewPosts(true)
        } else {
            // Refused now or earlier. Android will not ask twice, so its own
            // screen is the only way left, and the reader decides there.
            Toast.makeText(context, "Notifications are off for Yiḍ in Android", Toast.LENGTH_SHORT).show()
            openNotificationSettings(context)
        }
    }
    val onNotifyChange: (Boolean) -> Unit = { enabled ->
        when {
            !enabled -> viewModel.setNotifyNewPosts(false)
            notifier.canNotify() -> viewModel.setNotifyNewPosts(true)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED ->
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> {
                // Permission granted or not needed, but switched off in Android.
                Toast.makeText(context, "Notifications are off for Yiḍ in Android", Toast.LENGTH_SHORT).show()
                openNotificationSettings(context)
            }
        }
    }

    val message by viewModel.message.collectAsState()
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    val measuring = "Measuring"
    val storageSize = storageBytes?.let { Formatter.formatShortFileSize(context, it) }
    val imageCacheSize = imageCacheBytes?.let { Formatter.formatShortFileSize(context, it) }
    val mediaSize = offlineBytes?.let { bytes ->
        if (offlineCount == 0) "Nothing saved yet" else "$offlineCount files, ${Formatter.formatShortFileSize(context, bytes)}"
    }

    // Every tile of a section, in the order of Yiḍ's rows: a switch where it was a switch, a tile that opens a choice,
    // a page or a link where it was a row. The value is the second line, the summary the long press's explanation.
    val tiles: Map<SettingsSection, List<SettingsTile>> = mapOf(
        APPEARANCE to listOf(
            SettingsActionTile(
                id = "theme", name = "Theme", title = "Theme", icon = SettingsIcons.Palette,
                value = themeLabel(settings.themeMode), summary = themeLabel(settings.themeMode),
                onClick = { dialog = OpenDialog.THEME }
            ),
            SettingsSwitchTile(
                id = "black", name = "Pure black", title = "Pure black", icon = SettingsIcons.Night,
                summary = "Deeper blacks in dark mode. Easier on the battery with OLED screens.",
                checked = settings.pureBlack, enabled = settings.themeMode != ThemeMode.LIGHT,
                onChange = viewModel::setPureBlack
            ),
            SettingsSwitchTile(
                id = "glass", name = "Glass effects", title = "Glass effects", icon = SettingsIcons.AutoAwesome,
                summary = "Zones and floating buttons in liquid glass, over a soft light in your wallpaper's colours.",
                checked = settings.glass, onChange = viewModel::setGlass
            ),
            SettingsSwitchTile(
                id = "counts", name = "Counts", title = "Show counts", icon = YidIcons.Heart,
                summary = "Replies, reposts, quotes and likes under each post.",
                checked = settings.showCounts, onChange = viewModel::setShowCounts
            ),
            SettingsActionTile(
                id = "textSize", name = "Text size", title = "Text size", icon = SettingsIcons.FormatSize,
                value = textScaleLabel(settings.textScale),
                summary = textScaleLabel(settings.textScale) + ", on top of Android's font size",
                onClick = { dialog = OpenDialog.TEXT_SIZE }
            ),
            SettingsSwitchTile(
                id = "compact", name = "Compact posts", title = "Compact posts", icon = SettingsIcons.GridView,
                summary = "Tighter spacing, smaller avatars and shorter pictures. More posts on screen.",
                checked = settings.compactPosts, onChange = viewModel::setCompactPosts
            ),
            SettingsSwitchTile(
                id = "square", name = "Square avatars", title = "Square avatars", icon = YidIcons.Person,
                summary = "Rounded squares instead of circles.",
                checked = settings.squareAvatars, onChange = viewModel::setSquareAvatars
            )
        ),
        MEDIA to listOf(
            SettingsSwitchTile(
                id = "mediaWifi", name = "Media on Wi-Fi", title = "Media on Wi-Fi only", icon = SettingsIcons.Wifi,
                summary = "On mobile data, pictures and videos wait for a tap. Avatars still load.",
                checked = settings.mediaOnWifiOnly, onChange = viewModel::setMediaOnWifiOnly
            ),
            SettingsActionTile(
                id = "autoSave", name = "Save media", title = "Save media automatically", icon = YidIcons.Download,
                value = autoDownloadLabel(settings.autoDownloadMedia), summary = autoDownloadLabel(settings.autoDownloadMedia),
                onClick = { dialog = OpenDialog.AUTO_DOWNLOAD }
            ),
            SettingsSwitchTile(
                id = "autoplay", name = "Autoplay", title = "Play videos automatically", icon = YidIcons.Play,
                summary = "Videos and GIFs play silently in the list as you scroll, and start " +
                    "when opened. Never on mobile data while Wi-Fi only is on.",
                checked = settings.autoplayVideos, onChange = viewModel::setAutoplayVideos
            ),
            SettingsSwitchTile(
                id = "muted", name = "Start muted", title = "Start videos muted", icon = YidIcons.VolumeOff,
                summary = "Sound stays off until you tap the speaker.",
                checked = settings.startMuted, onChange = viewModel::setStartMuted
            )
        ),
        READING to listOf(
            SettingsActionTile(
                id = "startTab", name = "Start on", title = "Start on", icon = YidIcons.Home,
                value = startTabLabel(settings.startTab), summary = startTabLabel(settings.startTab),
                onClick = { dialog = OpenDialog.START_TAB }
            ),
            // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/settings/SettingsScreen.kt#SettingsScreen
            // The state is the stop's label, a tap opens RefreshDialog.
            SettingsActionTile(
                id = "refreshLaunch", name = "Refresh at launch", title = "Refresh at launch", icon = SettingsIcons.Timer,
                value = settings.launchRefresh.tile, summary = settings.launchRefresh.line,
                onClick = { dialog = OpenDialog.LAUNCH_REFRESH }
            ),
            SettingsActionTile(
                id = "blueskyLinks", name = "Bluesky links", title = "Open Bluesky links in Yiḍ", icon = SettingsIcons.Link,
                value = if (blueskyLinksOn) "On" else "Off",
                summary = if (blueskyLinksOn) {
                    "On. bsky.app links open here."
                } else {
                    "Off. Turn on \"Open supported links\" and add the bsky.app " +
                        "link. Sharing a link to Yiḍ works either way."
                },
                onClick = { openLinkSettings(context) }
            )
        ),
        BACKGROUND to listOf(
            SettingsSwitchTile(
                id = "sync", name = "New posts", title = "Check for new posts", icon = YidIcons.Refresh,
                summary = "Keeps Home up to date and saves new posts, even when the app is closed.",
                checked = settings.backgroundSync,
                onChange = { enabled ->
                    viewModel.setBackgroundSync(enabled)
                    // Asked here and nowhere else: the reader just asked for
                    // work that runs with the app closed. See BatteryExemption.
                    if (enabled) BatteryExemption.request(context)
                }
            ),
            SettingsActionTile(
                id = "frequency", name = "Frequency", title = "Frequency", icon = SettingsIcons.Timer,
                value = intervalLabel(settings.syncIntervalMinutes),
                summary = if (settings.backgroundSync) {
                    "Checks run " + intervalLabel(settings.syncIntervalMinutes).replaceFirstChar { it.lowercase() } + "."
                } else {
                    "Needs \"Check for new posts\"."
                },
                // Depends on the switch above: a tap then only explains.
                onClick = if (settings.backgroundSync) ({ dialog = OpenDialog.FREQUENCY }) else ({ tileId = "frequency" })
            ),
            SettingsSwitchTile(
                id = "syncWifi", name = "Wi-Fi only", title = "Wi-Fi only", icon = SettingsIcons.Wifi,
                summary = "Uses no mobile data for background checks.",
                checked = settings.syncOnWifiOnly, enabled = settings.backgroundSync,
                onChange = viewModel::setWifiOnly
            ),
            SettingsSwitchTile(
                id = "notify", name = "Notify", title = "Notify about new posts", icon = YidIcons.Bell,
                summary = when {
                    !settings.backgroundSync -> "Needs \"Check for new posts\"."
                    settings.notifyNewPosts && !notificationsAllowed ->
                        "Blocked in Android settings. Tap to allow."
                    else -> "A notification for each new post Home would show, grouped, one sound per check."
                },
                // Shown off while Android blocks it, so a tap then means
                // "make it work" and leads to the permission or Android's page.
                checked = settings.notifyNewPosts && notificationsAllowed, enabled = settings.backgroundSync,
                onChange = onNotifyChange
            )
        ),
        STORAGE to listOf(
            SettingsActionTile(
                id = "keep", name = "Keep posts", title = "Keep posts", icon = SettingsIcons.Timer,
                value = keepLabel(settings.keepPostsDays),
                summary = keepLabel(settings.keepPostsDays) + ". Saved media follows the same limit.",
                onClick = { dialog = OpenDialog.KEEP }
            ),
            SettingsActionTile(
                id = "savedPosts", name = "Saved posts", title = "Saved posts", icon = YidIcons.Delete,
                value = storageSize ?: measuring, summary = storageSize?.let { "$it. Tap to delete." } ?: measuring,
                onClick = { dialog = OpenDialog.CLEAR }
            ),
            SettingsActionTile(
                id = "savedMedia", name = "Saved media", title = "Saved media", icon = YidIcons.Download,
                value = mediaSize ?: measuring, summary = mediaSize?.let { "$it. Tap to browse and delete." } ?: measuring,
                onClick = onOpenSavedMedia
            ),
            SettingsActionTile(
                id = "cachedImages", name = "Cached images", title = "Cached images", icon = SettingsIcons.Image,
                value = imageCacheSize ?: measuring, summary = imageCacheSize?.let { "$it. Tap to delete." } ?: measuring,
                onClick = viewModel::clearImageCache
            )
        ),
        ACCOUNTS_FILE to listOf(
            SettingsActionTile(
                id = "export", name = "Export", title = "Export accounts", icon = SettingsIcons.Upload,
                value = "To a file", summary = "Save the accounts you follow, and their folders, to a file.",
                onClick = { exporter.launch("yid-accounts.json") }
            ),
            SettingsActionTile(
                id = "import", name = "Import", title = "Import accounts", icon = YidIcons.Download,
                value = "From a file", summary = "From a Yiḍ export, or a text file of handles or bsky.app links.",
                onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            )
        ),
        ABOUT to listOf(
            // Dialer's About "Guide" tile (YAZ/Dialer feature/settings/SettingsScreen.kt, id "guide"):
            // the same first tile in every app. The guide opens over Settings and shows the current choices,
            // so welcomeSeen is kept instead of reset as Dialer does.
            SettingsActionTile(
                id = "guide", name = "Guide", title = "Guide", icon = YidIcons.Play,
                value = setup.leftText(), summary = "See the welcome pages again",
                onClick = onOpenWelcome
            ),
            SettingsActionTile(
                id = "updates", name = "Updates", title = "Updates", icon = SettingsIcons.Update,
                value = updatesLabel(settings.updates), summary = updatesLabel(settings.updates),
                onClick = { dialog = OpenDialog.UPDATES }
            ),
            // Without Store the app always updates itself: the switch says so and is locked.
            SettingsSwitchTile(
                id = "self", name = "Update by itself", title = "Update by itself", icon = SettingsIcons.Update,
                summary = when {
                    !present -> "Store is not installed"
                    !own -> "Store updates this app"
                    else -> "Turn off to let Store update Yiḍ."
                },
                checked = !present || own, enabled = present,
                onChange = { on ->
                    haptics.toggle(on)
                    StoreLink.setSelfUpdates(context, on)
                    own = on
                }
            ),
            SettingsActionTile(
                id = "version", name = "Yiḍ", title = "Yiḍ ${BuildConfig.VERSION_NAME}", icon = YidIcons.Info,
                value = BuildConfig.VERSION_NAME, summary = "Read public Bluesky posts with no account, no tracking and no ads."
            ),
            SettingsActionTile(
                id = "source", name = "Source code", title = "Source code", icon = SettingsIcons.Code,
                value = "github.com/213YaZ786/Yid", summary = "github.com/213YaZ786/Yid",
                onClick = { uriHandler.openUri("https://github.com/213YaZ786/Yid") }
            ),
            SettingsActionTile(
                id = "thanks", name = "Thanks", title = "Thanks", icon = YidIcons.Heart,
                value = "Bluesky", summary = "Made possible by Bluesky's open protocol and its public API."
            )
        ),
        // For reporting a problem, not for everyday use: its own section, at the very end.
        TROUBLESHOOTING to listOf(
            SettingsActionTile(
                id = "log", name = "Activity log", title = "Activity log", icon = SettingsIcons.Code,
                value = "Open", summary = "What Yiḍ did lately, to report a problem.",
                onClick = onOpenDebugLog
            )
        )
    )

    // One section's page: its name, then its tiles.
    val page: @Composable ColumnScope.(SettingsSection) -> Unit = { which ->
        SettingsSectionTitle(which.title)
        val list: List<SettingsTile> = tiles[which].orEmpty()
        if (list.isNotEmpty()) SettingsTileGrid(list) { tileId = it.id }
    }

    // The window decides: a list and a page side by side from Medium up, as a phone held upright
    // keeps one page. The panes take the whole width, so the readable inset is dropped; the side dock
    // still needs its room when the readable column no longer clears it (see YidApp's clearsPill).
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = WidthClass.of(maxWidth) != WidthClass.COMPACT
        val inset = LocalReadableInset.current
        val dockRoom = if (wide && inset >= SideDockClearance) SideDockClearance else 0.dp
        val dockPadding = LocalDockPadding.current
        CompositionLocalProvider(LocalReadableInset provides if (wide) 0.dp else inset) {
            Box(
                Modifier.fillMaxSize().padding(start = dockRoom)
                    .nestedScroll(topBarScroll.nestedScrollConnection)
            ) {
                FloatingFrame(
                    bottom = 0.dp,
                    scroll = if (wide) detailScroll else compactScroll,
                    // The content runs whole under the window's top while the row folds.
                    softEdge = false,
                    // "Settings" small, bold and centred on the top line, no large title: the sections start 16 dp under it.
                    top = {
                        Box(
                            Modifier
                                // The row and the status bar's height, so it leaves the window whole.
                                .onSizeChanged { topBarScroll.state.heightOffsetLimit = -(it.height + statusTop).toFloat() }
                                .offset { IntOffset(0, topBarScroll.state.heightOffset.roundToInt()) }
                        ) { FloatingTop("Settings") }
                    }
                ) { padding ->
                    if (wide) {
                        SettingsListDetail(
                            // SettingsListDetail adds its own 24 dp: 8 dp less in the header gives the 16.
                            padding = PaddingValues(top = (padding.calculateTopPadding() - 8.dp).coerceAtLeast(0.dp)),
                            sections = SECTIONS,
                            section = section,
                            onSection = { sectionId = it.id },
                            detailScroll = detailScroll,
                            detail = { page(section) }
                        )
                    } else {
                        // Scrolls at full width, rows pushed in by the readable inset, so the
                        // margins of a tablet scroll like the rest. See ReadableScroll.
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(compactScroll)
                                .padding(horizontal = LocalReadableInset.current)
                        ) {
                            SettingsHeaderSpacer(padding)
                            Spacer(Modifier.height(16.dp))
                            SettingsFoldedSections(SECTIONS, open = openSection, onOpen = { openId = it?.id.orEmpty() }) { which ->
                                val list: List<SettingsTile> = tiles[which].orEmpty()
                                if (list.isNotEmpty()) SettingsTileGrid(list) { tileId = it.id }
                            }
                            Spacer(Modifier.height(24.dp))
                            // The floating dock covers the bottom: the last section stays above it.
                            Spacer(Modifier.height(dockPadding))
                        }
                    }
                }
            }
        }
    }

    tiles.values.flatten().firstOrNull { it.id == tileId }?.let { tile ->
        SettingsTileDialogs(tile, onDismiss = { tileId = "" })
    }

    when (dialog) {
        OpenDialog.LAUNCH_REFRESH -> RefreshDialog(
            selected = settings.launchRefresh,
            onPick = viewModel::setSyncFrequency,
            onDismiss = { dialog = OpenDialog.NONE }
        )
        OpenDialog.START_TAB -> ChoiceDialog(
            title = "Start on",
            options = StartTab.entries.map { it to startTabLabel(it) },
            selected = settings.startTab,
            onSelect = viewModel::setStartTab,
            onDismiss = { dialog = OpenDialog.NONE }
        )
        OpenDialog.THEME -> ChoiceDialog(
            title = "Theme",
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = settings.themeMode,
            onSelect = viewModel::setTheme,
            onDismiss = { dialog = OpenDialog.NONE }
        )
        OpenDialog.TEXT_SIZE -> ChoiceDialog(
            title = "Text size",
            options = TEXT_SCALES.map { it to textScaleLabel(it) },
            selected = settings.textScale,
            onSelect = viewModel::setTextScale,
            onDismiss = { dialog = OpenDialog.NONE }
        )
        OpenDialog.UPDATES -> ChoiceDialog(
            title = "Updates",
            options = UpdateMode.entries.map { it to updatesLabel(it) },
            selected = settings.updates,
            onSelect = { mode ->
                viewModel.setUpdates(mode)
                // Installing needs Android's leave, asked when chosen.
                if (mode == UpdateMode.INSTALL && !Updates.canInstall(context)) Updates.allowInstalls(context)
            },
            onDismiss = { dialog = OpenDialog.NONE }
        )
        OpenDialog.AUTO_DOWNLOAD -> ChoiceDialog(
            title = "Save media automatically",
            options = AutoDownload.entries.map { it to autoDownloadLabel(it) },
            selected = settings.autoDownloadMedia,
            onSelect = { choice ->
                viewModel.setAutoDownloadMedia(choice)
                if (choice != AutoDownload.OFF) askForDownloadNotifications()
            },
            onDismiss = { dialog = OpenDialog.NONE }
        )
        OpenDialog.KEEP -> ChoiceDialog(
            title = "Keep posts",
            options = KEEP_DAYS.map { it to keepLabel(it) },
            selected = settings.keepPostsDays,
            onSelect = viewModel::setKeepPostsDays,
            onDismiss = { dialog = OpenDialog.NONE }
        )
        OpenDialog.FREQUENCY -> ChoiceDialog(
            title = "Check for new posts",
            options = INTERVALS.map { it to intervalLabel(it) },
            selected = settings.syncIntervalMinutes,
            onSelect = viewModel::setInterval,
            onDismiss = { dialog = OpenDialog.NONE }
        )
        OpenDialog.CLEAR -> ZoneAlertDialog(
            onDismissRequest = { dialog = OpenDialog.NONE },
            title = { Text("Clear saved posts?") },
            text = {
                Text("Posts saved on this phone will be deleted. They load again the next time you open an account.")
            },
            confirmButton = {
                BoldButton(filled = true, onClick = {
                    viewModel.clearSavedPosts()
                    dialog = OpenDialog.NONE
                }) { Text("Clear") }
            },
            dismissButton = {
                QuietButton(onClick = { dialog = OpenDialog.NONE }) { Text("Cancel") }
            }
        )
        OpenDialog.NONE -> Unit
    }
}

/**
 * The choice dialog of Refresh at launch, as ChoiceDialog draws its frame (ZoneAlertDialog, title, a centred line, one
 * button): the line says the stop, the glass slider moves over the nine stops and the value is saved as it moves.
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/settings/SettingsScreen.kt#RefreshDialog
 * not ported: ChoiceDialog's option capsules, Tick, danger and icon (a slider takes their place), its Cancel button
 * (nothing is pending, Done closes)
 */
@Composable
internal fun RefreshDialog(selected: SyncFrequency, onPick: (SyncFrequency) -> Unit, onDismiss: () -> Unit) {
    val options = SyncFrequency.entries
    ZoneAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Refresh at launch") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    selected.line,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                GlassSlider(
                    value = selected.ordinal.toFloat(),
                    onValueChange = { onPick(options[it.toInt().coerceIn(0, options.lastIndex)]) },
                    valueRange = 0f..options.lastIndex.toFloat(),
                    steps = options.size - 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }
        },
        confirmButton = { QuietButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    ZoneAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassRadio(selected = value == selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            QuietButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun startTabLabel(tab: StartTab): String = when (tab) {
    StartTab.HOME -> "Home"
    StartTab.ACCOUNTS -> "Accounts"
    StartTab.LAST -> "Last tab used"
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Same as the system"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private val INTERVALS = listOf(15, 30, 60, 180, 360)

private fun intervalLabel(minutes: Int): String = when {
    minutes < 60 -> "Every $minutes minutes"
    minutes == 60 -> "Every hour"
    else -> "Every ${minutes / 60} hours"
}

/**
 * Whether Android sends bsky.app links to Yiḍ. They can only be enabled by
 * the reader, since only Bluesky could verify the domain, so this reads the
 * reader's choice rather than a verification.
 */
private fun blueskyLinksEnabled(context: Context): Boolean {
    val manager = context.getSystemService(DomainVerificationManager::class.java) ?: return false
    val state = runCatching { manager.getDomainVerificationUserState(context.packageName) }
        .getOrNull() ?: return false
    if (!state.isLinkHandlingAllowed) return false
    val hostState = state.hostToStateMap["bsky.app"]
    return hostState == DomainVerificationUserState.DOMAIN_STATE_SELECTED ||
        hostState == DomainVerificationUserState.DOMAIN_STATE_VERIFIED
}

/** The system screen where supported links are switched on for this app. */
private fun openLinkSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )
    runCatching { context.startActivity(intent) }
}
