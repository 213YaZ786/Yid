package com.yid.app.core.model

import kotlinx.serialization.Serializable

/**
 * One post, normalised.
 *
 * Every source produces this exact type, which is what lets a source be swapped
 * without the UI knowing. Fields a given source cannot supply stay null rather
 * than being faked, so the UI can hide what it does not have instead of showing
 * a plausible lie.
 */
@Serializable
data class Post(
    val id: String,
    val authorHandle: String,
    val authorName: String,
    val avatarUrl: String? = null,
    val text: String,
    val links: List<String> = emptyList(),
    val publishedAtMillis: Long,
    val permalink: String,
    val kind: PostKind = PostKind.ORIGINAL,
    val relatedHandle: String? = null,
    val isPinned: Boolean = false,
    val media: List<MediaItem> = emptyList(),
    val quoted: QuotedPost? = null,
    val card: LinkCard? = null,
    val stats: PostStats? = null,
    /**
     * The author's verification state: a verified account or a trusted verifier. A default, so a cache written
     * before this field still reads.
     * https://github.com/bluesky-social/atproto/blob/main/packages/api/lexicons/app/bsky/actor/defs.json#verificationState
     */
    val authorVerified: Boolean = false,
    /** The facets of [text], see [Facet]. A default, so a cache written before this field still reads. */
    val facets: List<Facet> = emptyList()
) {
    /**
     * The same post seen again. The stored post keeps its identity, which
     * includes how it reached the feed (a repost, a pin), and takes the newer
     * counts, since replies and likes move. A post that was not stored with
     * its avatar or its embeds takes them too.
     */
    fun mergedWith(fresh: Post): Post {
        if (fresh.id != id) return this
        val merged = copy(
            avatarUrl = fresh.avatarUrl ?: avatarUrl,
            media = media.ifEmpty { fresh.media },
            quoted = fresh.quoted ?: quoted,
            card = fresh.card ?: card,
            authorVerified = fresh.authorVerified,
            facets = facets.ifEmpty { fresh.facets },
            stats = stats?.let { old -> fresh.stats?.let(old::mergedWith) ?: old } ?: fresh.stats
        )
        return if (merged == this) this else merged
    }
}

/**
 * A facet of a post's text as app.bsky.richtext.facet#main gives it: where it is ([index], a byteSlice counted in
 * UTF-8 bytes of the text, start inclusive, end exclusive) and what it is ([features]). The type of Bluesky's
 * `Facet = AppBskyRichtextFacet.Main` and the field names of its `facetSort` and `facetFilter`:
 * https://github.com/bluesky-social/atproto/blob/main/packages/api/src/rich-text/rich-text.ts#L379-L383
 * not ported: facetSort (PostText.kt's RichText takes it)
 * not ported: facetFilter (PostText.kt's RichText takes it)
 */
@Serializable
data class Facet(val index: ByteSlice, val features: List<FacetFeature>)

@Serializable
data class ByteSlice(val byteStart: Int, val byteEnd: Int)

/** One of the lexicon's #mention (its [did]), #link (its [uri]) or #tag (its [tag], without the #). */
@Serializable
data class FacetFeature(val did: String? = null, val uri: String? = null, val tag: String? = null)

@Serializable
enum class PostKind { ORIGINAL, REPOST, REPLY, QUOTE }

@Serializable
enum class MediaType { PHOTO, VIDEO, GIF }

/**
 * [previewUrl] is what gets shown, [downloadUrl] is the full resolution
 * original. Bluesky serves both, and conflating them means either a blurry
 * gallery or a very slow timeline. A video's [downloadUrl] is its HLS
 * playlist, resolved to the original file only when it is saved. A GIF is
 * a short video its author sent as one, which loops without sound.
 */
@Serializable
data class MediaItem(
    val previewUrl: String,
    val downloadUrl: String,
    val type: MediaType,
    /**
     * The author's description of the picture, read by a screen reader and
     * shown nowhere. Null when the author wrote none. A default so a cache
     * written before this field still reads.
     */
    val altText: String? = null
)

@Serializable
data class QuotedPost(
    val handle: String,
    val name: String,
    val text: String,
    val permalink: String
)

/** A link preview, as the author's app attached it to the post. */
@Serializable
data class LinkCard(
    val title: String,
    val description: String?,
    val destination: String?,
    val imageUrl: String?,
    val url: String?
)

@Serializable
data class PostStats(
    val replies: Int? = null,
    val reposts: Int? = null,
    val quotes: Int? = null,
    val likes: Int? = null
) {
    /** Newer counts win, a count the fresh answer does not carry is kept. */
    fun mergedWith(fresh: PostStats) = PostStats(
        replies = fresh.replies ?: replies,
        reposts = fresh.reposts ?: reposts,
        quotes = fresh.quotes ?: quotes,
        likes = fresh.likes ?: likes
    )
}

/** A single account's feed, a profile and the posts read from it. */
@Serializable
data class Feed(
    val handle: String,
    val displayName: String,
    val posts: List<Post>,
    val fetchedFromHost: String,
    val fetchedAtMillis: Long,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val nextCursor: String? = null,
    /** Profile card details, from getProfile. */
    val bannerUrl: String? = null,
    /** For example "Joined March 2023". */
    val joined: String? = null,
    val stats: ProfileStats? = null,
    /** The account's verification state from getProfile; null when no profile was read. */
    val verified: Boolean? = null
)

/** The numbers on a profile card. Any may be missing. */
@Serializable
data class ProfileStats(
    val posts: Long? = null,
    val following: Long? = null,
    val followers: Long? = null,
    val likes: Long? = null
)

/**
 * A post with its surroundings, as read from its own page. Not cached: replies
 * change constantly and are only worth reading fresh.
 *
 * [ancestors] are the posts it answers, oldest first. [continuation] is the
 * author's own thread under it. [replies] are grouped in the small chains the
 * server shows, a reply followed by the answers to it.
 */
data class Conversation(
    val ancestors: List<Post>,
    val main: Post?,
    val continuation: List<Post>,
    val replies: List<List<Post>>,
    val host: String
)
