package com.yid.app.data.bsky

import com.yid.app.core.debug.RequestLog
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.cancellation.CancellationException

/**
 * The original file of a Bluesky video, for saving it.
 *
 * Videos are played from an HLS playlist, a list of short pieces that cannot
 * be saved as one file. The file the author uploaded is still a public blob
 * on their own server (checked on 2026-09-28: 200, video/mp4, the whole
 * file). The playlist address carries the author's DID and the video's CID,
 * the DID document names the server, and the server hands the blob out.
 */
class BskyVideo(
    private val client: HttpClient,
    private val log: RequestLog
) {

    private val servers = HashMap<String, String>()
    private val lock = Mutex()

    /** The MP4 address behind [playlist], or null when it cannot be found. */
    suspend fun fileFor(playlist: String): String? = withContext(Dispatchers.IO) {
        val (did, cid) = parts(playlist) ?: return@withContext null
        val server = serverOf(did) ?: return@withContext null
        "$server/xrpc/com.atproto.sync.getBlob?did=$did&cid=$cid"
    }

    private suspend fun serverOf(did: String): String? {
        lock.withLock { servers[did] }?.let { return it }
        val documentUrl = when {
            did.startsWith("did:plc:") -> "https://plc.directory/$did"
            did.startsWith("did:web:") -> "https://${did.removePrefix("did:web:")}/.well-known/did.json"
            else -> return null
        }
        return try {
            val body = client.get(documentUrl).bodyAsText()
            val found = serviceEndpoint(body)
            log.record(
                kind = RequestLog.Kind.MEDIA,
                url = documentUrl,
                outcome = if (found != null) "video server found" else "no server in the DID document"
            )
            found?.also { lock.withLock { servers[did] = it } }
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            log.record(RequestLog.Kind.MEDIA, documentUrl, "transport failure", detail = failure.message)
            null
        }
    }

    internal companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** The DID and CID in video.bsky.app/watch/<did>/<cid>/playlist.m3u8. */
        fun parts(playlist: String): Pair<String, String>? {
            val path = playlist.substringAfter("/watch/", "").split('/')
            if (path.size < 2) return null
            val did = URLDecoder.decode(path[0], StandardCharsets.UTF_8.name())
            val cid = path[1]
            if (!did.startsWith("did:") || cid.isBlank()) return null
            return did to cid
        }

        /** The personal data server named by a DID document. */
        fun serviceEndpoint(document: String): String? {
            val root = runCatching { json.parseToJsonElement(document) as? JsonObject }.getOrNull() ?: return null
            return (root["service"] as? JsonArray).orEmpty()
                .mapNotNull { it as? JsonObject }
                .firstOrNull { (it["type"] as? JsonPrimitive)?.content == "AtprotoPersonalDataServer" }
                ?.let { (it["serviceEndpoint"] as? JsonPrimitive)?.content }
                ?.trimEnd('/')
        }
    }
}
