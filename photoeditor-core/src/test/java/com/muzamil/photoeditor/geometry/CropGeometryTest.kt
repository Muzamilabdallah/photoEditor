package com.muzamil.photoeditor.geometry

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropGeometryTest {

    /** Square bitmap in a wide container — bitmap top/bottom get cropped. */
    private val wideContainer = CropGeometry(
        bitmapWidth = 2000f,
        bitmapHeight = 2000f,
        containerWidth = 1000f,
        containerHeight = 500f,
    )

    @Test
    fun `crop uses the larger scale so the container is fully covered`() {
        // scaleX = 0.5, scaleY = 0.25 → Crop picks 0.5
        assertEquals(0.5f, wideContainer.scale, 0f)
    }

    @Test
    fun `crop offsets are the hidden overflow split symmetrically`() {
        // Scaled bitmap is 1000x1000 in a 1000x500 container.
        assertEquals(0f, wideContainer.cropOffsetX, 0f)
        assertEquals(250f, wideContainer.cropOffsetY, 0f)
    }

    @Test
    fun `container origin maps to the first visible bitmap ratio`() {
        val ratio = wideContainer.screenToRatio(Offset.Zero)
        assertEquals(0f, ratio.x, 1e-6f)
        assertEquals(0.25f, ratio.y, 1e-6f)
    }

    @Test
    fun `container far corner maps to the last visible bitmap ratio`() {
        val ratio = wideContainer.screenToRatio(Offset(1000f, 500f))
        assertEquals(1f, ratio.x, 1e-6f)
        assertEquals(0.75f, ratio.y, 1e-6f)
    }

    @Test
    fun `screenToRatio and ratioToScreen are inverses for interior points`() {
        val points = listOf(
            Offset(0f, 0f),
            Offset(123.5f, 77.25f),
            Offset(500f, 250f),
            Offset(999f, 499f),
        )
        for (screen in points) {
            val roundTrip = wideContainer.ratioToScreen(wideContainer.screenToRatio(screen))
            assertEquals("x for $screen", screen.x, roundTrip.x, 1e-3f)
            assertEquals("y for $screen", screen.y, roundTrip.y, 1e-3f)
        }
    }

    @Test
    fun `matching aspect ratios need no crop offset`() {
        val g = CropGeometry(1600f, 900f, 800f, 450f)
        assertEquals(0.5f, g.scale, 0f)
        assertEquals(0f, g.cropOffsetX, 0f)
        assertEquals(0f, g.cropOffsetY, 0f)
        val center = g.screenToRatio(Offset(400f, 225f))
        assertEquals(0.5f, center.x, 1e-6f)
        assertEquals(0.5f, center.y, 1e-6f)
    }

    @Test
    fun `tall container crops the bitmap horizontally`() {
        // Landscape bitmap in a portrait container.
        val g = CropGeometry(4000f, 2000f, 500f, 1000f)
        assertEquals(0.5f, g.scale, 0f) // scaleY = 0.5 > scaleX = 0.125
        assertEquals(750f, g.cropOffsetX, 0f)
        assertEquals(0f, g.cropOffsetY, 0f)
        // Horizontal center of the container is the bitmap's horizontal center.
        assertEquals(0.5f, g.screenToRatio(Offset(250f, 500f)).x, 1e-6f)
    }

    @Test
    fun `points outside the container clamp to the ratio range`() {
        val below = wideContainer.screenToRatio(Offset(-50f, -50f))
        assertEquals(0f, below.x, 0f)
        assertTrue(below.y in 0f..1f)

        val beyond = wideContainer.screenToRatio(Offset(2000f, 2000f))
        assertEquals(1f, beyond.x, 0f)
        assertTrue(beyond.y in 0f..1f)
    }

    @Test
    fun `unmeasured geometry is invalid and maps everything to zero`() {
        val unmeasured = CropGeometry(0f, 0f, 0f, 0f)
        assertFalse(unmeasured.isValid)
        assertEquals(Offset.Zero, unmeasured.screenToRatio(Offset(100f, 100f)))
        assertEquals(Offset.Zero, unmeasured.ratioToScreen(Offset(0.5f, 0.5f)))

        val noBitmap = CropGeometry(0f, 0f, 1000f, 500f)
        assertFalse(noBitmap.isValid)
        assertEquals(Offset.Zero, noBitmap.screenToRatio(Offset(100f, 100f)))
    }
}
