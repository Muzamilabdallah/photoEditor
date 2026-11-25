package io.github.photoforge.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ImageGeometryTest {

    // 4:3 landscape photo in a portrait phone band: Fit by width, letterboxed top/bottom.
    private val portrait = ImageGeometry(4000f, 3000f, 1080f, 1738f)

    // Same photo after rotating to landscape: Fit by height, pillarboxed left/right.
    private val landscape = ImageGeometry(4000f, 3000f, 2316f, 900f)

    @Test
    fun `fit shows the whole image, scaled by the smaller axis`() {
        assertEquals(0.27f, portrait.scale, 1e-4f)
        assertEquals(IntSize(1080, 810), portrait.imageSize)
        assertEquals(IntOffset(0, 464), portrait.imageOffset)
    }

    @Test
    fun `pillarbox when the container is wider than the image`() {
        assertEquals(0.3f, landscape.scale, 1e-4f)
        assertEquals(IntSize(1200, 900), landscape.imageSize)
        assertEquals(IntOffset(558, 0), landscape.imageOffset)
    }

    @Test
    fun `size unit is the image's shorter side on screen`() {
        assertEquals(810f, portrait.sizeUnitPx, 0f)
        assertEquals(900f, landscape.sizeUnitPx, 0f)
    }

    @Test
    fun `ratio and screen round-trip`() {
        val ratio = Offset(0.25f, 0.8f)
        val back = portrait.screenToRatio(portrait.ratioToScreen(ratio))
        assertEquals(ratio.x, back.x, 1e-5f)
        assertEquals(ratio.y, back.y, 1e-5f)
    }

    @Test
    fun `an element stays on the same image pixel across a rotation`() {
        // The model stores the ratio, so the point it covers on the photo is
        // the same in both window shapes — only its screen position moves.
        val ratio = Offset(0.9f, 0.1f)
        fun toBitmapPx(g: ImageGeometry): Offset {
            val screen = g.ratioToScreen(ratio)
            val r = g.screenToRatio(screen)
            return Offset(r.x * g.bitmapWidth, r.y * g.bitmapHeight)
        }
        val a = toBitmapPx(portrait)
        val b = toBitmapPx(landscape)
        assertEquals(3600f, a.x, 0.5f)
        assertEquals(300f, a.y, 0.5f)
        assertEquals(a.x, b.x, 0.5f)
        assertEquals(a.y, b.y, 0.5f)
    }

    @Test
    fun `touches in the letterbox clamp to the nearest image edge`() {
        assertEquals(Offset(0.5f, 0f), portrait.screenToRatio(Offset(540f, 10f)))
        assertEquals(Offset(0.5f, 1f), portrait.screenToRatio(Offset(540f, 1730f)))
        assertEquals(Offset(0f, 0.5f), landscape.screenToRatio(Offset(5f, 450f)))
    }

    @Test
    fun `unmeasured geometry is invalid and maps to zero`() {
        val unmeasured = ImageGeometry(4000f, 3000f, 0f, 0f)
        assertFalse(unmeasured.isValid)
        assertEquals(IntSize.Zero, unmeasured.imageSize)
        assertEquals(Offset.Zero, unmeasured.screenToRatio(Offset(10f, 10f)))

        val noBitmap = ImageGeometry(0f, 0f, 1080f, 1738f)
        assertFalse(noBitmap.isValid)
    }
}
