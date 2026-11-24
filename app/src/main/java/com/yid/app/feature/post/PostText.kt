package com.yid.app.feature.post

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.yid.app.core.link.BskyLink
import com.yid.app.core.link.SafeLinks
import com.yid.app.core.model.Facet
import com.yid.app.core.model.FacetFeature

/**
 * A text and its UTF-8 bytes: the facets of a post count bytes, Kotlin's strings UTF-16 units, so a slice is cut from
 * the bytes and decoded, as Bluesky's UnicodeString does.
 * https://github.com/bluesky-social/atproto/blob/main/packages/api/src/rich-text/unicode.ts#UnicodeString
 * not ported: graphemeLength (no grapheme count is asked: a post's length is not drawn)
 */
internal class UnicodeString(val utf16: String) {
    val utf8: ByteArray = utf16.toByteArray(Charsets.UTF_8)

    val length: Int get() = utf8.size

    fun slice(start: Int = 0, end: Int = length): String = String(utf8.copyOfRange(start, end), Charsets.UTF_8)

    fun utf16IndexToUtf8Index(i: Int): Int = utf16.substring(0, i).toByteArray(Charsets.UTF_8).size

    override fun toString(): String = utf16
}

/**
 * One piece of a text: plain, or under a facet, which says whether it is a link, a mention or a tag.
 * https://github.com/bluesky-social/atproto/blob/main/packages/api/src/rich-text/rich-text.ts#L122-L151
 */
internal class RichTextSegment(val text: String, val facet: Facet? = null) {
    val link: FacetFeature? get() = facet?.features?.firstOrNull { it.uri != null }

    fun isLink(): Boolean = link != null

    val mention: FacetFeature? get() = facet?.features?.firstOrNull { it.did != null }

    fun isMention(): Boolean = mention != null

    val tag: FacetFeature? get() = facet?.features?.firstOrNull { it.tag != null }

    fun isTag(): Boolean = tag != null
}

/**
 * A text with its facets: [segments] cuts it into plain pieces and faceted ones, in order.
 * The facets are kept as RichText's constructor keeps them, not negative-length (facetFilter) and sorted by where they
 * start (facetSort). A facet that reaches past the end of the text, or starts or ends inside a character (a continuation
 * byte), is dropped too: a record is hostile input, and Bluesky's slice would cut it into replacement characters.
 * https://github.com/bluesky-social/atproto/blob/main/packages/api/src/rich-text/rich-text.ts#L153-L177
 * https://github.com/bluesky-social/atproto/blob/main/packages/api/src/rich-text/rich-text.ts#L195-L233
 * https://github.com/bluesky-social/atproto/blob/main/packages/api/src/rich-text/rich-text.ts#L379-L383
 * not ported: RichText's graphemeLength (UnicodeString's, not ported)
 */
internal class RichText(text: String, facets: List<Facet> = emptyList()) {
    val unicodeText = UnicodeString(text)

    val facets: List<Facet> = facets.filter(::facetFilter).filter(this::onCharacters).sortedWith(::facetSort)

    val text: String get() = unicodeText.toString()

    val length: Int get() = unicodeText.length

    fun segments(): Sequence<RichTextSegment> = sequence {
        val facets = this@RichText.facets
        if (facets.isEmpty()) {
            yield(RichTextSegment(unicodeText.utf16))
            return@sequence
        }

        var textCursor = 0
        var facetCursor = 0
        do {
            val currFacet = facets[facetCursor]
            if (textCursor < currFacet.index.byteStart) {
                yield(RichTextSegment(unicodeText.slice(textCursor, currFacet.index.byteStart)))
            } else if (textCursor > currFacet.index.byteStart) {
                facetCursor++
                continue
            }
            if (currFacet.index.byteStart < currFacet.index.byteEnd) {
                val subtext = unicodeText.slice(currFacet.index.byteStart, currFacet.index.byteEnd)
                if (subtext.isBlank()) {
                    // dont empty string entities
                    yield(RichTextSegment(subtext))
                } else {
                    yield(RichTextSegment(subtext, currFacet))
                }
            }
            textCursor = currFacet.index.byteEnd
            facetCursor++
        } while (facetCursor < facets.size)
        if (textCursor < unicodeText.length) {
            yield(RichTextSegment(unicodeText.slice(textCursor, unicodeText.length)))
        }
    }

    private fun onCharacters(facet: Facet): Boolean {
        val (byteStart, byteEnd) = facet.index
        if (byteStart < 0 || byteEnd > unicodeText.length) return false
        // 0b10xxxxxx is the continuation of a character begun before it.
        fun inside(at: Int) = at < unicodeText.length && (unicodeText.utf8[at].toInt() and 0xC0) == 0x80
        return !inside(byteStart) && !inside(byteEnd)
    }
}

