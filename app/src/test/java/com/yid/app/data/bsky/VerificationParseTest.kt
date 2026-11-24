package com.yid.app.data.bsky

import com.yid.app.core.model.Actor
import com.yid.app.core.model.Feed
import com.yid.app.core.model.Post
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The verification state of getProfile and of a post's author reaches the models (requests.md point 17), as Bluesky's
 * useSimpleVerificationState reads it: "valid" in verifiedStatus or trustedVerifierStatus is verified, "invalid", "none"
 * and a missing `verification` are not.
 * https://github.com/bluesky-social/social-app/blob/main/src/components/verification/index.ts#useSimpleVerificationState
 * https://github.com/bluesky-social/atproto/blob/main/packages/api/lexicons/app/bsky/actor/defs.json#verificationState
 * Fixtures reduced to the fields read, neutral content.
 */
class VerificationParseTest {

    private fun verification(verified: String, verifier: String) =
        """"verification":{"verifications":[],"verifiedStatus":"$verified","trustedVerifierStatus":"$verifier"}"""

    private fun profile(extra: String) =
        """{"did":"did:plc:abc","handle":"example.com","displayName":"Example","postsCount":3${if (extra.isEmpty()) "" else ",$extra"}}"""

    private fun feedOf(profile: String?, authorExtra: String = ""): Feed {
        val post = """{"uri":"at://did:plc:abc/app.bsky.feed.post/p1","cid":"cp1",
             "author":{"did":"did:plc:abc","handle":"example.com","displayName":"Example"${if (authorExtra.isEmpty()) "" else ",$authorExtra"}},
             "record":{"${'$'}type":"app.bsky.feed.post","createdAt":"2023-09-18T01:20:00.000Z","text":"Hello"},
             "replyCount":0,"repostCount":0,"quoteCount":0,"likeCount":0,"indexedAt":"2023-09-18T01:20:01.000Z"}"""
        return BskyParser.feed(profile, """{"feed":[{"post":$post}]}""", "example.com", nowMillis = 1_000)
    }

    @Test
    fun `a valid verified status is verified`() {
        val feed = feedOf(profile(verification("valid", "none")), verification("valid", "none"))

        assertEquals(true, feed.verified)
        assertTrue(feed.posts.single().authorVerified)
    }

    @Test
    fun `a trusted verifier is verified with the same mark`() {
        val feed = feedOf(profile(verification("none", "valid")), verification("none", "valid"))

        assertEquals(true, feed.verified)
        assertTrue(feed.posts.single().authorVerified)
    }

    @Test
    fun `an invalid status is not verified`() {
        val feed = feedOf(profile(verification("invalid", "invalid")), verification("invalid", "none"))

        assertEquals(false, feed.verified)
        assertFalse(feed.posts.single().authorVerified)
    }

    @Test
    fun `none is not verified`() {
        val feed = feedOf(profile(verification("none", "none")), verification("none", "none"))

        assertEquals(false, feed.verified)
        assertFalse(feed.posts.single().authorVerified)
    }

    @Test
    fun `a missing verification is not verified`() {
        val feed = feedOf(profile(""))

        assertEquals(false, feed.verified)
        assertFalse(feed.posts.single().authorVerified)
    }

    @Test
    fun `no profile read leaves the state unknown`() {
        assertNull(feedOf(null).verified)
    }

    @Test
    fun `a search result carries its state`() {
        val found = BskyParser.actors(
            """{"actors":[{"did":"did:plc:a","handle":"a.example.com",${verification("valid", "none")}},
                |{"did":"did:plc:b","handle":"b.example.com"}]}""".trimMargin()
        )

        assertEquals(listOf(true, false), found.map(Actor::verified))
    }

    @Test
    fun `a cache written before the field still reads`() {
        val oldPost = """{"id":"i","authorHandle":"h","authorName":"n","text":"t","publishedAtMillis":1,"permalink":"p"}"""
        val oldFeed = """{"handle":"h","displayName":"n","posts":[$oldPost],"fetchedFromHost":"x","fetchedAtMillis":1}"""

        val post = Json.decodeFromString<Post>(oldPost)
        assertFalse(post.authorVerified)
        assertTrue(post.facets.isEmpty())
        assertNull(Json.decodeFromString<Feed>(oldFeed).verified)
    }
}
