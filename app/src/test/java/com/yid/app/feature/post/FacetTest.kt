package com.yid.app.feature.post

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.link.BskyLink
import com.yid.app.core.model.ByteSlice
import com.yid.app.core.model.Facet
import com.yid.app.core.model.FacetFeature
import com.yid.app.data.bsky.BskyParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A post's facets (requests.md point 21): byteStart and byteEnd count UTF-8 bytes of the text, so a slice is decoded from
 * the bytes and an emoji before a facet does not move it. The slices are cut as Bluesky's RichText.segments cuts them: in
 * order, a facet starting inside the one before is ignored, and a record is hostile input, so a slice out of the text or
 * cut inside a character is ignored too.
 * https://github.com/bluesky-social/atproto/blob/main/packages/api/lexicons/app/bsky/richtext/facet.json
 * https://github.com/bluesky-social/atproto/blob/main/packages/api/src/rich-text/rich-text.ts#L195-L233
 * Fixtures reduced to the fields read, neutral content.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = android.app.Application::class, sdk = [36])
class FacetTest {

    private fun tag(start: Int, end: Int, name: String) = Facet(ByteSlice(start, end), listOf(FacetFeature(tag = name)))
    private fun mention(start: Int, end: Int) = Facet(ByteSlice(start, end), listOf(FacetFeature(did = "did:plc:abc")))
    private fun link(start: Int, end: Int, uri: String) = Facet(ByteSlice(start, end), listOf(FacetFeature(uri = uri)))

    private fun pieces(text: String, vararg facets: Facet) = RichText(text, facets.toList()).segments().toList()

    // Fixtures as the post record of app.bsky.feed.post gives its facets, like MediaAltTextTest's feedWith
    // (src/test/java/com/yid/app/data/bsky/MediaAltTextTest.kt): the fields read only.
    // https://github.com/bluesky-social/atproto/blob/main/packages/api/lexicons/app/bsky/richtext/facet.json
    private fun record(facets: String): JsonObject = Json.parseToJsonElement("""{"facets":$facets}""") as JsonObject

    private fun postWith(facets: String): String =
        """{"uri":"at://did:plc:abc/app.bsky.feed.post/p1","author":{"did":"did:plc:abc","handle":"example.com"},
            "record":{"${'$'}type":"app.bsky.feed.post","createdAt":"2023-09-18T01:20:00.000Z","text":"hi #trail"$facets},
            "indexedAt":"2023-09-18T01:20:01.000Z"}"""

    @Test
    fun `ascii text, byte offsets are character offsets`() {
        val text = "see #trail now"

        val segments = pieces(text, tag(4, 10, "trail"))

        assertEquals(listOf("see ", "#trail", " now"), segments.map { it.text })
        assertTrue(segments[1].isTag())
        assertEquals("trail", segments[1].tag?.tag)
        assertNull(segments[0].facet)
    }

    @Test
    fun `an emoji before the facet counts four bytes and two characters`() {
        // 🌲 is 4 bytes and 2 UTF-16 units, so "#trail" starts at byte 5 and at character 3.
        val text = "🌲 #trail"

        val segments = pieces(text, tag(5, 11, "trail"))

        assertEquals(listOf("🌲 ", "#trail"), segments.map { it.text })
        // The same numbers read as characters fall inside the emoji, whose fourth byte is not a place to cut: ignored.
        assertEquals(listOf(text), pieces(text, tag(3, 9, "trail")).map { it.text })
    }

    @Test
    fun `a mention and a link`() {
        val text = "thanks @bram.example.com see example.com/x"

        val segments = pieces(text, mention(7, 24), link(29, 42, "https://example.com/xyz"))

        assertEquals(listOf("thanks ", "@bram.example.com", " see ", "example.com/x"), segments.map { it.text })
        assertTrue(segments[1].isMention())
        assertEquals("https://example.com/xyz", segments[3].link?.uri)
    }

    @Test
    fun `a facet starting inside the one before is ignored`() {
        val text = "see #trailguide now"

        val segments = pieces(text, tag(4, 15, "trailguide"), mention(6, 12))

        assertEquals(listOf("see ", "#trailguide", " now"), segments.map { it.text })
        assertTrue(segments[1].isTag())
    }

    @Test
    fun `facets out of order are sorted`() {
        val text = "#a and #b"

        val segments = pieces(text, tag(7, 9, "b"), tag(0, 2, "a"))

        assertEquals(listOf("#a", " and ", "#b"), segments.map { it.text })
    }

