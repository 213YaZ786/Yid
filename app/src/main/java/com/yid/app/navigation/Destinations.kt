package com.yid.app.navigation

import android.net.Uri

import androidx.compose.ui.graphics.vector.ImageVector
import com.yid.app.ui.icon.YidIcons

/** The tabs, in dock order. */
enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    TIMELINE("timeline", "Home", YidIcons.Home),
    ACCOUNTS("accounts", "Accounts", YidIcons.Person),
    SETTINGS("settings", "Settings", YidIcons.Settings)
}

/** Destinations pushed on top, not part of the bar. */
object Routes {
    /** The three tabs, hosted together in one pager. */
    const val MAIN = "main"
    const val FEED_PATTERN = "feed/{handle}"
    const val DEBUG_LOG = "debuglog"
    const val SEARCH = "search"
    const val SAVED_MEDIA = "savedmedia"
    const val FOLDERS = "folders"

    const val POST_PATTERN = "post/{id}?from={from}"

    fun feed(handle: String): String = "feed/$handle"

    /**
     * [id] is the post's AT URI, which holds slashes and a colon, so it is
     * encoded to stay one path segment. [from] is the account whose cache
     * holds the post, a lookup hint.
     */
    fun post(id: String, from: String?): String {
        val encoded = Uri.encode(id)
        return if (from.isNullOrBlank()) "post/$encoded" else "post/$encoded?from=${Uri.encode(from)}"
    }
}
