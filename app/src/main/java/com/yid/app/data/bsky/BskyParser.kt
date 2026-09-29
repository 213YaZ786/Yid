package com.yid.app.data.bsky

import com.yid.app.core.link.BskyLink
import com.yid.app.core.model.Actor
import com.yid.app.core.model.Conversation
import com.yid.app.core.model.Feed
import com.yid.app.core.model.LinkCard
import com.yid.app.core.model.MediaItem
import com.yid.app.core.model.MediaType
import com.yid.app.core.model.Post
import com.yid.app.core.model.PostKind
import com.yid.app.core.model.PostStats
import com.yid.app.core.model.ProfileStats
import com.yid.app.core.model.QuotedPost
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns the public Bluesky API's JSON into Yiḍ's models. Pure, tested on
 * answers saved from public.api.bsky.app (September 2026).
 *
 * Nothing is scraped: the AppView answers structured records, the same ones
 * the official app reads. The only interpretation here is which embed shape
 * becomes a picture, a video, a link card or a quote.
 */
internal object BskyParser {

    private val json = Json { ignoreUnknownKeys = true }

    /** A profile and one page of its posts, from getProfile and getAuthorFeed. */
    fun feed(profileJson: String?, feedJson: String, actor: String, nowMillis: Long): Feed {
        val profile = profileJson?.let { obj(it) }
        val page = obj(feedJson) ?: JsonObject(emptyMap())
        val posts = page.array("feed").mapNotNull { (it as? JsonObject)?.let(::feedItem) }
        return Feed(
            handle = profile?.text("handle") ?: actor,
            displayName = profile?.text("displayName").orEmpty(),
            posts = posts,
            fetchedFromHost = BskyApi.HOST,
            fetchedAtMillis = nowMillis,
            avatarUrl = profile?.text("avatar"),
            bio = profile?.text("description"),
            nextCursor = page.text("cursor"),
            bannerUrl = profile?.text("banner"),
            joined = profile?.text("createdAt")?.let(::joinedLabel),
            stats = profile?.let {
                ProfileStats(posts = it.long("postsCount"), following = it.long("followsCount"), followers = it.long("followersCount"))
            }
        )
    }

    /** A post with the posts it answers and the replies under it, from getPostThread. */
    fun conversation(threadJson: String): Conversation? {
        val thread = obj(threadJson)?.obj("thread") ?: return null
        if (thread.text("\$type") != THREAD_POST) return null
        val main = postView(thread.obj("post") ?: return null) ?: return null
        val ancestors = ArrayDeque<Post>()
        var parent = thread.obj("parent")
        while (parent != null && parent.text("\$type") == THREAD_POST) {
            parent.obj("post")?.let(::postView)?.let { ancestors.addFirst(it) }
            parent = parent.obj("parent")
        }
        val replies = thread.array("replies").mapNotNull { it as? JsonObject }
            .filter { it.text("\$type") == THREAD_POST }
        // The author answering themselves right under the post is the rest of
        // the post, as a thread reads, and goes before the other replies.
        val continuation = mutableListOf<Post>()
        var own = replies.firstOrNull { it.obj("post")?.obj("author")?.text("did") == thread.obj("post")?.obj("author")?.text("did") }
        while (own != null) {
            own.obj("post")?.let(::postView)?.let(continuation::add)
            val author = own.obj("post")?.obj("author")?.text("did")
            own = own.array("replies").mapNotNull { it as? JsonObject }
                .firstOrNull { it.text("\$type") == THREAD_POST && it.obj("post")?.obj("author")?.text("did") == author }
        }
        val continuationIds = continuation.map { it.id }.toSet()
        val chains = replies
            .filter { it.obj("post")?.text("uri") !in continuationIds }
            .map { reply ->
                // One reply and the first answer to it, the way a thread is
                // shown in small chains rather than as a full tree.
                buildList {
                    reply.obj("post")?.let(::postView)?.let(::add)
                    reply.array("replies").mapNotNull { it as? JsonObject }
                        .firstOrNull { it.text("\$type") == THREAD_POST }
                        ?.obj("post")?.let(::postView)?.let(::add)
                }
            }
            .filter { it.isNotEmpty() }
        return Conversation(
            ancestors = ancestors.toList(),
            main = main,
            continuation = continuation,
            replies = chains,
            host = BskyApi.HOST
        )
    }