    @Test
    fun `a slice past the end, negative or cut inside a character is ignored`() {
        val text = "café #ok"

        // é is bytes 3 and 4: a facet starting at 4 or ending at 4 cuts it in two.
        assertEquals(listOf(text), pieces(text, tag(7, 99, "ok")).map { it.text })
        assertEquals(listOf(text), pieces(text, tag(-2, 3, "x")).map { it.text })
        assertEquals(listOf(text), pieces(text, tag(4, 8, "x")).map { it.text })
        assertEquals(listOf(text), pieces(text, tag(0, 4, "x")).map { it.text })
        assertEquals(listOf(text), pieces(text, tag(5, 3, "x")).map { it.text })
    }

    @Test
    fun `text with no facet is one plain piece`() {
        val segments = pieces("nothing here")

        assertEquals(listOf("nothing here"), segments.map { it.text })
        assertNull(segments.single().facet)
    }

    @Test
    fun `the parser reads the three kinds, sorted, and drops what it does not know`() {
        val facets = BskyParser.facets(
            record(
                """[
                {"index":{"byteStart":9,"byteEnd":12},"features":[{"${'$'}type":"app.bsky.richtext.facet#tag","tag":"b"}]},
                {"index":{"byteStart":0,"byteEnd":4},"features":[{"${'$'}type":"app.bsky.richtext.facet#mention","did":"did:plc:abc"}]},
                {"index":{"byteStart":5,"byteEnd":8},"features":[{"${'$'}type":"app.bsky.richtext.facet#link","uri":"https://example.com"}]},
                {"index":{"byteStart":13,"byteEnd":14},"features":[{"${'$'}type":"app.bsky.richtext.facet#other","x":"y"}]},
                {"index":{"byteStart":15,"byteEnd":16},"features":[]},
                {"index":{"byteStart":20,"byteEnd":18},"features":[{"${'$'}type":"app.bsky.richtext.facet#tag","tag":"c"}]},
                {"features":[{"${'$'}type":"app.bsky.richtext.facet#tag","tag":"d"}]}
                ]"""
            )
        )

        assertEquals(listOf(0, 5, 9), facets.map { it.index.byteStart })
        assertEquals("did:plc:abc", facets[0].features.single().did)
        assertEquals("https://example.com", facets[1].features.single().uri)
        assertEquals("b", facets[2].features.single().tag)
    }

    @Test
    fun `a post carries its facets and a record without any has none`() {
        val view = postWith(
            ""","facets":[{"index":{"byteStart":3,"byteEnd":9},"features":[{"${'$'}type":"app.bsky.richtext.facet#tag","tag":"trail"}]}]"""
        )

        val withFacets = BskyParser.feed(null, """{"feed":[{"post":$view}]}""", "example.com", 1).posts.single()
        val without = BskyParser.feed(null, """{"feed":[{"post":${postWith("")}}]}""", "example.com", 1).posts.single()

        assertEquals("trail", withFacets.facets.single().features.single().tag)
        assertTrue(without.facets.isEmpty())
    }

    @Test
    fun `the text is drawn with its facets in the colour and each one tappable`() {
        val text = "🌲 #trail with @bram.example.com and https://example.com/long"

        val drawn = facetText(
            text,
            listOf(
                tag(5, 11, "trail"),
                mention(17, 34),
                link(39, 63, "https://example.com/long")
            ),
            Color.Red
        )

        assertEquals(text, drawn.text)
        val links = drawn.getLinkAnnotations(0, drawn.length)
        assertEquals(3, links.size)
        assertEquals(listOf(3 to 9, 15 to 32, 37 to 61), links.map { it.start to it.end })
        assertEquals(
            listOf(
                BskyLink.makeSearchLink("#trail"),
                BskyLink.profileUrl("bram.example.com"),
                "https://example.com/long"
            ),
            links.map { (it.item as LinkAnnotation.Url).url }
        )
    }

    @Test
    fun `a mention that is not a handle and a link that cannot leave stay plain`() {
        val text = "@not_a_handle and javascript:alert(1)"

        val drawn = facetText(text, listOf(mention(0, 13), link(18, 37, "javascript:alert(1)")), Color.Red)

        assertEquals(text, drawn.text)
        assertTrue(drawn.getLinkAnnotations(0, drawn.length).isEmpty())
    }

    @Test
    fun `a tag leads to Bluesky's search address`() {
        assertEquals("https://bsky.app/search?q=%23trail", BskyLink.makeSearchLink("#trail"))
        assertEquals("https://bsky.app/search?q=%23a%20b", BskyLink.makeSearchLink("#a b"))
        assertEquals("https://bsky.app/search?q=%23trail%20from%3Aada.bsky.social", BskyLink.makeSearchLink("#trail", from = "ada.bsky.social"))
    }
}
