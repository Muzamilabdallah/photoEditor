package io.github.photoforge.export

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.photoforge.domain.EditElement
import io.github.photoforge.state.PhotoEditorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Checks the long-standing claim that hardware bitmaps "crash the exporter".
 * Hardware bitmaps can't be created on the JVM, so this runs on a device.
 */
@RunWith(AndroidJUnit4::class)
class HardwareBitmapExportTest {

    private fun hardwareBitmap(): Bitmap {
        val png = ByteArrayOutputStream().also {
            Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
                .apply { eraseColor(android.graphics.Color.WHITE) }
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(png))) { d, _, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_HARDWARE
        }
    }

    private val stroke = EditElement.Drawing(
        points = listOf(Offset(0.1f, 0.5f), Offset(0.9f, 0.5f)),
        color = Color.Red,
        strokeWidthRatio = 0.1f,
    )

    @Test
    fun renderEdits_acceptsAHardwareBitmapDirectly() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        val source = hardwareBitmap()
        assertEquals(Bitmap.Config.HARDWARE, source.config)

        val out = renderEdits(source, listOf(stroke))

        assertEquals(Bitmap.Config.ARGB_8888, out.config)
        assertEquals(android.graphics.Color.RED, out.getPixel(100, 50))
    }

    @Test
    fun loadImage_convertsAHardwareBitmapToSoftware() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        val state = PhotoEditorState(CoroutineScope(Dispatchers.Unconfined))
        state.loadImage(hardwareBitmap())
        assertEquals(Bitmap.Config.ARGB_8888, state.editorState.backgroundBitmap!!.config)

        val out = runBlocking { state.renderBitmap() }.getOrThrow()
        assertEquals(200, out.width)
    }
}
