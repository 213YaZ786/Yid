package com.yid.app.core.common

/**
 * Turns an AppError into something a human reads, plus the action that
 * actually helps. Kept out of the UI so it can be unit tested and localised.
 */
data class ErrorPresentation(
    val headline: String,
    val explanation: String,
    val action: ErrorAction
)

enum class ErrorAction { RETRY, OPEN_LOG, NONE }

fun AppError.present(): ErrorPresentation = when (this) {
    AppError.Offline -> ErrorPresentation(
        headline = "No internet connection",
        explanation = "Saved posts are still readable. New ones load as soon as you are back online.",
        action = ErrorAction.RETRY
    )
    is AppError.DnsFailure -> ErrorPresentation(
        headline = "Can't reach Bluesky",
        explanation = "$host can't be found from this network right now. Saved posts are still readable.",
        action = ErrorAction.RETRY
    )
    is AppError.TlsFailure -> ErrorPresentation(
        headline = "The connection is not secure",
        explanation = "The connection to $host could not be verified, so Yiḍ stopped rather than take a risk. " +
            "A public Wi-Fi that intercepts traffic does this.",
        action = ErrorAction.OPEN_LOG
    )
    is AppError.Timeout -> ErrorPresentation(
        headline = "Bluesky is slow to answer",
        explanation = "No answer from $host in ${millis / 1000} seconds. Try again in a moment.",
        action = ErrorAction.RETRY
    )
    is AppError.ClientRefused -> ErrorPresentation(
        headline = "Bluesky refused the request",
        explanation = "$host answered $status. The activity log shows what was asked.",
        action = ErrorAction.OPEN_LOG
    )
    is AppError.RateLimited -> ErrorPresentation(
        headline = "Asked to slow down",
        explanation = retryAfterSeconds?.let { "Bluesky asked Yiḍ to wait $it seconds. It tries again on its own." }
            ?: "Bluesky asked Yiḍ to slow down. It tries again on its own.",
        action = ErrorAction.NONE
    )
    is AppError.ServerError -> ErrorPresentation(
        headline = "Bluesky is having trouble",
        explanation = "The service failed, not your phone or your connection. Try again later.",
        action = ErrorAction.RETRY
    )
    is AppError.AccountNotFound -> ErrorPresentation(
        headline = "@$handle doesn't exist",
        explanation = "No Bluesky account has this handle. It may have changed: search for the name in Accounts.",
        action = ErrorAction.NONE
    )
    is AppError.AccountUnavailable -> ErrorPresentation(
        headline = "Can't show @$handle",
        explanation = reason ?: "This account can't be shown to logged out readers.",
        action = ErrorAction.NONE
    )
    is AppError.PostUnavailable -> ErrorPresentation(
        headline = "This post can't be shown",
        explanation = reason ?: "It was deleted, or its author hides it from logged out readers.",
        action = ErrorAction.NONE
    )
    is AppError.StorageFailure -> ErrorPresentation(
        headline = "Saved posts couldn't be used",
        explanation = "Yiḍ could not read or write its saved posts. Check that the phone has free space.",
        action = ErrorAction.NONE
    )
    is AppError.Unknown -> ErrorPresentation(
        headline = "Something unexpected happened",
        explanation = "The activity log in Settings has the details.",
        action = ErrorAction.OPEN_LOG
    )
}
