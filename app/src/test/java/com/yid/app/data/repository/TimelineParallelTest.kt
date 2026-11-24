package com.yid.app.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Home reads two accounts at a time, never three: the semaphore of LinkedOut's TimelineRepository,
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/data/repository/TimelineRepository.kt#MAX_PARALLEL_FETCHES
 */
class TimelineParallelTest {

    @Test
    fun `two reads run together and never three`() = runBlocking {
        val running = AtomicInteger(0)
        val most = AtomicInteger(0)

        val done = TimelineRepository.inParallel((1..8).toList()) { account ->
            val now = running.incrementAndGet()
            most.updateAndGet { maxOf(it, now) }
            delay(20)
            running.decrementAndGet()
            account
        }

        assertEquals(2, TimelineRepository.MAX_PARALLEL_FETCHES)
        assertEquals(2, most.get())
        assertEquals((1..8).toList(), done)
    }
}
