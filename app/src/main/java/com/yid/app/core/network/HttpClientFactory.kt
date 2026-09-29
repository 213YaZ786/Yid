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

    const val USER_AGENT = "Yiḍ/${BuildConfig.VERSION_NAME} (+https://github.com/213YaZ786/Yiḍ)"

    const val CONNECT_TIMEOUT_MS = 8_000L
    const val REQUEST_TIMEOUT_MS = 15_000L

    fun create(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        followRedirects = true

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
