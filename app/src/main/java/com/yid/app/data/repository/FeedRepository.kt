package com.yid.app.data.repository

import com.yid.app.core.common.Outcome
import com.yid.app.core.model.Conversation
import com.yid.app.core.model.Feed
import com.yid.app.data.bsky.BskyApi

/**
 * Where an account's posts and a post's conversation come from. One source:
 * Bluesky's public API, which pages back through history for real, so there
 * is no choice of server to make and no fallback to try.
 */
class FeedRepository(private val api: BskyApi) {

    suspend fun loadFeed(handle: String, cursor: String? = null): Outcome<Feed> = api.feed(handle, cursor)

    /** A post's conversation. Never cached: replies change all the time. */
    suspend fun loadConversation(atUri: String): Outcome<Conversation> = api.thread(atUri)
}
