package com.yid.app.data.bsky

import com.yid.app.core.common.AppError
import com.yid.app.core.common.Outcome
import com.yid.app.core.debug.RequestLog
import com.yid.app.core.model.Actor
import com.yid.app.core.model.Conversation
import com.yid.app.core.model.Feed
import com.yid.app.core.network.ErrorMapper
import com.yid.app.core.network.HostThrottle
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.cancellation.CancellationException

/**
 * Bluesky's public AppView, which answers logged out readers with the same
 * records the official app shows. Checked on 2026-09-28: profiles, author
 * feeds with real paging, threads and people search answer 200 without an
 * account. Post search does not (403), so Yiḍ only searches the posts it
 * has saved.
 *
 * One host, one throttle. The AppView is generous, but a Home refresh asks
 * for every followed account at once and politeness is cheaper than a ban.
 */
class BskyApi(
    private val client: HttpClient,
    private val throttle: HostThrottle,
    private val log: RequestLog
) {

    /**
     * An account's profile and a page of its posts, pinned post included on
     * the first page. [cursor] continues further back, and the page says
     * where the next one starts.
     */
    suspend fun feed(actor: String, cursor: String? = null): Outcome<Feed> {
        // The profile only on the first page: a page further back only adds posts.
        val profile = if (cursor == null) {
            when (val read = get("app.bsky.actor.getProfile?actor=${enc(actor)}", RequestLog.Kind.PROFILE, actor)) {
                is Outcome.Success -> read.value
                is Outcome.Failure -> return read
            }
        } else {
            null
        }
        val path = buildString {
            append("app.bsky.feed.getAuthorFeed?actor=").append(enc(actor))
            append("&limit=").append(PAGE_SIZE)
            // Replies to others are conversations, not the account's timeline.
            append("&filter=posts_no_replies")
            if (cursor == null) append("&includePins=true") else append("&cursor=").append(enc(cursor))
        }
        return when (val page = get(path, if (cursor == null) RequestLog.Kind.PROFILE else RequestLog.Kind.PAGE, actor)) {
            is Outcome.Success -> Outcome.Success(BskyParser.feed(profile, page.value, actor, System.currentTimeMillis()))
            is Outcome.Failure -> page
        }
    }

    suspend fun thread(atUri: String): Outcome<Conversation> =
        when (val read = get("app.bsky.feed.getPostThread?uri=${enc(atUri)}&depth=$THREAD_DEPTH&parentHeight=$THREAD_PARENTS", RequestLog.Kind.THREAD)) {
            is Outcome.Success -> BskyParser.conversation(read.value)?.let { Outcome.Success(it) }
                ?: Outcome.Failure(AppError.PostUnavailable(HOST, "This post is gone or hidden from logged out readers"))
            is Outcome.Failure -> read
        }

    /** People whose name or handle matches [query]. */
    suspend fun searchActors(query: String): Outcome<List<Actor>> =
        when (val read = get("app.bsky.actor.searchActors?q=${enc(query)}&limit=$SEARCH_SIZE", RequestLog.Kind.LIST)) {
            is Outcome.Success -> Outcome.Success(BskyParser.actors(read.value))
            is Outcome.Failure -> read
        }

    private suspend fun get(path: String, kind: RequestLog.Kind, handle: String? = null): Outcome<String> =
        withContext(Dispatchers.IO) {
            val url = "https://$HOST/xrpc/$path"
            if (!throttle.acquire(HOST)) {
                return@withContext Outcome.Failure(AppError.RateLimited(HOST, throttle.cooldownRemainingMs(HOST) / 1000))
            }
            val started = System.currentTimeMillis()
            try {
                val response = client.get(url)
                val body = response.bodyAsText()
                val status = response.status.value
                log.record(
                    kind = kind,
                    url = url,
                    outcome = if (status == 200) "ok" else "http $status",
                    httpStatus = status,
                    bodyBytes = body.length,
                    durationMillis = System.currentTimeMillis() - started
                )
                if (status == 429) throttle.penalise(HOST, response.headers["Retry-After"]?.toLongOrNull())
                ErrorMapper.fromStatus(HOST, status, response.headers["Retry-After"]?.toLongOrNull(), body, handle)
                    ?.let { Outcome.Failure(it) }
                    ?: Outcome.Success(body)
            } catch (failure: Throwable) {
                if (failure is CancellationException) throw failure
                log.record(kind, url, "transport failure", durationMillis = System.currentTimeMillis() - started, detail = failure.message)
                Outcome.Failure(ErrorMapper.fromThrowable(HOST, failure))
            }
        }

    private fun enc(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    companion object {
        const val HOST = "public.api.bsky.app"
        private const val PAGE_SIZE = 30
        private const val SEARCH_SIZE = 12
        private const val THREAD_DEPTH = 3
        private const val THREAD_PARENTS = 10
    }
}
