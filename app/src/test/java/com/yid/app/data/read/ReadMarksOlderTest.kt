package com.yid.app.data.read

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The previous visit decides what is new, the cases of LinkedOut's test:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/data/read/ReadPostsLastVisitTest.kt
 * The folder is JUnit 4's TemporaryFolder rule.
 */
class ReadMarksOlderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val visit = 1_000L

    private fun store(previousVisit: Long? = visit): ReadMarks {
        if (previousVisit != null) folder.root.resolve("last-visit.txt").writeText(previousVisit.toString())
        return ReadMarks(folder.root)
    }

    private fun ReadMarks.isUnread(id: String) = id !in read.value

    @Test
    fun `a post published before the previous visit is read`() = runBlocking {
        val marks = store()
        marks.load()
        marks.markReadBeforeLastVisit(emptyMap())

        marks.markReadIfOld(listOf("old" to visit - 1))

        assertFalse(marks.isUnread("old"))
    }

    @Test
    fun `a post published after the previous visit is not`() = runBlocking {
        val marks = store()
        marks.load()
        marks.markReadBeforeLastVisit(emptyMap())

        marks.markReadIfOld(listOf("new" to visit + 1))

        assertTrue(marks.isUnread("new"))
    }

    @Test
    fun `before the launch has read the previous visit nothing is marked`() = runBlocking {
        val marks = store()
        marks.load()

        marks.markReadIfOld(listOf("any" to visit + 1))

        assertTrue(marks.isUnread("any"))
    }

    @Test
    fun `at launch only the old ones are marked`() = runBlocking {
        val marks = store()
        marks.load()

        marks.markReadBeforeLastVisit(mapOf("old" to visit - 1, "same" to visit, "new" to visit + 1))

        assertFalse(marks.isUnread("old"))
        assertFalse(marks.isUnread("same"))
        assertTrue(marks.isUnread("new"))
    }

    @Test
    fun `the first launch has no previous visit and nothing is new`() = runBlocking {
        val marks = store(previousVisit = null)
        marks.load()

        marks.markReadBeforeLastVisit(mapOf("a" to 5L, "b" to Long.MAX_VALUE - 1))

        assertFalse(marks.isUnread("a"))
        assertFalse(marks.isUnread("b"))
    }

    @Test
    fun `the launch writes this visit for the next one`() = runBlocking {
        val marks = store()
        marks.load()

        marks.markReadBeforeLastVisit(emptyMap())

        val written = folder.root.resolve("last-visit.txt").readText().trim().toLong()
        assertTrue(written > visit)
    }
}
