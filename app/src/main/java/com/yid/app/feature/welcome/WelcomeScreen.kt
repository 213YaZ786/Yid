/*
 * The welcome guide as tiles, after FlorisBoard's SetupScreen: every step is read from the system (what is enabled,
 * selected, allowed) and shown done or not, the tiles are Modules' SetupTiles. Yiḍ's own settings logic of the former
 * pager (media choice, new post alerts) is kept as it was.
 *
 * Copyright (C) 2021-2025 The FlorisBoard Contributors, Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0), translated from
 * https://github.com/florisboard/florisboard/blob/main/app/src/main/kotlin/dev/patrickgold/florisboard/app/setup/SetupScreen.kt
 */
package com.yid.app.feature.welcome

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.yid.app.core.system.BatteryExemption
import com.yid.app.data.settings.AutoDownload
import com.yid.app.data.settings.SettingsStore
import com.yid.app.feature.settings.SettingsViewModel
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.ChoiceDialog
import com.yid.app.ui.component.LoadingMark
import com.yid.app.ui.component.SetupTile
import com.yid.app.ui.component.SetupTileDialog
import com.yid.app.ui.component.SetupTileGrid
import com.yid.app.ui.component.leftText
import com.yid.app.ui.component.requiredDone
import com.yid.app.ui.component.requiredLeft
import com.yid.app.ui.icon.YidIcons
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// not ported: WelcomePage and PAGES (the pager's pages: their texts moved into the tiles' summaries)
// not ported: PageContent, Dots, Point, LinkExample, accentDisc (the pages' own drawing: the pager is gone)
// not ported: MediaChoice, Choices, Option (rows of the pager's pages: the tiles open the app's ChoiceDialog instead)
// not ported: the Updates, Find someone, Follow an account and Sort into folders pages (updates stay on Install, the preset; following and folders are done in Accounts)
// not ported: awaitTransition (FlorisBoard's wait on a Flow of the system's change: each state is read again at every resume)
// not ported: flow (FlorisBoard's Flow of step changes: the tiles are read again at every resume instead)
// not ported: Steps (FlorisBoard's sealed step ids: a tile carries its own string id)
// not ported: SetupLinks (FlorisBoard's links of its footer: Yiḍ's guide has none)
// not ported: SetupScreen (the name only: its header, steps and footer are WelcomeContent, its state logic rememberSetupTiles)

/**
 * The few choices worth making, the best one already set, as tiles: each is read again each time the screen comes
 * back (SetupScreen reads isFlorisBoardEnabled, isFlorisBoardSelected and hasNotificationPermission, and sets the
 * current step from them), and turns done by itself. [onFinish] receives true when the reader asks for Accounts.
 * Ported from rememberSetupTiles of Language
 * https://github.com/213YaZ786/Language/blob/main/app/src/main/java/com/yaz/language/feature/welcome/WelcomeScreen.kt#rememberSetupTiles
 * with the settings logic of Yiḍ's former WelcomeScreen
 * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/welcome/WelcomeScreen.kt#WelcomeScreen
 * Only the choices the reader must make (the user, 2026-11-06: a tile only for an important call, the best preset
 * for the rest): Save media (spends data, required) and New posts (a permission and checks with the app closed).
 * Updates stay on Install through Store, following is done in Accounts.
 */
