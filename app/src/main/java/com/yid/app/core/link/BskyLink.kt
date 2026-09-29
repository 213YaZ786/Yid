package com.yid.app.core.link

/**
 * What a Bluesky link points at. Pure, no Android.
 *
 * Reads the web app's addresses, bsky.app/profile/<who> and
 * bsky.app/profile/<who>/post/<id>, and AT URIs,
 * at://<who>/app.bsky.feed.post/<id>. <who> is a handle or a DID, and both
 * are accepted by every public endpoint Yiḍ uses, so neither is resolved
 * here. Anything else, a feed, a list, a search, is left to the browser.
 */
sealed interface BskyLink {
    data class Profile(val actor: String) : BskyLink

    data class Post(val actor: String, val rkey: String) : BskyLink {
        /** The post's AT URI, which is also its id in Yiḍ. */
        val atUri: String get() = "at://$actor/app.bsky.feed.post/$rkey"
    }

    companion object {

        fun parse(raw: String): BskyLink? {
            val trimmed = raw.trim()
            if (trimmed.startsWith("at://")) return fromAtUri(trimmed)
            val scheme = trimmed.substringBefore("://", "").lowercase()
            if (scheme != "https" && scheme != "http") return null
            val rest = trimmed.substringAfter("://")
            val host = rest.substringBefore('/').substringBefore('?').lowercase()
            if (host !in HOSTS) return null
            val segments = rest.substringAfter('/', "").substringBefore('?').substringBefore('#')
                .split('/').filter { it.isNotEmpty() }
            if (segments.size < 2 || segments[0] != "profile") return null
            val actor = segments[1].removePrefix("@").takeIf(::isActor) ?: return null
            return when {
                segments.size == 2 -> Profile(actor)
                segments.size == 4 && segments[2] == "post" && isRkey(segments[3]) -> Post(actor, segments[3])
                else -> null
            }
        }

        /** The post an AT URI names, or null when it names something else. */
        fun fromAtUri(uri: String): Post? {
            val parts = uri.removePrefix("at://").split('/')
            if (parts.size != 3 || parts[1] != "app.bsky.feed.post") return null
            if (!isActor(parts[0]) || !isRkey(parts[2])) return null
            return Post(parts[0], parts[2])
        }

        /** The first http or https URL inside shared text, for the share sheet. */
        fun firstUrlIn(text: String): String? = URL.find(text)?.value?.trimEnd('.', ',', ')', '!', '?')

        /** The web address of a post, to open or share. */
        fun webUrl(atUri: String): String? = fromAtUri(atUri)?.let { "https://bsky.app/profile/${it.actor}/post/${it.rkey}" }

        fun profileUrl(actor: String): String = "https://bsky.app/profile/$actor"

        /** A handle is a domain name, a DID starts with did:. */
        fun isActor(value: String): Boolean = DID.matches(value) || HANDLE.matches(value)

        private fun isRkey(value: String) = RKEY.matches(value)

        private val HOSTS = setOf("bsky.app", "www.bsky.app", "staging.bsky.app")
        private val HANDLE = Regex("^(?=.{3,253}$)([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z][a-zA-Z0-9-]{0,61}$")
        private val DID = Regex("^did:(plc|web):[a-zA-Z0-9._:%-]{1,200}$")
        private val RKEY = Regex("^[a-zA-Z0-9._~:-]{1,512}$")
        private val URL = Regex("""https?://\S+""")
    }
}
