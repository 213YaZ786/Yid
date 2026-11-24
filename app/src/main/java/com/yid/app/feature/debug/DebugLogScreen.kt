package com.yid.app.feature.debug

import androidx.compose.ui.graphics.Color
import com.yid.app.feature.BarAction
import com.yid.app.ui.component.BarTitle
import com.yid.app.ui.component.FloatingTopBar
import com.yid.app.ui.component.plus
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import com.yid.app.ui.component.navigationBarBottom
import androidx.compose.foundation.shape.RoundedCornerShape
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.ui.component.rememberHaptics
import com.yid.app.ui.component.BoldButton
import com.yid.app.navigation.LocalReadableInset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.yid.app.core.debug.LogExporter
import com.yid.app.core.debug.RequestLog
import com.yid.app.ui.icon.YidIcons
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Every request and its result, exportable as a text file.
 *
 * The point is that a bug report can carry evidence rather than a description.
 * Nothing here leaves the device unless you export it and send it yourself.
 *
 * Ported from LinkedOut's DebugLogScreen
 * (https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/debug/DebugLogScreen.kt#DebugLogScreen):
 * the bar is the floating capsule, Back at the left, Clear at the right, the title once the two buttons have
 * scrolled away; it folds away going down and returns going up, the list full screen.
 * not ported: the lastBody flow, its "Save page" button and the "Last page received" line (LinkedIn's page, Bluesky answers JSON)
 * not ported: the entries' rank key and animateItem of the rows (point 18, the animations)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(onBack: () -> Unit) {
    val log: RequestLog = koinInject()
    val exporter: LogExporter = koinInject()
    val entries by log.entries.collectAsState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }
    val stamp = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // The page's ground is painted under the whole app, see MainActivity.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        // The status and navigation bars are the list's own, in its content padding.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FloatingTopBar(
                scrollBehavior = scrollBehavior,
                title = { if (headerGone) BarTitle("Activity log") },
                navigationIcon = { BarAction(YidIcons.ArrowBack, "Back", onBack) },
                actions = { BarAction(YidIcons.Delete, "Clear", log::clear) }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LocalReadableInset.current,
                end = LocalReadableInset.current,
                bottom = 12.dp + navigationBarBottom()
            ).plus(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BoldButton(onClick = {
                        clipboard.setText(AnnotatedString(log.render()))
                        haptics.done()
                        scope.launch { snackbar.showSnackbar("Log copied") }
                    }) { Text("Copy all") }

                    BoldButton(onClick = {
                        scope.launch {
                            val name = "yid-log-${System.currentTimeMillis()}.txt"
                            exporter.exportText(name, log.render())
                                .onSuccess { snackbar.showSnackbar("Saved to $it") }
                                .onFailure { snackbar.showSnackbar("Could not save the file") }
                        }
                    }) { Text("Save to Downloads") }
                }
            }

            if (entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Nothing recorded yet. Open an account or scroll for older posts, then " +
                            "come back here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                    )
                }
            } else {
                items(entries.reversed()) { entry ->
                    ZoneSurface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "${stamp.format(Date(entry.atMillis))}  ${entry.kind}  ${entry.outcome}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                entry.url,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val meta = buildList {
                                entry.httpStatus?.let { add("http $it") }
                                entry.durationMillis?.let { add("${it}ms") }
                                entry.bodyBytes?.let { add("$it bytes") }
                            }
                            if (meta.isNotEmpty()) {
                                // The details joined with a comma, never a dot or a dash (the readers' plain text rule).
                                Text(
                                    meta.joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            entry.detail?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