@Composable
fun rememberSetupTiles(onFinish: (openAccounts: Boolean) -> Unit): List<SetupTile> {
    val settings: SettingsStore = koinInject()
    val context = LocalContext.current
    var checks by remember { mutableIntStateOf(0) }

    // On first launch nothing is picked: this is the only setting that spends
    // the reader's data without asking again, so it is not left as a default
    // they never saw. Reopened from Settings, the current choice is shown.
    // LinkedOut starts blank every time, which locked the last button for a
    // reader who had already chosen.
    var chosen by remember {
        mutableStateOf(settings.current.takeIf { it.welcomeSeen }?.autoDownloadMedia)
    }
    // New post notifications start from the current settings and ask for
    // the permission they need when turned on, not before.
    val sync: SettingsViewModel = koinViewModel()
    var notify by remember { mutableStateOf(settings.current.backgroundSync && settings.current.notifyNewPosts) }
    LifecycleResumeEffect(Unit) {
        checks++
        notify = settings.current.backgroundSync && settings.current.notifyNewPosts
        onPauseOrDispose { }
    }
    // The choice dialog open ("media"), "" when none.
    var choosing by rememberSaveable { mutableStateOf("") }

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { checks++ }
    val choose: (AutoDownload) -> Unit = { choice ->
        chosen = choice
        // Same as the Settings dialog: the watermark restarts, so what is on
        // screen now is saved on the next refresh.
        settings.update { it.copy(autoDownloadMedia = choice, autoDownloadedUntilMillis = 0) }
        // The progress line of a batch is a notification, and Android 13 and
        // later want the permission for it. Asked here, where saving was asked for.
        if (choice != AutoDownload.OFF &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val turnOnNotify = {
        notify = true
        sync.setBackgroundSync(true)
        sync.setNotifyNewPosts(true)
        // Checks run with the app closed: see BatteryExemption.
        BatteryExemption.request(context)
    }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) turnOnNotify() else notify = false
    }
    val chooseNotify: (Boolean) -> Unit = { on ->
        when {
            !on -> {
                notify = false
                sync.setNotifyNewPosts(false)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED -> askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> turnOnNotify()
        }
    }

    if (choosing == "media") {
        ChoiceDialog(
            title = "Save media",
            options = listOf(
                AutoDownload.OFF to "Never",
                AutoDownload.UNMETERED to "On Wi-Fi only",
                AutoDownload.ANY to "On Wi-Fi or mobile data"
            ),
            onPick = choose,
            onDismiss = { choosing = "" },
            selected = chosen
        )
    }
    return listOf(
        SetupTile(
            id = "media", name = "Save media", title = "Save media automatically", icon = YidIcons.Download,
            state = when (chosen) {
                null -> "Not chosen"
                AutoDownload.OFF -> "Never"
                AutoDownload.UNMETERED -> "On Wi-Fi only"
                AutoDownload.ANY -> "On Wi-Fi or mobile data"
            },
            summary = "Posts saved on the phone open again offline. It spends data without asking again, so choose: " +
                "never, on Wi-Fi only, or on Wi-Fi or mobile data. Changeable in Settings.",
            done = chosen != null, required = true,
            onClick = { choosing = "media" }
        ),
        SetupTile(
            id = "notify", name = "New posts", title = "New posts", icon = YidIcons.Bell,
            state = if (notify) "On" else "Off",
            summary = "A notification when followed accounts post. Checks run with the app closed, so Android's " +
                "notification permission and battery exemption are asked when it is turned on. Changeable in Settings.",
            done = notify, required = false,
            onClick = { chooseNotify(!notify) }
        )
    )
}

/**
 * The guide shown on first launch, and again from Settings: the logo, the title, how many steps are left, the tiles,
 * then Start (Done when reopened), which waits for the required ones. [onFinish] receives true when the reader asks
 * to go to Accounts. Ported from SetupScreen#SetupScreen of
 * https://github.com/florisboard/florisboard/blob/main/app/src/main/kotlin/dev/patrickgold/florisboard/app/setup/SetupScreen.kt
 * through Language's
 * https://github.com/213YaZ786/Language/blob/main/app/src/main/java/com/yaz/language/feature/welcome/WelcomeScreen.kt#WelcomeScreen
 * and Yiḍ's former
 * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/welcome/WelcomeScreen.kt#WelcomeScreen
 */
@Composable
fun WelcomeScreen(onFinish: (openAccounts: Boolean) -> Unit) {
    val settings: SettingsStore = koinInject()
    val reopened = remember { settings.current.welcomeSeen }
    val steps = rememberSetupTiles(onFinish)
    // The tile whose long press is open (its id), "" when none.
    var explained by rememberSaveable { mutableStateOf("") }

    WelcomeContent(steps, reopened, { onFinish(false) }) { explained = it.id }

    steps.firstOrNull { it.id == explained }?.let { tile ->
        SetupTileDialog(tile, onDismiss = { explained = "" })
    }
}

/**
 * What the guide draws from the tiles it is given: the layout of SetupScreen (header, steps, footer with the one
 * button), split from [WelcomeScreen] as Language's. Ported from
 * https://github.com/213YaZ786/Language/blob/main/app/src/main/java/com/yaz/language/feature/welcome/WelcomeScreen.kt#WelcomeContent
 * and
 * https://github.com/florisboard/florisboard/blob/main/app/src/main/kotlin/dev/patrickgold/florisboard/app/setup/SetupScreen.kt#SetupScreen
 */
@Composable
internal fun WelcomeContent(tiles: List<SetupTile>, reopened: Boolean, onStart: () -> Unit, onExplain: (SetupTile) -> Unit = {}) {
    val steps = tiles
    // Drawn over the whole window, so it keeps clear of the system bars itself.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(Modifier.height(40.dp))
            // The old guide's mark, turning at its size (https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/welcome/WelcomeScreen.kt#PageContent).
            LoadingMark(size = 160.dp)
            Spacer(Modifier.height(20.dp))
            // Every title in the accent (the user, 2026-10-31), as Modules' SettingsSectionTitle.
            Text("Welcome to Yiḍ", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(steps.leftText(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            SetupTileGrid(steps, onExplain)

            Spacer(Modifier.height(32.dp))
            val ready = steps.requiredDone()
            BoldButton(filled = true, enabled = ready, onClick = onStart) { Text(if (reopened) "Done" else "Start") }
            if (!ready) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Still needed: " + steps.requiredLeft().joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
