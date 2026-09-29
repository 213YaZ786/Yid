package com.yid.app.core.link

import com.yid.app.data.bsky.BskyVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BskyLinkTest {

    @Test
    fun `reads profile and post addresses`() {
        assertEquals(BskyLink.Profile("nasa.gov"), BskyLink.parse("https://bsky.app/profile/nasa.gov"))
        assertEquals(BskyLink.Post("nasa.gov", "3mw2cdr44fc2a"), BskyLink.parse("https://bsky.app/profile/nasa.gov/post/3mw2cdr44fc2a?ref=x"))
    }

    @Test
    fun `reads AT URIs and builds web addresses back`() {
        val post = BskyLink.fromAtUri("at://did:plc:abc/app.bsky.feed.post/p1")!!
        assertEquals("at://did:plc:abc/app.bsky.feed.post/p1", post.atUri)
        assertEquals("https://bsky.app/profile/did:plc:abc/post/p1", BskyLink.webUrl(post.atUri))
    }

    @Test
    fun `leaves feeds, lists and other sites alone`() {
        assertNull(BskyLink.parse("https://bsky.app/profile/nasa.gov/feed/space"))
        assertNull(BskyLink.parse("https://example.com/profile/nasa.gov"))
        assertNull(BskyLink.parse("https://bsky.app/profile/not a handle"))
    }

    @Test
    fun `finds the author and video in a playlist address`() {
        assertEquals(
            "did:plc:eclio37ymobqex2ncko63h4r" to "bafkre",
            BskyVideo.parts("https://video.bsky.app/watch/did%3Aplc%3Aeclio37ymobqex2ncko63h4r/bafkre/playlist.m3u8")
        )
        assertEquals(
            "https://enoki.us-east.host.bsky.network",
            BskyVideo.serviceEndpoint("""{"service":[{"id":"#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"https://enoki.us-east.host.bsky.network/"}]}""")
        )
    }
}
