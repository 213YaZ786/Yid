package com.yid.app.core.network

import com.yid.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header

/**
 * The single HTTP client for the app.
 *
 * expectSuccess stays false on purpose. A 429 or a 403 is information we want
 * to classify and report, not an exception thrown from deep inside a plugin.
 *
 * No cookies and no browser disguise. Bluesky's public API is meant to be
 * read by apps, so Yiḍ says what it is, with a link to its source, which
 * is what an honest client of an open network does.
 */
object HttpClientFactory {

    // Plain ASCII: an HTTP header cannot carry the dot under the d.
    const val USER_AGENT = "Yid/${BuildConfig.VERSION_NAME} (+https://github.com/213YaZ786/Yid)"

    const val CONNECT_TIMEOUT_MS = 8_000L
    const val REQUEST_TIMEOUT_MS = 15_000L

    /**
     * The biggest answer taken. The largest real one measured 2026-10-17 was
     * a busy post thread, 425 kB (a feed page of 30 posts: 70 to 200 kB, a
     * search 20 kB); 4 MB leaves ten times that and stops a hostile or
     * broken answer from being read whole into memory.
     */
    const val MAX_BODY_BYTES = 4L * 1024 * 1024

    fun create(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        followRedirects = true

        // Ported from Signal-Android's OkHttpUtil size limit, see ResponseLimit.
        engine { addInterceptor(ResponseLimit(MAX_BODY_BYTES)) }

        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
        }

        defaultRequest {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json")
        }
    }
}
