package com.yid.app.data.repository

import com.yid.app.core.common.Outcome
import com.yid.app.core.model.Conversation
import com.yid.app.core.model.Feed
import com.yid.app.data.bsky.BskyApi
import com.yid.app.data.read.ReadMarks

/**
 * Where an account's posts and a post's conversation come from. One source:
 * Bluesky's public API, which pages back through history for real, so there
 * is no choice of server to make and no fallback to try.
 */
class FeedRepository(private val api: BskyApi, private val marks: ReadMarks) {

    /**
     * Every page and refresh of Home and of a profile comes through here, so
     * what was published before the previous visit is marked read as it
     * arrives: an older page or a profile read again shows no NewDot. A folder
     * switch reads from disk and never reaches this. LinkedOut calls
     * markReadIfOld in its TimelineViewModel after the same two reads:
     * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/timeline/TimelineViewModel.kt
     */
    suspend fun loadFeed(handle: String, cursor: String? = null): Outcome<Feed> =
        api.feed(handle, cursor).also { outcome ->
            if (outcome is Outcome.Success) marks.markReadIfOld(outcome.value.posts.map { it.id to it.publishedAtMillis })
        }

    /** A post's conversation. Never cached: replies change all the time. */
    suspend fun loadConversation(atUri: String): Outcome<Conversation> = api.thread(atUri)
}
