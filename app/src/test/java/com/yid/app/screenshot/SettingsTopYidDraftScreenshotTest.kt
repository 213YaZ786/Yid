package com.yid.app.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.navigation.WidthClass
import com.yid.app.ui.component.FloatingFrame
import com.yid.app.ui.component.FloatingTop
import com.yid.app.ui.component.GlassSwitch
import com.yid.app.ui.component.ScreenBanner
import com.yid.app.ui.component.SettingsActionTile
import com.yid.app.ui.component.SettingsFoldedSections
import com.yid.app.ui.component.SettingsHeaderSpacer
import com.yid.app.ui.component.SettingsListDetail
import com.yid.app.ui.component.SettingsSection
import com.yid.app.ui.component.SettingsSectionTitle
import com.yid.app.ui.component.SettingsSwitchTile
import com.yid.app.ui.component.SettingsTileGrid
import com.yid.app.ui.component.SideDockClearance
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.ui.icon.YidIcons
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DRAFT for the user's yes (requests.md point 4, also 8: one text size), nothing of app/src/main changes: the top of
 * Settings. settings_top_now is today's: feature/settings/SettingsScreen.kt's column, ScreenBanner("Settings") then every
 * section as a title over one zone of rows (SettingRow, SwitchRow), here Appearance, Media and Reading with fixed data.
 * settings_top_draft is Yiḍ's real sections as the kit's SettingsTiles under "Settings" small, bold and centred on the top
 * line, as FloatingTop draws it, the sections' list right under it, 16 dp below (24 dp for the sections' titles today). On a
 * phone the sections are folded as the screen opens, on a wide window SettingsListDetail lists them and shows the first
 * one's page, Appearance, as tiles (Yiḍ's seven rows of it: two choices and five switches, fixed values). On a wide window
 * SettingsListDetail keeps its own 24 dp spacer, so the draft gives it a header 8 dp shorter to show the 16 dp.
 * The screen is SettingsScreen's frame, a faithful copy with the sections folded as the screen opens (a phone) and the
 * first section open (a tablet), as LinkedOut's draft of the same point:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SettingsTopLinkedOutDraftScreenshotTest.kt#SettingsTopLinkedOutDraftScreenshotTest
 * not ported: SettingsScreen's view model, every tile but Appearance's and its dialogs (ChoiceDialog, the tile explain
 * dialogs), the toast, the storage measures, the dock padding, the section state saved across a turn, the folding top row
 * (topBarScroll, softEdge off); LinkedOut's LargeTitle("Settings") and its largeTitle parameter of FloatingFrame (Yiḍ's
 * today has the ScreenBanner, drawn in [NowScreen], not a large title); its Updates and Storage tiles (a later pass of the
 * point: Updates through StoreLink)
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class SettingsTopYidDraftScreenshotTest : ScreenshotBase() {

    // The groups of settings, as SettingsScreen lists them (Section("...") there), Troubleshooting last as its own zone.
    private val appearance = SettingsSection("appearance", "Appearance", DraftIcons.Palette)
    private val sections = listOf(
        appearance,
        SettingsSection("media", "Media", DraftIcons.Image),
        SettingsSection("reading", "Reading", YidIcons.Comment),
        SettingsSection("background", "Background updates", YidIcons.Refresh),
        SettingsSection("storage", "Storage", YidIcons.Folder),
        SettingsSection("accounts", "Accounts file", DraftIcons.Upload),
        SettingsSection("about", "About", YidIcons.Info),
        SettingsSection("troubleshooting", "Troubleshooting", DraftIcons.Warning)
    )

    // Appearance's rows, as SettingsScreen builds them (fixed values).
    private val tiles = listOf(
        SettingsActionTile(
            id = "theme", name = "Theme", title = "Theme", icon = DraftIcons.Palette,
            value = "System", summary = "System", onClick = {}
        ),
        SettingsSwitchTile(
            id = "black", name = "Pure black", title = "Pure black", icon = DraftIcons.Night,
            summary = "Deeper blacks in dark mode. Easier on the battery with OLED screens.",
            checked = false, onChange = {}
        ),
        SettingsSwitchTile(
            id = "glass", name = "Glass effects", title = "Glass effects", icon = DraftIcons.AutoAwesome,
            summary = "Zones and floating buttons in liquid glass, over a soft light in your wallpaper's colours.",
            checked = true, onChange = {}
        ),
        SettingsSwitchTile(
            id = "counts", name = "Counts", title = "Show counts", icon = YidIcons.Heart,
            summary = "Replies, reposts, quotes and likes under each post.",
            checked = true, onChange = {}
        ),
        SettingsActionTile(
            id = "textSize", name = "Text size", title = "Text size", icon = DraftIcons.FormatSize,
            value = "Normal", summary = "Normal, on top of Android's font size", onClick = {}
        ),
        SettingsSwitchTile(
            id = "compact", name = "Compact posts", title = "Compact posts", icon = DraftIcons.GridView,
            summary = "Tighter spacing, smaller avatars and shorter pictures. More posts on screen.",
            checked = false, onChange = {}
        ),
        SettingsSwitchTile(
            id = "square", name = "Square avatars", title = "Square avatars", icon = YidIcons.Person,
            summary = "Rounded squares instead of circles.",
            checked = false, onChange = {}
        )
    )

    @Test
    fun now() = capture("settings_top_now") { NowScreen() }

    @Test
    fun draft() = capture("settings_top_draft") { Settings(draft = true) }

    // Edge: the draft at 200 % text and on a 360 dp phone (font scale as Roborazzi's captureRoboImage, device by qualifiers as
    // Now in Android's captureMultiDevice)
    // https://github.com/takahirom/roborazzi/blob/main/roborazzi-compose/src/main/java/com/github/takahirom/roborazzi/RoborazziCompose.kt#captureRoboImage
    // https://github.com/android/nowinandroid/blob/main/core/screenshot-testing/src/main/kotlin/com/google/samples/apps/nowinandroid/core/testing/util/ScreenshotHelper.kt#captureMultiDevice
    @Test
    fun edge() = captureEdge("settings_top_draft") { Settings(draft = true) }

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SettingsTopLinkedOutDraftScreenshotTest.kt#Settings
    @Composable
    private fun Settings(draft: Boolean) {
        val compactScroll = rememberScrollState()
        val detailScroll = rememberScrollState()
        // The gap between the header and the list: 24 dp for the section titles today, 16 dp in the draft.
        val gap: Dp = if (draft) 16.dp else 24.dp
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = WidthClass.of(maxWidth) != WidthClass.COMPACT
            val inset = LocalReadableInset.current
            val dockRoom = if (wide && inset >= SideDockClearance) SideDockClearance else 0.dp
            CompositionLocalProvider(LocalReadableInset provides if (wide) 0.dp else inset) {
                Box(Modifier.fillMaxSize().padding(start = dockRoom)) {
                    FloatingFrame(
                        bottom = 0.dp,
                        scroll = if (wide) detailScroll else compactScroll,
                        top = { FloatingTop("Settings") }
                    ) { padding ->
                        if (wide) {
                            SettingsListDetail(
                                // SettingsListDetail adds its own 24 dp: 8 dp less in the header gives the draft's 16.
                                padding = if (draft) PaddingValues(top = (padding.calculateTopPadding() - 8.dp).coerceAtLeast(0.dp)) else padding,
                                sections = sections,
                                section = appearance,
                                onSection = {},
                                detailScroll = detailScroll,
                                detail = {
                                    SettingsSectionTitle(appearance.title)
                                    SettingsTileGrid(tiles) {}
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(compactScroll)
                                    .padding(horizontal = LocalReadableInset.current)
                            ) {
                                SettingsHeaderSpacer(padding)
                                Spacer(Modifier.height(gap))
                                SettingsFoldedSections(sections, open = null, onOpen = {}) {}
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Today's top, SettingsScreen's column: the status bar's room, ScreenBanner("Settings"), then each section as a centred
     * title over one zone of rows. Appearance, Media and Reading, with the texts of SettingsScreen.
     * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/settings/SettingsScreen.kt#SettingsScreen
     */
    @Composable
    private fun NowScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalReadableInset.current)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            ScreenBanner(title = "Settings")

            Section("Appearance") {
                SettingRow(title = "Theme", summary = "System", onClick = {})
                SwitchRow(
                    title = "Pure black",
                    summary = "Deeper blacks in dark mode. Easier on the battery with OLED screens.",
                    checked = false
                )
                SwitchRow(
                    title = "Glass effects",
                    summary = "Zones and floating buttons in liquid glass, over a soft light in your wallpaper's colours.",
                    checked = true
                )
                SwitchRow(title = "Show counts", summary = "Replies, reposts, quotes and likes under each post.", checked = true)
                SettingRow(title = "Text size", summary = "Normal, on top of Android's font size", onClick = {})
                SwitchRow(
                    title = "Compact posts",
                    summary = "Tighter spacing, smaller avatars and shorter pictures. More posts on screen.",
                    checked = false
                )
                SwitchRow(title = "Square avatars", summary = "Rounded squares instead of circles.", checked = false)
            }

            Section("Media") {
                SwitchRow(
                    title = "Media on Wi-Fi only",
                    summary = "On mobile data, pictures and videos wait for a tap. Avatars still load.",
                    checked = false
                )
                SettingRow(title = "Save media automatically", summary = "Never", onClick = {})
                SwitchRow(
                    title = "Play videos automatically",
                    summary = "Videos and GIFs play silently in the list as you scroll, and start " +
                        "when opened. Never on mobile data while Wi-Fi only is on.",
                    checked = true
                )
                SwitchRow(title = "Start videos muted", summary = "Sound stays off until you tap the speaker.", checked = true)
            }

            Section("Reading") {
                SettingRow(title = "Start on", summary = "Home", onClick = {})
                SettingRow(
                    title = "Open Bluesky links in Yiḍ",
                    summary = "Off. Turn on \"Open supported links\" and add the bsky.app link. " +
                        "Sharing a link to Yiḍ works either way.",
                    onClick = {}
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    /**
     * SettingsScreen's Section: a titled group of rows on one rounded card.
     * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/settings/SettingsScreen.kt#Section
     */
    @Composable
    private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 10.dp)
        )
        ZoneSurface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(content = content)
        }
    }

    /**
     * SettingsScreen's SettingRow, without its tap (a picture is still).
     * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/settings/SettingsScreen.kt#SettingRow
     * not ported: SettingRow's haptics, its clickable and its `enabled` and `quiet` parameters
     */
    @Composable
    private fun SettingRow(
        title: String,
        summary: String?,
        onClick: (() -> Unit)?,
        trailing: (@Composable () -> Unit)? = null
    ) {
        ListItem(
            headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
            supportingContent = summary?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            trailingContent = trailing,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }

    /**
     * SettingsScreen's SwitchRow: a SettingRow with the glass switch at its end.
     * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/settings/SettingsScreen.kt#SwitchRow
     */
    @Composable
    private fun SwitchRow(title: String, summary: String, checked: Boolean) {
        SettingRow(
            title = title,
            summary = summary,
            onClick = {},
            trailing = { GlassSwitch(checked = checked, onCheckedChange = {}) }
        )
    }
}
