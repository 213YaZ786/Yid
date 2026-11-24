package com.yid.app.feature.media

import com.yid.app.feature.media.SavedMediaViewModel.Auto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaViewModel.kt#build
// for Yiḍ: a video's cell takes its saved still, a photo its own file.
class SavedMediaBuildTest {

    private fun cells(state: SavedMediaUiState) = state.rows.filterIsInstance<SavedCell>().associateBy { it.key }

    @Test
    fun `a video with a still gets its path as thumb`() {
        val files = listOf(Auto("ada.bsky.social", "3kpost", 0, "/m/ada/3kpost_0.mp4", 5_000))
        val stills = mapOf("3kpost/0" to "/m/ada/3kpost_0.still.jpg")

        val cell = cells(SavedMediaViewModel.build(files, stills)).getValue("c/3kpost/0")

        assertEquals("/m/ada/3kpost_0.still.jpg", cell.thumb)
        assertEquals(true, cell.video)
    }

    @Test
    fun `a video without a still keeps no thumb`() {
        val files = listOf(Auto("ada.bsky.social", "3kpost", 0, "/m/ada/3kpost_0.mp4", 5_000))

        val cell = cells(SavedMediaViewModel.build(files, mapOf("3kother/0" to "/m/x.jpg"))).getValue("c/3kpost/0")

        assertNull(cell.thumb)
        assertEquals(true, cell.video)
    }

    @Test
    fun `a photo keeps its own file and ignores a still`() {
        val files = listOf(Auto("ada.bsky.social", "3kpost", 1, "/m/ada/3kpost_1.jpg", 4_000))

        val cell = cells(SavedMediaViewModel.build(files, mapOf("3kpost/1" to "/m/y.jpg"))).getValue("c/3kpost/1")

        assertEquals("/m/ada/3kpost_1.jpg", cell.thumb)
        assertEquals(false, cell.video)
    }
}
