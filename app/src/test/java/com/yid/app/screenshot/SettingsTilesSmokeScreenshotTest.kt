package com.yid.app.screenshot

import androidx.compose.foundation.layout.Column
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.ui.component.SettingsActionTile
import com.yid.app.ui.component.SettingsSwitchTile
import com.yid.app.ui.component.SettingsTile
import com.yid.app.ui.component.SettingsTileGrid
import com.yid.app.ui.icon.YidIcons
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The smoke test of the screenshot setup: the kit's tiles drawn with fixed data on a phone and a tablet, by day and
 * by night, so a first picture is recorded and compared. Ported from LinkedOut's SettingsScreenshotTest
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SettingsScreenshotTest.kt#SettingsScreenshotTest
 * (the capture itself is ScreenshotBase's).
 * not ported: SettingsScreenshotTest's Koin start and stop, tap, and the sections folded, appearance and about (they
 * draw LinkedOut's SettingsScreen through its view model; Yiḍ's Settings is not built on the tiles yet, so this draws
 * the kit's SettingsTileGrid alone)
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class SettingsTilesSmokeScreenshotTest : ScreenshotBase() {

    private val tiles: List<SettingsTile> = listOf(
        SettingsSwitchTile(
            id = "notifications", name = "New posts", title = "New post notifications", icon = YidIcons.Bell,
            summary = "Tell me when a followed account posts.", checked = true, onChange = {}
        ),
        SettingsSwitchTile(
            id = "media", name = "Save media", title = "Save media", icon = YidIcons.Download,
            summary = "Keep the pictures and videos of the posts on the phone.", checked = false, onChange = {}
        ),
        SettingsActionTile(
            id = "folders", name = "Folders", title = "Folders", icon = YidIcons.Folder, value = "3 folders",
            summary = "Sort the followed accounts into folders.", onClick = {}
        ),
        SettingsActionTile(
            id = "version", name = "Version", title = "Version", icon = YidIcons.Info, value = "0.5.26",
            summary = "The installed version."
        )
    )

    @Test
    fun tiles() = capture("settings_tiles_smoke") {
        Column { SettingsTileGrid(tiles, onExplain = {}) }
    }
}
