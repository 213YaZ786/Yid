package com.yid.app.feature.accounts

import com.yid.app.ui.component.GlassSearchField
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.core.link.LinkCleaner
import com.yid.app.ui.component.CleanLinkEffect
import com.yid.app.ui.component.rememberHaptics
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.yid.app.ui.component.AppBarWithSearch
import com.yid.app.ui.component.BoldIconButton
import com.yid.app.ui.component.CapsuleMenuOffset
import com.yid.app.ui.component.DropdownMenuGroup
import com.yid.app.ui.component.DropdownMenuItem
import com.yid.app.ui.component.FolderSwitch
import com.yid.app.ui.component.GlassMenu
import com.yid.app.ui.component.SearchCapsuleField
import com.yid.app.ui.component.SelectableDropdownMenuItem
import androidx.compose.ui.platform.LocalContext
import com.yid.app.core.link.PastedText
import android.widget.Toast
import android.content.ClipboardManager
import com.yid.app.core.model.Actor
import com.yid.app.ui.component.EmptyZone
import com.yid.app.ui.component.BoldButton
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.FolderDialog
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.AssistChip
import com.yid.app.ui.component.LocalDockPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yid.app.ui.component.Avatar
import com.yid.app.ui.component.relativeTime
import com.yid.app.ui.icon.YidIcons
import org.koin.androidx.compose.koinViewModel

/**
 * Accounts and search merged. One pill search bar on top: it filters the
 * accounts you follow, searches people on Bluesky by name, and when the text
 * is a handle or a link, a card offers to open that profile or follow it.
 * Typing never follows anyone.
 * Unfollowing happens on the profile, which keeps it away from a stray tap.
 */
