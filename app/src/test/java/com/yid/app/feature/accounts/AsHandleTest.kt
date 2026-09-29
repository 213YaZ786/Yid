package com.yid.app.feature.accounts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AsHandleTest {

    @Test
    fun `a typed handle is read as is, lowercased`() {
        assertEquals("nasa.gov", AccountsViewModel.asHandle("@NASA.gov "))
        assertEquals("jane.bsky.social", AccountsViewModel.asHandle("jane.bsky.social"))
    }

    @Test
    fun `a bare word is a name to search for, not a handle`() {
        assertNull(AccountsViewModel.asHandle("nasa"))
    }

    @Test
    fun `a pasted profile or post link gives the account`() {
        assertEquals("nasa.gov", AccountsViewModel.asHandle("https://bsky.app/profile/nasa.gov"))
        assertEquals("nasa.gov", AccountsViewModel.asHandle("bsky.app/profile/nasa.gov/post/3mw2cdr44fc2a"))
        assertEquals("did:plc:z72i7hdynmk6r22z27h6tvur", AccountsViewModel.asHandle("https://bsky.app/profile/did:plc:z72i7hdynmk6r22z27h6tvur"))
    }

    @Test
    fun `a link to something else is not an account`() {
        assertNull(AccountsViewModel.asHandle("https://bsky.app/search?q=space"))
        assertNull(AccountsViewModel.asHandle("https://example.com/profile/nasa.gov"))
    }
}
