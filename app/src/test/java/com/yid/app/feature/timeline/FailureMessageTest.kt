package com.yid.app.feature.timeline

import com.yid.app.core.common.AppError
import com.yid.app.ui.component.failureMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Local unit tests on the JVM, Android's recipe for logic without a device:
// https://developer.android.com/training/testing/local-tests (JUnit 4 @Test, assertEquals).
// Ported from LinkedOut:
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/feature/timeline/FailureMessageTest.kt#FailureMessageTest
// Yiḍ's Blame is DEVICE, NETWORK, SERVICE, APP (core/common/AppError.kt): the kit reads SERVICE as the host and has no
// upstream case here, so the four messages are tested, and an account that does not exist is the host's too.
class FailureMessageTest {
    private val offline = AppError.Offline
    private val dns = AppError.DnsFailure("public.api.bsky.app")
    private val rate = AppError.RateLimited("public.api.bsky.app", null)
    private val gone = AppError.AccountNotFound("someone")
    private val unknown = AppError.Unknown("a page that does not parse")

    @Test
    fun deviceIgnoresCounts() {
        assertEquals(
            "You are offline" to "Saved posts stay readable. New ones load when you are back online.",
            message(listOf(offline, offline), 10)
        )
    }

    @Test
    fun networkCountsOutOfFollowed() {
        assertEquals(
            "Your connection does not reach Bluesky" to
                "2 of 10 profiles were not updated. Their saved posts stay readable.",
            message(listOf(dns, dns), 10)
        )
    }

    @Test
    fun hostAdvisesWaiting() {
        assertEquals(
            "Bluesky is holding back guests" to
                "3 of 40 profiles were not updated. Their saved posts stay readable. Try again in a few minutes.",
            message(listOf(rate, rate, rate), 40)
        )
    }

    @Test
    fun serviceRefusalsAreTheHostsToo() {
        assertEquals(
            "Bluesky is holding back guests" to
                "2 of 5 profiles were not updated. Their saved posts stay readable. Try again in a few minutes.",
            message(listOf(gone, gone), 5)
        )
    }

    @Test
    fun appCouldNotRead() {
        assertEquals(
            "Yiḍ could not read some pages" to
                "2 of 5 profiles were not updated. Their saved posts stay readable.",
            message(listOf(unknown, unknown), 5)
        )
    }

    @Test
    fun majorityWins() {
        assertEquals("Bluesky is holding back guests", message(listOf(dns, rate, rate), 9).first)
    }

    @Test
    fun tieGoesToTheFirstInOrder() {
        assertEquals("You are offline", message(listOf(unknown, offline), 9).first)
        assertEquals(
            "Your connection does not reach Bluesky",
            message(listOf(dns, rate, unknown), 9).first
        )
        assertEquals("Bluesky is holding back guests", message(listOf(unknown, rate), 9).first)
    }

    @Test
    fun singular() {
        assertEquals(
            "1 of 8 profiles was not updated. Their saved posts stay readable.",
            message(listOf(dns), 8).second
        )
    }

    @Test
    fun unknownFollowedCount() {
        assertEquals(
            "3 profiles were not updated. Their saved posts stay readable.",
            message(listOf(dns, dns, dns), 0).second
        )
        assertEquals(
            "2 profiles were not updated. Their saved posts stay readable.",
            message(listOf(unknown, unknown), 1).second
        )
        assertEquals(
            "1 profile was not updated. Their saved posts stay readable.",
            message(listOf(dns), 0).second
        )
    }

    @Test
    fun nothingFailedIsTheDownloadsNotice() {
        assertNull(failureMessage(emptyList(), 3, "Bluesky"))
    }

    // Local unit test helper, https://developer.android.com/training/testing/local-tests
    private fun message(failed: List<AppError>, followed: Int) = failureMessage(failed, followed, site = "Bluesky")!!
}