    /** People found by searchActors: handle, name, avatar, bio. */
    fun actors(searchJson: String): List<Actor> =
        obj(searchJson)?.array("actors").orEmpty().mapNotNull { element ->
            val actor = element as? JsonObject ?: return@mapNotNull null
            val handle = actor.text("handle") ?: return@mapNotNull null
            Actor(handle = handle, displayName = actor.text("displayName"), avatarUrl = actor.text("avatar")?.let(::thumbnail), description = actor.text("description"))
        }

    private fun feedItem(item: JsonObject): Post? {
        val post = postView(item.obj("post") ?: return null) ?: return null
        val reason = item.obj("reason")
        return when (reason?.text("\$type")) {
            REASON_REPOST -> post.copy(kind = PostKind.REPOST, relatedHandle = reason.obj("by")?.text("handle"))
            REASON_PIN -> post.copy(isPinned = true)
            // The feed names who a reply answers, the post record only links to it.
            else -> if (post.kind == PostKind.REPLY) {
                post.copy(relatedHandle = item.obj("reply")?.obj("parent")?.obj("author")?.text("handle"))
            } else {
                post
            }
        }
    }

    private fun postView(view: JsonObject): Post? {
        val uri = view.text("uri") ?: return null
        val author = view.obj("author") ?: return null
        val handle = author.text("handle") ?: return null
        val record = view.obj("record") ?: return null
        val embed = view.obj("embed")
        val quoted = quote(embed)
        val (text, links) = textAndLinks(record)
        return Post(
            id = uri,
            authorHandle = handle,
            authorName = author.text("displayName")?.takeIf { it.isNotBlank() } ?: handle,
            avatarUrl = author.text("avatar")?.let(::thumbnail),
            text = text,
            links = links,
            publishedAtMillis = record.text("createdAt")?.let(::millis) ?: view.text("indexedAt")?.let(::millis) ?: 0L,
            permalink = BskyLink.webUrl(uri) ?: uri,
            kind = when {
                record.obj("reply") != null -> PostKind.REPLY
                quoted != null -> PostKind.QUOTE
                else -> PostKind.ORIGINAL
            },
            media = media(embed),
            quoted = quoted,
            card = card(embed),
            stats = PostStats(
                replies = view.int("replyCount"),
                reposts = view.int("repostCount"),
                quotes = view.int("quoteCount"),
                likes = view.int("likeCount")
            )
        )
    }

    /**
     * The text as written, and the full addresses of its links. Bluesky
     * shortens a link in the text ("example.com/some/pa...") and keeps the
     * real address in a facet, which is exactly the pair the post text
     * renderer expects.
     */
    private fun textAndLinks(record: JsonObject): Pair<String, List<String>> {
        val text = record.text("text").orEmpty()
        val links = record.array("facets").mapNotNull { it as? JsonObject }
            .flatMap { it.array("features") }
            .mapNotNull { it as? JsonObject }
            .filter { it.text("\$type") == FACET_LINK }
            .mapNotNull { it.text("uri") }
            .distinct()
        return text to links
    }

