package com.yid.app.core.media

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Which listed files are saved, in the shape of LinkedOut's test:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/core/media/OfflineMediaUnfinishedTest.kt
 * The transfer states come from DownloadManager, see
 * https://github.com/rdoeffinger/Dictionary/blob/main/src/com/hughes/android/dictionary/DictionaryManagerActivity.kt#isDownloadActive
 */
class OfflineMediaUnfinishedTest {

    private val running = File("/media/someone__42__0.mp4")
    private val failed = File("/media/someone__42__1.mp4")
    private val finished = File("/media/someone__43__0.jpg")
    private val all = listOf(running, failed, finished)

    private val unfinished = setOf(running.absolutePath)
    private val failedPaths = setOf(failed.absolutePath)
    private val active = OfflineMedia.isDownloadActive(all, unfinished, failedPaths, cancel = false)
    private val cancelled = OfflineMedia.isDownloadActive(all, unfinished, failedPaths, cancel = true)
    private val indexed = all - active.toSet() - cancelled.toSet()

    @Test
    fun `a file whose transfer runs is not indexed and not deleted`() {
        assertEquals(listOf(running), active)
        assertEquals(false, running in indexed)
        assertEquals(false, running in cancelled)
    }

    @Test
    fun `a failed transfer is to be deleted so a retry is not blocked`() {
        assertEquals(listOf(failed), cancelled)
        assertEquals(false, failed in indexed)
    }

    @Test
    fun `a finished file is indexed`() {
        assertEquals(listOf(finished), indexed)
    }

    @Test
    fun `with no transfer known every file is indexed`() {
        assertEquals(emptyList<File>(), OfflineMedia.isDownloadActive(all, emptySet(), emptySet(), cancel = false))
        assertEquals(emptyList<File>(), OfflineMedia.isDownloadActive(all, emptySet(), emptySet(), cancel = true))
    }
}
