package com.yid.app.core.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.model.MediaItem
import com.yid.app.core.model.MediaType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * What locks point 25 of requests.md: a saved post's photo, a video's cover and the video resolve to files on the
 * phone with no network. Run on the JVM under Robolectric for the app's external files folder, with real files:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/core/media/SavedPostOfflineTest.kt
 * The rule under test is AntennaPod's, the local file when it is available, else the stream:
 * https://github.com/AntennaPod/AntennaPod/blob/develop/playback/base/src/main/java/de/danoeh/antennapod/playback/base/MediaItemAdapter.java#L85-L90
 *
 * not ported: files saved before the account folders are deleted at the refresh (Yiḍ has no account folders)
 * not ported: a stored video without an address becomes playable after replaceMedia, and replaceMedia leaves a playable
 * video and an unknown post alone (LinkedIn's profile page names a video without its address, a Bluesky video always
 * has its playlist: no replaceMedia, no `playable`)
 * not ported: post and feed (they only built the posts of the replaceMedia tests)
 */
@RunWith(AndroidJUnit4::class)
@Config(application = android.app.Application::class, sdk = [36])
class SavedPostOfflineTest {

    private val context get() = RuntimeEnvironment.getApplication()

    // Post ids are at-URIs, which no file name can hold as they are.
    private val post42 = "at://did:plc:abc123/app.bsky.feed.post/3k42"
    private val post43 = "at://did:plc:abc123/app.bsky.feed.post/3k43"
    private val post45 = "at://did:plc:abc123/app.bsky.feed.post/3k45"
    private val post46 = "at://did:plc:abc123/app.bsky.feed.post/3k46"

    private val photo = MediaItem("https://cdn.bsky.app/img/feed_thumbnail/p", "https://cdn.bsky.app/img/feed_fullsize/p", MediaType.PHOTO)
    private val video = MediaItem("https://video.bsky.app/watch/did/cid/thumbnail.jpg", "https://video.bsky.app/watch/did/cid/playlist.m3u8", MediaType.VIDEO)

    private fun save(file: java.io.File) {
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(1, 2, 3))
    }

    @Test
    fun `a saved photo, a cover and a video resolve to local files`() = runBlocking {
        val store = OfflineMedia(context)
        save(store.fileFor(post42, "SomeOne", 0, photo))
        save(store.stillFor(post42, "SomeOne", 1, video))
        save(store.fileFor(post42, "SomeOne", 1, video))
        store.refresh()

        val shownPhoto = resolve(store, post42, 0, photo)
        assertTrue(shownPhoto.offline)
        assertEquals(store.fileFor(post42, "SomeOne", 0, photo).absolutePath, shownPhoto.playback)
        assertEquals(shownPhoto.playback, shownPhoto.preview)

        val shownVideo = resolve(store, post42, 1, video)
        assertTrue(shownVideo.offline)
        assertEquals(store.fileFor(post42, "SomeOne", 1, video).absolutePath, shownVideo.playback)
        assertEquals(store.stillFor(post42, "SomeOne", 1, video).absolutePath, shownVideo.preview)
    }

    @Test
    fun `an unsaved video is the network address, and the other address of a saved post still finds its file`() = runBlocking {
        val store = OfflineMedia(context)
        save(store.fileFor(post43, "someone", 0, photo))
        store.refresh()

        assertFalse(resolve(store, post43, 1, video).offline)
        assertEquals(video.downloadUrl, resolve(store, post43, 1, video).playback)
        // The CDN may name the same picture otherwise: the file is found by post and position.
        val renamed = photo.copy(previewUrl = "https://cdn.bsky.app/img/feed_thumbnail/q", downloadUrl = "https://cdn.bsky.app/img/feed_fullsize/q")
        assertTrue(resolve(store, post43, 0, renamed).offline)
    }

    @Test
    fun `a video saved without its still shows its network cover`() = runBlocking {
        val store = OfflineMedia(context)
        save(store.fileFor(post43, "someone", 2, video))
        store.refresh()

        val shown = resolve(store, post43, 2, video)
        assertTrue(shown.offline)
        assertEquals(video.previewUrl, shown.preview)
    }

    @Test
    fun `a cover is no copy of its own, the list of saved files leaves it out`() = runBlocking {
        val store = OfflineMedia(context)
        save(store.fileFor(post43, "someone", 3, video))
        save(store.stillFor(post43, "someone", 3, video))
        store.refresh()

        assertEquals(1, store.entries().count { it.postId == post43 && it.index == 3 })
        assertNotNull(store.stillPaths()["$post43/3"])
    }

    @Test
    fun `retention drops the files of a post the cache no longer holds`() = runBlocking {
        val store = OfflineMedia(context)
        save(store.fileFor(post45, "someone", 0, photo))
        save(store.fileFor(post46, "someone", 0, photo))
        save(store.stillFor(post46, "someone", 1, video))
        store.refresh()

        store.keepOnly(setOf(post45))

        assertNotNull(store.localPath(post45, 0))
        assertNull(store.localPath(post46, 0))
        assertNull(store.localStill(post46, 1))
    }
}