    private fun media(embed: JsonObject?): List<MediaItem> = when (embed?.text("\$type")) {
        EMBED_IMAGES -> embed.array("images").mapNotNull { element ->
            val image = element as? JsonObject ?: return@mapNotNull null
            val full = image.text("fullsize") ?: return@mapNotNull null
            MediaItem(previewUrl = image.text("thumb") ?: full, downloadUrl = full, type = MediaType.PHOTO)
        }
        EMBED_VIDEO -> embed.text("playlist")?.let { playlist ->
            val type = if (embed.text("presentation") == "gif") MediaType.GIF else MediaType.VIDEO
            listOf(MediaItem(previewUrl = embed.text("thumbnail") ?: playlist, downloadUrl = playlist, type = type))
        }.orEmpty()
        EMBED_RECORD_WITH_MEDIA -> media(embed.obj("media"))
        else -> emptyList()
    }

    private fun card(embed: JsonObject?): LinkCard? {
        val external = when (embed?.text("\$type")) {
            EMBED_EXTERNAL -> embed.obj("external")
            EMBED_RECORD_WITH_MEDIA -> embed.obj("media")?.takeIf { it.text("\$type") == EMBED_EXTERNAL }?.obj("external")
            else -> null
        } ?: return null
        val uri = external.text("uri") ?: return null
        val thumb = external.text("thumb")
        return LinkCard(
            title = external.text("title")?.takeIf { it.isNotBlank() } ?: uri,
            description = external.text("description")?.takeIf { it.isNotBlank() },
            destination = uri.substringAfter("://").substringBefore('/').removePrefix("www."),
            imageUrl = thumb,
            url = uri
        )
    }

    /**
     * The post a post quotes. A quote of a feed, a list or a post that is
     * gone or hidden from logged out readers carries no text to show, and is
     * left out rather than drawn as an empty box.
     */
    private fun quote(embed: JsonObject?): QuotedPost? {
        val holder = when (embed?.text("\$type")) {
            EMBED_RECORD -> embed.obj("record")
            EMBED_RECORD_WITH_MEDIA -> embed.obj("record")?.obj("record")
            else -> null
        } ?: return null
        if (holder.text("\$type") != VIEW_RECORD) return null
        val uri = holder.text("uri") ?: return null
        val author = holder.obj("author") ?: return null
        val handle = author.text("handle") ?: return null
        return QuotedPost(
            handle = handle,
            name = author.text("displayName")?.takeIf { it.isNotBlank() } ?: handle,
            text = holder.obj("value")?.text("text").orEmpty(),
            permalink = BskyLink.webUrl(uri) ?: uri
        )
    }

    /**
     * The small version of an avatar. The API names the original, around 80 KB
     * for a picture drawn 40 dp wide on a card; the CDN serves a 5 KB one under
     * avatar_thumbnail (checked 2026-09-28). The profile header keeps the
     * original.
     */
    internal fun thumbnail(avatar: String): String = avatar.replace("/img/avatar/plain/", "/img/avatar_thumbnail/plain/")

    private fun millis(iso: String): Long? = runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull()

    private fun joinedLabel(iso: String): String? = millis(iso)?.let {
        "Joined " + JOINED.format(Instant.ofEpochMilli(it))
    }

    private fun obj(text: String): JsonObject? = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()
    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotEmpty() }
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.content?.toLongOrNull()
    private fun JsonObject.int(key: String): Int? = long(key)?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()

    private val JOINED = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH).withZone(ZoneOffset.UTC)

    private const val THREAD_POST = "app.bsky.feed.defs#threadViewPost"
    private const val REASON_REPOST = "app.bsky.feed.defs#reasonRepost"
    private const val REASON_PIN = "app.bsky.feed.defs#reasonPin"
    private const val FACET_LINK = "app.bsky.richtext.facet#link"
    private const val EMBED_IMAGES = "app.bsky.embed.images#view"
    private const val EMBED_VIDEO = "app.bsky.embed.video#view"
    private const val EMBED_EXTERNAL = "app.bsky.embed.external#view"
    private const val EMBED_RECORD = "app.bsky.embed.record#view"
    private const val EMBED_RECORD_WITH_MEDIA = "app.bsky.embed.recordWithMedia#view"
    private const val VIEW_RECORD = "app.bsky.embed.record#viewRecord"
}
