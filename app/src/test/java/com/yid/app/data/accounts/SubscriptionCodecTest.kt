package com.yid.app.data.accounts

import com.yid.app.core.model.FollowedAccount
import com.yid.app.core.model.FollowedAccount.Companion.MAIN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionCodecTest {

    @Test
    fun `folders survive an export and an import`() {
        val accounts = listOf(
            FollowedAccount("nasa.gov", folder = "Science"),
            FollowedAccount("esa.int", folder = "Science"),
            FollowedAccount("nytimes.com", folder = "News"),
            FollowedAccount("someone.bsky.social")
        )
        val file = SubscriptionCodec.export(accounts, listOf(MAIN, "News", "Science", "Empty"), nowMillis = 0)

        assertEquals(
            listOf(
                SubscriptionCodec.Entry("nasa.gov", "Science"),
                SubscriptionCodec.Entry("esa.int", "Science"),
                SubscriptionCodec.Entry("nytimes.com", "News"),
                SubscriptionCodec.Entry("someone.bsky.social", null)
            ),
            SubscriptionCodec.import(file)
        )
        assertTrue("\"name\":\"Empty\"" in file)
        assertTrue("\"name\":\"Main\"" !in file)
    }

    @Test
    fun `plain text takes handles and bsky links, a post link gives its author`() {
        assertEquals(
            listOf(SubscriptionCodec.Entry("nasa.gov"), SubscriptionCodec.Entry("esa.int"), SubscriptionCodec.Entry("jane.bsky.social")),
            SubscriptionCodec.import("https://bsky.app/profile/nasa.gov/post/3mw2cdr44fc2a\n@esa.int, jane.bsky.social")
        )
    }
}
