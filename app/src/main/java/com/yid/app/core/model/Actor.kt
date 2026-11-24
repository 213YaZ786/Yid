package com.yid.app.core.model

/** Someone found by the people search: handle, name, avatar, bio. */
data class Actor(
    val handle: String,
    val displayName: String?,
    val avatarUrl: String?,
    val description: String?,
    /** A verified account or a trusted verifier (app.bsky.actor.defs#verificationState). */
    val verified: Boolean = false
)
