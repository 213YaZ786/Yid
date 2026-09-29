package com.yid.app.core.network

import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentTest {

    /** An HTTP header refuses anything but printable ASCII: one accent and no request leaves. */
    @Test
    fun userAgentIsPlainAscii() {
        val ua = HttpClientFactory.USER_AGENT
        assertTrue(ua, ua.all { it.code in 0x20..0x7E })
    }
}
