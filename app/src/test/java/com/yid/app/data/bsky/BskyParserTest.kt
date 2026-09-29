package com.yid.app.data.bsky

import com.yid.app.core.model.MediaType
import com.yid.app.core.model.PostKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shapes of public.api.bsky.app answers of September 2026, reduced to the
 * fields read, with neutral content.
 */
class BskyParserTest {

    private val feed = BskyParser.feed(PROFILE, FEED, "example.com", nowMillis = 1_000)

    @Test
    fun `reads the profile around the posts`() {
        assertEquals("example.com", feed.handle)
        assertEquals("Example", feed.displayName)
        assertEquals("A bio", feed.bio)
        assertEquals("https://cdn.bsky.app/img/banner/plain/did:plc:abc/b", feed.bannerUrl)
        assertEquals("Joined May 2023", feed.joined)
        assertEquals(120L, feed.stats?.followers)
        assertEquals("next-page", feed.nextCursor)
    }

    @Test
    fun `marks the pin and the repost`() {
        val (pinned, repost) = feed.posts
        assertTrue(pinned.isPinned)
        assertEquals(PostKind.REPOST, repost.kind)
        assertEquals("example.com", repost.relatedHandle)
        assertEquals("other.bsky.social", repost.authorHandle)
    }

    @Test
    fun `turns embeds into pictures, videos, link cards and quotes`() {
        val pinned = feed.posts[0]
        assertEquals(listOf(MediaType.PHOTO), pinned.media.map { it.type })
        assertEquals("https://cdn.bsky.app/img/feed_fullsize/plain/did:plc:abc/img1", pinned.media.single().downloadUrl)
        assertEquals(listOf("https://example.org/article"), pinned.links)

        val repost = feed.posts[1]
        val video = repost.media.single()
        assertEquals(MediaType.VIDEO, video.type)
        assertTrue(video.downloadUrl.endsWith("playlist.m3u8"))
        assertEquals("Quoted text", repost.quoted?.text)
        assertEquals(PostKind.REPOST, repost.kind)

        val card = feed.posts[2].card!!
        assertEquals("An article", card.title)
        assertEquals("example.org", card.destination)
        assertEquals("https://example.org/a", card.url)
    }

    @Test
    fun `ids are AT URIs and permalinks bsky app addresses`() {
        val post = feed.posts[0]
        assertEquals("at://did:plc:abc/app.bsky.feed.post/p1", post.id)
        assertEquals("https://bsky.app/profile/did:plc:abc/post/p1", post.permalink)
        assertEquals(1_695_000_000_000L - 1_695_000_000_000L % 1000, post.publishedAtMillis - post.publishedAtMillis % 1000)
    }

    @Test
    fun `reads a thread with parents, the author's continuation and replies`() {
        val conversation = BskyParser.conversation(THREAD)!!
        assertEquals(listOf("at://did:plc:abc/app.bsky.feed.post/parent"), conversation.ancestors.map { it.id })
        assertEquals("at://did:plc:abc/app.bsky.feed.post/main", conversation.main?.id)
        assertEquals(listOf("at://did:plc:abc/app.bsky.feed.post/self"), conversation.continuation.map { it.id })
        assertEquals(listOf(listOf("at://did:plc:xyz/app.bsky.feed.post/r1", "at://did:plc:abc/app.bsky.feed.post/r1a")), conversation.replies.map { chain -> chain.map { it.id } })
    }

    @Test
    fun `a missing post is no conversation`() {
        assertNull(BskyParser.conversation("""{"thread":{"${'$'}type":"app.bsky.feed.defs#notFoundPost","uri":"at://x/app.bsky.feed.post/y","notFound":true}}"""))
    }

    @Test
    fun `a video sent as a GIF stays a GIF, and a reply names who it answers`() {
        val reply = feed.posts[3]
        assertEquals(MediaType.GIF, reply.media.single().type)
        assertEquals(PostKind.REPLY, reply.kind)
        assertEquals("example.com", reply.relatedHandle)
    }

    @Test
    fun `reads every count and draws avatars from thumbnails`() {
        val stats = feed.posts[0].stats!!
        assertEquals(listOf(1, 2, 4, 3), listOf(stats.replies, stats.reposts, stats.quotes, stats.likes))
        assertEquals("https://cdn.bsky.app/img/avatar_thumbnail/plain/did:plc:abc/av", feed.posts[0].avatarUrl)
        // The profile header keeps the original.
        assertEquals("https://cdn.bsky.app/img/avatar/plain/did:plc:abc/av", feed.avatarUrl)
    }