private fun facetSort(a: Facet, b: Facet): Int = a.index.byteStart - b.index.byteStart

// Discard negative-length facets. Zero-length facets are valid.
private fun facetFilter(facet: Facet): Boolean = facet.index.byteStart <= facet.index.byteEnd

/**
 * The text of a post drawn with its facets: a link, a mention and a tag in [linkColor] and tappable, the rest plain.
 * Each segment is looked at in order, as Bluesky's RichText does: a mention opens the profile of the handle written in
 * the text slice (the facet's DID is not asked, the AppView takes a handle), a link its address, a tag Bluesky's search
 * address (BskyLink.makeSearchLink), which the app answers with Search over the saved posts. A mention whose slice is
 * not a handle and a link that is not an openable address stay plain text.
 * https://github.com/bluesky-social/social-app/blob/main/src/components/RichText.tsx#RichText
 * not ported: RichText's emoji-only enlargement, numberOfLines, suffix, selectable, onLinkPress, shouldProxyLinks,
 * disableLinks, enableTags and disableMentionFacetValidation (a Text of the card or the post takes those itself)
 * not ported: ProfileHoverCard and RichTextTag (the hover card and the tag's menu of mute and search: a tag leads straight to the search)
 * not ported: toShortUrl (Bluesky shortens the link's text on the client; the record's text is already shortened)
 */
internal fun facetText(text: String, facets: List<Facet>, linkColor: Color): AnnotatedString {
    val style = TextLinkStyles(style = SpanStyle(color = linkColor))
    val richText = RichText(text, facets)
    return buildAnnotatedString {
        for (segment in richText.segments()) {
            val link = segment.link
            val mention = segment.mention
            val tag = segment.tag
            val handle = segment.text.removePrefix("@")
            if (mention != null && BskyLink.isActor(handle)) {
                withLink(LinkAnnotation.Url(BskyLink.profileUrl(handle), style)) { append(segment.text) }
            } else if (link?.uri != null && SafeLinks.isOpenable(link.uri)) {
                withLink(LinkAnnotation.Url(link.uri, style)) { append(segment.text) }
            } else if (tag?.tag != null) {
                withLink(LinkAnnotation.Url(BskyLink.makeSearchLink("#" + tag.tag), style)) { append(segment.text) }
            } else {
                append(segment.text)
            }
        }
    }
}

/**
 * Turns post text into tappable text: @mentions open the profile in the app,
 * and link tokens open the full URL.
 *
 * Bluesky shortens links in the visible text ("example.com/some/pa…") while
 * the real URLs arrive separately in [links], from the post's link facets. A token is linked only when it
 * matches one of those real URLs, or when it is a complete http(s) URL itself.
 * That is what keeps "Node.js" or "e.g." from becoming broken links.
 */
internal fun linkify(
    text: String,
    links: List<String>,
    linkColor: Color,
    onMention: (String) -> Unit,
    facets: List<Facet> = emptyList()
): AnnotatedString {
    // A post read with its facets is drawn from them; one cached before them keeps the guess below.
    if (facets.isNotEmpty()) return facetText(text, facets, linkColor)
    val style = TextLinkStyles(style = SpanStyle(color = linkColor))
    val matches = TOKEN.findAll(text).toList()

    return buildAnnotatedString {
        var cursor = 0
        for (match in matches) {
            val token = match.value
            val mention = match.groups[1]?.value
            val target = if (mention == null) resolve(token, links) else null
            if (mention == null && target == null) continue

            append(text.substring(cursor, match.range.first))
            if (mention != null) {
                withLink(LinkAnnotation.Clickable("mention:$mention", style) { onMention(mention) }) {
                    append(token)
                }
            } else {
                withLink(LinkAnnotation.Url(target!!, style)) { append(token) }
            }
            cursor = match.range.last + 1
        }
        append(text.substring(cursor))
    }
}

private fun resolve(token: String, links: List<String>): String? {
    val wanted = normal(token)
    if (wanted.length < 4) return null
    links.firstOrNull { normal(it).startsWith(wanted) }?.let { return it }
    return token.takeIf { it.startsWith("https://") || it.startsWith("http://") }
}

private fun normal(value: String): String =
    value.removePrefix("https://").removePrefix("http://").removePrefix("www.")
        .trimEnd('…', '.', ',', ')', '!', '?')

/**
 * Group 1 is a mention handle, a whole domain on Bluesky, @jane.bsky.social,
 * ending on a letter so the full stop after a mention is not taken in.
 * Anything else is a link candidate.
 */
private val TOKEN = Regex(
    "(?<![\\w@])@((?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z][A-Za-z0-9-]{0,61})" +
        "|(?:https?://)?(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}(?:/[^\\s]*)?"
)