@Composable
fun AccountsScreen(
    onOpenFeed: (String) -> Unit,
    onOpenFolders: () -> Unit,
    viewModel: AccountsViewModel = koinViewModel()
) {
    val rows by viewModel.rows.collectAsState()
    val folders by viewModel.folders.collectAsState()
    var filing by remember { mutableStateOf<AccountRow?>(null) }

    filing?.let { row ->
        FolderDialog(
            title = "File @${row.handle}",
            folders = folders,
            selected = row.folder,
            everything = null,
            onSelect = { name ->
                viewModel.setFolder(row.handle, name.orEmpty())
                filing = null
            },
            onDismiss = { filing = null }
        )
    }
    var query by rememberSaveable { mutableStateOf("") }
    // The folder chosen in the capsule's menu, null for All. A folder deleted since is All again.
    var chosenFolder by rememberSaveable { mutableStateOf<String?>(null) }
    val folder = chosenFolder?.takeIf { it in folders }
    var foldersOpen by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    val haptics = rememberHaptics()

    val people by viewModel.people.collectAsState()

    // Coming back from a profile may have brought new posts or an avatar.
    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(query) { viewModel.searchPeople(query) }

    val candidate = AccountsViewModel.asHandle(query)
    // A pasted link filters by the handle it names, so a link to an account
    // already followed finds that account instead of matching nothing.
    val trimmed = if ('/' in query) candidate.orEmpty() else query.trim().removePrefix("@")
    val alreadyFollowed = candidate != null &&
        rows.any { it.handle.equals(candidate, ignoreCase = true) }
    val inFolder = if (folder == null) rows else rows.filter { it.folder == folder }
    val visible = if (trimmed.isEmpty()) {
        inFolder
    } else {
        inFolder.filter {
            it.handle.contains(trimmed, ignoreCase = true) ||
                it.name?.contains(trimmed, ignoreCase = true) == true
        }
    }

    fun open(handle: String) {
        focus.clearFocus()
        onOpenFeed(handle)
    }

    // A detour link in the field, from a search engine or another site,
    // becomes the address it stands for; a pasted one then opens like any
    // other paste.
    var openWhenClean by remember { mutableStateOf(false) }
    CleanLinkEffect(
        text = query,
        onClean = { clean ->
            query = clean
            if (openWhenClean) {
                openWhenClean = false
                AccountsViewModel.asHandle(clean)?.let { open(it) }
            }
        },
        onUnreadable = {
            openWhenClean = false
            haptics.reject()
            Toast.makeText(context, "Google did not say where this link leads. Open it and copy the page address.", Toast.LENGTH_SHORT).show()
        }
    )

    /**
     * One tap: read the clipboard, put what it holds in the field, and open
     * the profile it names. A shared sentence goes through PastedText first,
     * so the link inside it is what lands in the field.
     */
    fun paste() {
        val clip = context.getSystemService(ClipboardManager::class.java)
            ?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
        val text = PastedText.query(clip) { "bsky.app/" in it }
        if (text.isEmpty()) {
            haptics.reject()
            Toast.makeText(context, "Nothing to paste. Copy a profile link or a handle first.", Toast.LENGTH_SHORT).show()
            return
        }
        query = text
        if (LinkCleaner.needsResolving(text)) {
            // Opened once Google has said where it leads, see CleanLinkEffect.
            openWhenClean = true
            focus.clearFocus()
            return
        }
        AccountsViewModel.asHandle(text)?.let { open(it) } ?: focus.clearFocus()
    }

    // The field's keyboard action and its two buttons, shared by the capsule's field and the lone field.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/accounts/AccountsScreen.kt#AccountsScreen
    // not ported: AccountsScreen (the screen itself is this file's, only its capsule with the folder switch and menu is
    // rebuilt here)
    // not ported: AccountKind, QueryKind, classify, explain, notice (LinkedIn's address kinds and the line under the field;
    // Bluesky has handles and the search of people, kept below)
    // not ported: confirmUnfollow and AccountCard's HoldMenu with Unfollow (the held card is another point; the unfollow
    // stays on the profile)
    // not ported: animateItem on the rows, the notice and the candidate (point 18, not now)
    // not ported: paste (LinkedOut's reads LinkedIn addresses; this file's own paste() above reads Bluesky links and stays)
    // not ported: open (LinkedOut's takes a kind too; Bluesky has one kind of account)
    val searchActions = KeyboardActions(onSearch = {
        when {
            candidate != null && !alreadyFollowed -> open(candidate)
            visible.size == 1 -> open(visible.first().handle)
            else -> focus.clearFocus()
        }
    })
    // The Clear icon grows in and out with the field's end, so the field does not jump when the first letter is typed.
    // https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedVisibilitySamples.kt#AnimatedVisibilityWithBooleanVisibleParamNoReceiver
    // not ported: AnimatedVisibilityWithBooleanVisibleParamNoReceiver's clickable Box toggling `visible`, its Alignment.Center
    // and its corner radius animated by `transition.animateDp` (the Clear icon is shown by the query, and has no corner)
    val fieldActions: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(tween(200)) + expandHorizontally(tween(200)),
            exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200))
        ) {
            BoldIconButton(onClick = { query = "" }) {
                Icon(YidIcons.Close, contentDescription = "Clear")
            }
        }
        // Always there, and the clipboard is read only on a tap: reading it to decide whether to show the button would be
        // a read too, and Android raises its notice on every one.
        BoldIconButton(onClick = { paste() }) {
            Icon(YidIcons.Paste, contentDescription = "Paste a link or a handle")
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // The capsule is the first row of the list, so the whole screen scrolls, under the status bar too.
            contentPadding = PaddingValues(
                start = 16.dp + LocalReadableInset.current,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                end = 16.dp + LocalReadableInset.current,
                bottom = 16.dp + LocalDockPadding.current
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // The capsule is the first row of the list. Nobody followed: the field alone is the first row, 8 dp under the
            // status bar as the capsule is. The folder choice is the capsule's navigation icon: a menu under it with the
            // folders and their counts, then Manage folders last (Gmail's Manage labels), the shown folder checked; the real
            // search field in its middle.
            // https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/SearchBar.kt#AppBarWithSearch
            // not ported: AppBarWithSearch (the app's AppBarWithSearch in ui/component/FeedBanner.kt is its port, used as it is)
            // not ported: DropdownMenuGroup (the app's ui/component/GlassMenu.kt is its port, used as it is)
            if (rows.isNotEmpty()) item(key = "header") {
                Box(Modifier.padding(top = 8.dp)) {
                    AppBarWithSearch(
                        inputField = {
                            SearchCapsuleField(
                                searchIcon = YidIcons.Search,
                                value = query,
                                onValueChange = { query = it },
                                hints = listOf("Search ${rows.size} accounts or open a handle", "Search ${rows.size} accounts"),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = searchActions
                            )
                        },
                        actions = { fieldActions() },
                        navigationIcon = {
                            // The wrapper is the screen's heading for TalkBack, the switch inside stays its own button.
                            Box(Modifier.semantics { heading(); contentDescription = "Accounts" }) {
                                Box {
                                    FolderSwitch(
                                        icon = YidIcons.Folder,
                                        trailingIcon = YidIcons.ExpandMore,
                                        label = folder ?: "All",
                                        expanded = foldersOpen,
                                        onClick = { foldersOpen = true }
                                    )
                                    GlassMenu(expanded = foldersOpen, onDismissRequest = { foldersOpen = false }, contentOffset = CapsuleMenuOffset) {
                                        DropdownMenuGroup(index = 0, count = 2) {
                                            SelectableDropdownMenuItem(
                                                selected = folder == null,
                                                onClick = { chosenFolder = null; foldersOpen = false },
                                                text = "All",
                                                count = rows.size
                                            )
                                            // With Main alone the folder row would repeat All, so folders come with the first
                                            // one the reader makes, as the filing chip does.
                                            if (folders.size > 1) {
                                                folders.forEach { name ->
                                                    SelectableDropdownMenuItem(
                                                        selected = folder == name,
                                                        onClick = { chosenFolder = name; foldersOpen = false },
                                                        text = name,
                                                        count = rows.count { it.folder == name }
                                                    )
                                                }
                                            }
                                        }
                                        DropdownMenuGroup(index = 1, count = 2) {
                                            DropdownMenuItem(
                                                text = "Manage folders",
                                                onClick = { foldersOpen = false; onOpenFolders() },
                                                leadingIcon = {
                                                    Icon(YidIcons.Tune, contentDescription = null, modifier = Modifier.size(24.dp))
                                                },
                                                trailingIcon = {
                                                    Icon(
                                                        YidIcons.ChevronRight,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
            // The capsule and the bare field are two items with two keys: one leaves while the other arrives at the same
            // place, so neither takes animateItem (the two would overlap and the rows below jump twice). Left as it is.
            if (rows.isEmpty()) item(key = "search") {
                // Nothing to see, but TalkBack still finds the screen's name (the old banner row's Box): 1 dp here, 7 dp of
                // padding below, so the field stays 8 dp down.
                Box(Modifier.size(1.dp).semantics { heading(); contentDescription = "Accounts" })
                GlassSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search people or paste a link",
                    leadingIcon = { Icon(YidIcons.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(YidIcons.Close, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = { paste() }) {
                                Icon(YidIcons.Paste, contentDescription = "Paste a link or a handle")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = searchActions,
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp)
                )
            }
            if (candidate != null && !alreadyFollowed) {
                item(key = "candidate") {
                    CandidateCard(
                        handle = candidate,
                        onOpen = { open(candidate) },
                        onFollow = { haptics.done(); viewModel.follow(candidate) }
                    )
                }
            } else if (trimmed.isNotEmpty() && candidate == null && visible.isEmpty() && people.isEmpty()) {
                item(key = "invalid") {
                    Hint("No one found yet. Search a name, or type a whole handle like jane.bsky.social.")
                }
            }

            items(visible, key = { it.handle }) { row ->
                AccountCard(
                    row = row,
                    onClick = { open(row.handle) },
                    // Only once the reader has made a folder. With Main alone
                    // the chip would name the one place everything is.
                    onFile = if (folders.size > 1) ({ filing = row }) else null
                )
            }

            // People from the search, those already followed left out: they
            // are in the list above.
            val newPeople = people.filterNot { actor -> rows.any { it.handle.equals(actor.handle, ignoreCase = true) } }
            if (newPeople.isNotEmpty()) {
                item(key = "people-title") {
                    Text(
                        "People on Bluesky",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                items(newPeople, key = { "person-${it.handle}" }) { actor ->
                    PersonCard(actor = actor, onOpen = { open(actor.handle) }, onFollow = { viewModel.follow(actor.handle) })
                }
            }

            if (rows.isEmpty() && trimmed.isEmpty()) {
                item(key = "empty") { EmptyState(Modifier.fillParentMaxHeight(0.7f)) }
            }
        }
    }
}

@Composable
private fun AccountCard(row: AccountRow, onClick: () -> Unit, onFile: (() -> Unit)?) {
    ZoneSurface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(url = row.avatarUrl, name = row.name ?: row.handle, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    row.name ?: "@${row.handle}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (row.name != null) {
                    Text(
                        "@${row.handle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    row.lastPostMillis?.let(::relativeTime) ?: "Not read yet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // The folder is a control, not a label: tapping the card opens
                // the account, tapping this files it.
                if (onFile != null) {
                    AssistChip(
                        onClick = onFile,
                        label = { Text(row.folder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(YidIcons.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateCard(handle: String, onOpen: () -> Unit, onFollow: () -> Unit) {
    ZoneSurface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        accent = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(url = null, name = handle, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    "@$handle",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Tap to read the profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            BoldButton(onClick = onFollow, filled = true) { Text("Follow") }
        }
    }
}

@Composable
private fun PersonCard(actor: Actor, onOpen: () -> Unit, onFollow: () -> Unit) {
    ZoneSurface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(url = actor.avatarUrl, name = actor.displayName ?: actor.handle, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    actor.displayName?.takeIf { it.isNotBlank() } ?: "@${actor.handle}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "@${actor.handle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BoldButton(onClick = onFollow) { Text("Follow") }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(24.dp)
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    EmptyZone(
        title = "No accounts yet",
        message = "Search a name or paste a profile link above, then follow to build your timeline. " +
            "The list stays on this phone. Only the reads go to Bluesky.",
        icon = YidIcons.Person,
        modifier = modifier
    )
}
