package com.muzamil.photoeditor.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import android.graphics.Color as AColor

/**
 * Pixel tests for the exporter, drawn with Robolectric's native graphics
 * (real Skia), so they check what actually lands in the file.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderEditsTest {

    private fun white(w: Int = 400, h: Int = 300) =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(AColor.WHITE) }

    /** Bounding box of pixels that aren't (near) white: left, top, right, bottom. */
    private fun Bitmap.inkBounds(): IntArray {
        var l = width; var t = height; var r = -1; var b = -1
        for (y in 0 until height) for (x in 0 until width) {
            val c = getPixel(x, y)
            if (AColor.red(c) < 200 || AColor.green(c) < 200 || AColor.blue(c) < 200) {
                if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > b) b = y
            }
        }
        return intArrayOf(l, t, r, b)
    }

    private fun IntArray.centreX() = (this[0] + this[2]) / 2f
    private fun IntArray.centreY() = (this[1] + this[3]) / 2f
    private fun IntArray.height() = this[3] - this[1]

    @Test
    fun `a stroke is drawn at its bitmap ratio with a shorter-side width`() {
        val stroke = EditElement.Drawing(
            points = listOf(Offset(0.1f, 0.5f), Offset(0.9f, 0.5f)),
            color = Color.Red,
            strokeWidthRatio = 0.05f, // 15px on a 300px shorter side
        )
        val out = renderEdits(white(), listOf(stroke))

        assertEquals(AColor.RED, out.getPixel(200, 150))
        assertEquals(AColor.WHITE, out.getPixel(200, 130))
        val thickness = (0 until 300).count { out.getPixel(200, it) == AColor.RED }
        assertTrue("thickness $thickness", thickness in 13..16)
    }

    @Test
    fun `the source bitmap is not modified`() {
        val source = white()
        val out = renderEdits(source, listOf(
            EditElement.Shape(kind = EditorShape.Rect, filled = true, color = Color.Blue)
        ))
        assertNotSame(source, out)
        assertEquals(AColor.WHITE, source.getPixel(200, 150))
        assertEquals(AColor.BLUE, out.getPixel(200, 150))
    }

    @Test
    fun `a shape box is square in shorter-side units on a landscape image`() {
        val circle = EditElement.Shape(
            kind = EditorShape.Circle, filled = true, color = Color.Black,
            widthRatio = 0.5f, heightRatio = 0.5f, // 150px on a 300px shorter side
        )
        val box = renderEdits(white(), listOf(circle)).inkBounds()
        val w = box[2] - box[0]
        val h = box[3] - box[1]
        assertTrue("w=$w h=$h", abs(w - 150) <= 2 && abs(h - 150) <= 2)
        assertEquals(200f, box.centreX(), 1.5f)
        assertEquals(150f, box.centreY(), 1.5f)
    }

    @Test
    fun `text is centred on its position`() {
        val text = EditElement.Text(
            text = "Hello", color = Color.Black, fontSizeRatio = 0.1f,
            offsetRatioX = 0.25f, offsetRatioY = 0.75f,
        )
        val box = renderEdits(white(), listOf(text)).inkBounds()
        assertEquals(100f, box.centreX(), 3f)
        assertEquals(225f, box.centreY(), 6f) // glyph ink sits inside the line box
    }

    @Test
    fun `multi-line text keeps every line`() {
        fun inkHeight(s: String) = renderEdits(
            white(), listOf(EditElement.Text(text = s, color = Color.Black, fontSizeRatio = 0.08f))
        ).inkBounds().height()

        val one = inkHeight("Line")
        val three = inkHeight("Line\nLine\nLine")
        assertTrue("one=$one three=$three", three > one * 2.5f)
    }

    @Test
    fun `long text wraps at the wrap width instead of running off the image`() {
        val long = "word ".repeat(40).trim()
        val box = renderEdits(
            white(), listOf(EditElement.Text(text = long, color = Color.Black, fontSizeRatio = 0.05f))
        ).inkBounds()
        // Wrap width is 90% of 400px = 360px, centred: ink stays within 20..380.
        assertTrue("left ${box[0]}", box[0] >= 18)
        assertTrue("right ${box[2]}", box[2] <= 382)
        assertTrue("wrapped onto several lines", box.height() > 40)
    }

    @Test
    fun `arabic text renders, centred like latin text`() {
        val arabic = EditElement.Text(text = "مرحبا بالعالم", color = Color.Black, fontSizeRatio = 0.08f)
        val box = renderEdits(white(), listOf(arabic)).inkBounds()
        assertTrue("some ink drawn", box[2] > box[0])
        assertEquals(200f, box.centreX(), 4f)
    }

    @Test
    fun `text layout is right-to-left for arabic and left-to-right for latin`() {
        val paint = TextPaint().apply { textSize = 30f }
        val rtl = textLayout("مرحبا", paint, maxWidth = 1000)
        val ltr = textLayout("Hello", paint, maxWidth = 1000)
        assertEquals(android.text.Layout.DIR_RIGHT_TO_LEFT, rtl.getParagraphDirection(0))
        assertEquals(android.text.Layout.DIR_LEFT_TO_RIGHT, ltr.getParagraphDirection(0))
    }

    @Test
    fun `later elements paint over earlier ones`() {
        val under = EditElement.Shape(kind = EditorShape.Rect, filled = true, color = Color.Blue, zIndex = 0)
        val over = EditElement.Drawing(
            points = listOf(Offset(0f, 0.5f), Offset(1f, 0.5f)),
            color = Color.Red, strokeWidthRatio = 0.05f, zIndex = 1,
        )
        val out = renderEdits(white(), listOf(over, under))
        assertEquals(AColor.RED, out.getPixel(200, 150))
    }

    @Test
    fun `each export format encodes a decodable image of the full size`() {
        val source = white(320, 240)
        for (format in listOf(ExportFormat.Jpeg(90), ExportFormat.Png, ExportFormat.Webp(), ExportFormat.Webp(lossless = true))) {
            val bytes = ByteArrayOutputStream().also { assertTrue(format.compress(source, it)) }.toByteArray()
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertEquals("$format width", 320, decoded.width)
            assertEquals("$format height", 240, decoded.height)
        }
    }
}
