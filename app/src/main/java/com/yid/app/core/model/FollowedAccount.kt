package com.yid.app.core.model

import com.yid.app.core.link.BskyLink

import kotlinx.serialization.Serializable

@Serializable
data class FollowedAccount(
    val handle: String,
    val displayName: String? = null,
    /**
     * The folder this account is filed in. Never blank: an account put
     * nowhere is in [MAIN], which is also how a file written before folders
     * existed loads. The list of folders, empty ones included, is kept by
     * [com.yid.app.data.accounts.AccountStore].
     */
    val folder: String = MAIN,
    val addedAtMillis: Long = 0L
) {
    companion object {
        /** Where an account goes when it has been put nowhere. Cannot be renamed or deleted. */
        const val MAIN = "Main"

        /**
         * A Bluesky handle is a domain name, nytimes.com or jane.bsky.social, and
         * an account can also be named by its DID. Handles are lowercased,
         * as Bluesky treats them. Validated locally so a typo fails at once
         * instead of costing a request.
         */
        fun normalise(raw: String): String? {
            val cleaned = raw.trim().removePrefix("@").trimEnd('/', '.')
            val value = if (cleaned.startsWith("did:")) cleaned else cleaned.lowercase()
            return value.takeIf { BskyLink.isActor(it) }
        }
    }
}
