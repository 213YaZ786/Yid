package com.yid.app.data.repository

import com.yid.app.core.common.AppError
import com.yid.app.core.common.Outcome
import com.yid.app.core.model.Feed
import com.yid.app.core.model.Post
import com.yid.app.data.accounts.AccountStore
import com.yid.app.data.cache.FeedCache
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Everything you follow, merged into one stream.
 *
 * Cache first, then network. The reader sees content immediately and watches it
 * update, rather than staring at a spinner while several instances are polled.
 * When a fetch fails the cached posts stay on screen and the failure is
 * reported per account, because losing one account is not losing the timeline.
 */
class TimelineRepository(
    private val accounts: AccountStore,
    private val feeds: FeedRepository,
    private val cache: FeedCache
) {

    data class Merged(
        val posts: List<Post> = emptyList(),
        val errors: Map<String, AppError> = emptyMap(),
        val fromCache: Boolean = false,
        val oldestFetchedAtMillis: Long? = null,
        val canLoadMore: Boolean = false
    )

    /** Null means every account, which is Home with no folder chosen. */
    private fun handlesIn(folder: String?): List<String> =
        accounts.accounts.value.filter { folder == null || it.folder == folder }.map { it.handle }

    /** Instant, offline, no network touched. [folder] as in [handlesIn]. */
    suspend fun cached(folder: String? = null): Merged {
        val handles = handlesIn(folder)
        if (handles.isEmpty()) return Merged()

        val loaded = handles.mapNotNull { cache.read(it) }
        return Merged(
            posts = merge(loaded.flatMap { it.posts }),
            fromCache = true,
            oldestFetchedAtMillis = loaded.minOfOrNull { it.fetchedAtMillis },
            canLoadMore = loaded.any { it.nextCursor != null }
        )
    }

    /**
     * Fetches followed accounts, a few at a time, and returns the whole merged
     * timeline.
     *
     * [only] narrows the fetch to those handles, lowercased, and leaves every
     * other account to its cache. Following one account then costs one read,
     * not a pass over the whole list against a fragile host. Null fetches all.
     *
     * The concurrency limit is the point. Firing twenty simultaneous requests at
     * a single surviving instance is the fastest way to get rate limited, and
     * the pool's backoff would then punish every later read.
     */
    suspend fun refresh(only: Set<String>? = null, folder: String? = null): Merged = coroutineScope {
        val handles = handlesIn(folder)
        if (handles.isEmpty()) return@coroutineScope Merged()

        val targets = if (only == null) handles else handles.filter { it.lowercase() in only }

        val results = inParallel(targets) { handle -> handle to feeds.loadFeed(handle) }

        val errors = mutableMapOf<String, AppError>()
        var oldest: Long? = null
        var more = false

        for ((handle, outcome) in results) {
            when (outcome) {
                is Outcome.Success -> {
                    // Merge rather than overwrite, so a refresh does not throw
                    // away every page the reader already scrolled through.
                    val merged = cache.append(outcome.value)
                    accounts.updateDisplayName(handle, outcome.value.displayName)
                    if (merged.nextCursor != null) more = true
                    oldest = minOf(oldest ?: outcome.value.fetchedAtMillis, outcome.value.fetchedAtMillis)
                }
                is Outcome.Failure -> {
                    errors[handle] = outcome.error
                    cache.read(handle)?.let { if (it.nextCursor != null) more = true }
                }
            }
        }

        // Read back from the cache rather than from this run's results, so the
        // merged view includes everything ever collected, not only what today's
        // fetch happened to return. This is what makes background polling
        // accumulate history instead of replacing it. The list is read again
        // here, so an account unfollowed during the fetch does not come back.
        val stored = handlesIn(folder).mapNotNull { cache.read(it) }

        Merged(
            posts = merge(stored.flatMap { it.posts }),
            errors = errors,
            fromCache = false,
            oldestFetchedAtMillis = oldest,
            canLoadMore = more || stored.any { it.nextCursor != null }
        )
    }

    /**
     * Extends the merged timeline further back.
     *
     * The trick is choosing whom to ask. An account whose oldest loaded post is
     * recent is the one capping how far back the merged view can honestly go,
     * so those get paged first. Asking every account for another page instead
     * would waste requests on accounts that already reach back weeks, and with
     * one fragile instance in the pool, wasted requests are the scarce resource.
     *
     * Only the accounts of [folder] are paged. LinkedOut pages every account
     * whatever folder is on screen, which spends requests on posts the
     * reader is not looking at.
     */
    suspend fun loadMore(folder: String? = null): Merged = coroutineScope {
        val handles = handlesIn(folder)
        if (handles.isEmpty()) return@coroutineScope Merged()

        val cached = handles.mapNotNull { cache.read(it) }
        val blocking = cached
            .filter { it.nextCursor != null && it.posts.isNotEmpty() }
            .sortedByDescending { feed -> feed.oldestUnpinnedMillis() }
            .take(MAX_PARALLEL_FETCHES)

        if (blocking.isEmpty()) {
            return@coroutineScope Merged(
                posts = merge(cached.flatMap { it.posts }),
                canLoadMore = false
            )
        }

        val errors = mutableMapOf<String, AppError>()

        inParallel(blocking) { feed ->
            feed.handle to feeds.loadFeed(feed.handle, feed.nextCursor)
        }.forEach { (handle, outcome) ->
            when (outcome) {
                is Outcome.Success -> cache.append(outcome.value, isPagedFetch = true)
                is Outcome.Failure -> errors[handle] = outcome.error
            }
        }

        val refreshed = handles.mapNotNull { cache.read(it) }
        Merged(
            posts = merge(refreshed.flatMap { it.posts }),
            errors = errors,
            fromCache = false,
            oldestFetchedAtMillis = refreshed.minOfOrNull { it.fetchedAtMillis },
            canLoadMore = refreshed.any { it.nextCursor != null }
        )
    }

    /**
     * How far back this feed actually reaches, ignoring the pinned post. A pin
     * can be months old while the feed has only been read back an hour, and
     * counting it made the account look already deep, so it was never chosen
     * for the next page and its older posts were never fetched.
     */
    private fun Feed.oldestUnpinnedMillis(): Long =
        posts.filterNot { it.isPinned }.minOfOrNull { it.publishedAtMillis }
            ?: posts.minOfOrNull { it.publishedAtMillis }
            ?: Long.MAX_VALUE

    /**
     * Pinned posts first, then newest first, deduplicated. Sorted by date
     * alone, a pin that is months old would sit at the very bottom of Home.
     * The author's choice is honoured across accounts, so every followed
     * account's pin sits at the top, each labelled "Pinned" on its card.
     */
    private fun merge(posts: List<Post>): List<Post> =
        posts.distinctBy { it.id }
            .sortedWith(
                compareByDescending<Post> { it.isPinned }
                    .thenByDescending { it.publishedAtMillis }
            )
            .take(MAX_TIMELINE_POSTS)

    internal companion object {
        /**
         * Two at a time. Everything is read from one host, and HostThrottle
         * keeps that host's pace whatever the count here, so two reads in
         * flight cost one slot of waiting and not a second burst. loadMore
         * pages with the same count. Declared and used as LinkedOut's, which
         * keeps its own count at one:
         * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/data/repository/TimelineRepository.kt#MAX_PARALLEL_FETCHES
         */
        const val MAX_PARALLEL_FETCHES = 2
        const val MAX_TIMELINE_POSTS = 20_000

        /**
         * Runs [read] for every item, at most [limit] at a time, and returns the
         * results in the order of [items]. The semaphore and the async of
         * refresh and loadMore, in one place so a test can count the reads in
         * flight.
         */
        internal suspend fun <T, R> inParallel(items: List<T>, limit: Int = MAX_PARALLEL_FETCHES, read: suspend (T) -> R): List<R> =
            coroutineScope {
                val gate = Semaphore(limit)
                items.map { item -> async { gate.withPermit { read(item) } } }.map { it.await() }
            }
    }
}