    @Test
    fun `reads people search results`() {
        val actors = BskyParser.actors("""{"actors":[{"did":"did:plc:1","handle":"nasa.gov","displayName":"NASA","avatar":"https://a/1"},{"did":"did:plc:2","handle":"esa.int"}]}""")
        assertEquals(listOf("nasa.gov", "esa.int"), actors.map { it.handle })
        assertEquals("NASA", actors[0].displayName)
    }

    private companion object {
        const val T = "\$type"

        fun author(did: String, handle: String, name: String?) =
            """{"did":"$did","handle":"$handle"${name?.let { ",\"displayName\":\"$it\"" } ?: ""},"avatar":"https://cdn.bsky.app/img/avatar/plain/$did/av"}"""

        fun post(rkey: String, did: String = "did:plc:abc", handle: String = "example.com", text: String = "Hello", embed: String? = null, record: String = "", replyTo: Boolean = false) = """
            {"uri":"at://$did/app.bsky.feed.post/$rkey","cid":"c$rkey","author":${author(did, handle, "Example")},
             "record":{"$T":"app.bsky.feed.post","createdAt":"2023-09-18T01:20:00.000Z","text":"$text"$record${if (replyTo) ",\"reply\":{\"parent\":{\"uri\":\"x\"},\"root\":{\"uri\":\"x\"}}" else ""}},
             ${embed?.let { "\"embed\":$it," } ?: ""}"replyCount":1,"repostCount":2,"quoteCount":4,"likeCount":3,"indexedAt":"2023-09-18T01:20:01.000Z"}
        """.trimIndent()

        val PROFILE = """{"did":"did:plc:abc","handle":"example.com","displayName":"Example","description":"A bio","avatar":"https://cdn.bsky.app/img/avatar/plain/did:plc:abc/av","banner":"https://cdn.bsky.app/img/banner/plain/did:plc:abc/b","followersCount":120,"followsCount":30,"postsCount":400,"createdAt":"2023-05-02T10:00:00.000Z"}"""

        val FEED = """
            {"feed":[
              {"post":${post("p1", text = "Read example.org/article", embed = """{"$T":"app.bsky.embed.images#view","images":[{"thumb":"https://cdn.bsky.app/img/feed_thumbnail/plain/did:plc:abc/img1","fullsize":"https://cdn.bsky.app/img/feed_fullsize/plain/did:plc:abc/img1","alt":""}]}""", record = ""","facets":[{"index":{"byteStart":5,"byteEnd":24},"features":[{"$T":"app.bsky.richtext.facet#link","uri":"https://example.org/article"}]}]""")},
               "reason":{"$T":"app.bsky.feed.defs#reasonPin"}},
              {"post":${post("p2", did = "did:plc:xyz", handle = "other.bsky.social", embed = """{"$T":"app.bsky.embed.recordWithMedia#view","media":{"$T":"app.bsky.embed.video#view","cid":"v1","playlist":"https://video.bsky.app/watch/did%3Aplc%3Axyz/v1/playlist.m3u8","thumbnail":"https://video.bsky.app/watch/did%3Aplc%3Axyz/v1/thumbnail.jpg"},"record":{"record":{"$T":"app.bsky.embed.record#viewRecord","uri":"at://did:plc:q/app.bsky.feed.post/q1","author":${author("did:plc:q", "quoted.bsky.social", "Quoted")},"value":{"text":"Quoted text"}}}}""")},
               "reason":{"$T":"app.bsky.feed.defs#reasonRepost","by":${author("did:plc:abc", "example.com", "Example")}}},
              {"post":${post("p3", embed = """{"$T":"app.bsky.embed.external#view","external":{"uri":"https://example.org/a","title":"An article","description":"About it","thumb":"https://cdn.bsky.app/img/feed_thumbnail/plain/did:plc:abc/t"}}""")}},
              {"post":${post("p4", replyTo = true, embed = """{"$T":"app.bsky.embed.video#view","cid":"v2","playlist":"https://video.bsky.app/watch/did%3Aplc%3Aabc/v2/playlist.m3u8","presentation":"gif"}""")},
               "reply":{"parent":${post("p3")},"root":${post("p3")}}}
            ],"cursor":"next-page"}
        """.trimIndent()

        fun threadPost(rkey: String, did: String, handle: String, replies: String = "[]", parent: String? = null) =
            """{"$T":"app.bsky.feed.defs#threadViewPost","post":${post(rkey, did, handle)}${parent?.let { ",\"parent\":$it" } ?: ""},"replies":$replies}"""

        val THREAD = """{"thread":${threadPost("main", "did:plc:abc", "example.com",
            parent = threadPost("parent", "did:plc:abc", "example.com"),
            replies = "[${threadPost("self", "did:plc:abc", "example.com")},${threadPost("r1", "did:plc:xyz", "other.bsky.social", replies = "[${threadPost("r1a", "did:plc:abc", "example.com")}]")}]")}}"""
    }
}
