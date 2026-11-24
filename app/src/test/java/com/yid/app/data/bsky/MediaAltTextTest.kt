package com.yid.app.data.bsky

import kotlinx.serialization.json.Json
import com.yid.app.core.model.MediaItem
import com.yid.app.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The alt text of a picture reaches the model, from the lexicon's `alt` of images#view:
 * https://raw.githubusercontent.com/bluesky-social/atproto/main/packages/api/lexicons/app/bsky/embed/images.json#alt
 * Fixture reduced to the fields read, neutral content.
 */
class MediaAltTextTest {

    private fun feedWith(embed: String): List<MediaItem> {
        val post = """
            {"uri":"at://did:plc:abc/app.bsky.feed.post/p1","cid":"cp1",
             "author":{"did":"did:plc:abc","handle":"example.com","displayName":"Example"},
             "record":{"${'$'}type":"app.bsky.feed.post","createdAt":"2023-09-18T01:20:00.000Z","text":"Hello"},
             "embed":$embed,"replyCount":0,"repostCount":0,"quoteCount":0,"likeCount":0,"indexedAt":"2023-09-18T01:20:01.000Z"}
        """.trimIndent()
        val page = """{"feed":[{"post":$post}]}"""
        return BskyParser.feed(null, page, "example.com", nowMillis = 1_000).posts.single().media
    }

    private fun images(vararg alts: String?) = """{"${'$'}type":"app.bsky.embed.images#view","images":[${
        alts.mapIndexed { i, alt ->
            """{"thumb":"https://cdn.bsky.app/t$i","fullsize":"https://cdn.bsky.app/f$i"${alt?.let { ",\"alt\":\"$it\"" } ?: ""}}"""
        }.joinToString(",")
    }]}"""

    @Test
    fun `the alt of an image reaches the media item`() {
        val media = feedWith(images("A red door", "A tree"))

        assertEquals(listOf("A red door", "A tree"), media.map { it.altText })
    }

    @Test
    fun `an empty or missing alt stays null`() {
        val media = feedWith(images("", null))

        assertNull(media[0].altText)
        assertNull(media[1].altText)
    }

    @Test
    fun `the alt of a video embed is read when present`() {
        val media = feedWith(
            """{"${'$'}type":"app.bsky.embed.video#view","cid":"v1","playlist":"https://video.bsky.app/p.m3u8","alt":"A clip"}"""
        )

        assertEquals(MediaType.VIDEO, media.single().type)
        assertEquals("A clip", media.single().altText)
    }

    @Test
    fun `a cache written before the field still reads`() {
        val old = """{"previewUrl":"p","downloadUrl":"d","type":"PHOTO"}"""

        assertNull(Json.decodeFromString<MediaItem>(old).altText)
    }

    @Test
    fun `the alt survives the cache's JSON`() {
        val item = MediaItem("p", "d", MediaType.PHOTO, altText = "A red door")

        assertEquals(item, Json.decodeFromString<MediaItem>(Json.encodeToString(MediaItem.serializer(), item)))
    }
}
