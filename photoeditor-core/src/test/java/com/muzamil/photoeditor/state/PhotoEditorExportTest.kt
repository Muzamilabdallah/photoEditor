package com.muzamil.photoeditor.state

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.muzamil.photoeditor.domain.EditorError
import com.muzamil.photoeditor.domain.EditorException
import com.muzamil.photoeditor.export.ExportFormat
import com.muzamil.photoeditor.export.SaveDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import android.graphics.Color as AColor

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PhotoEditorExportTest {

    private lateinit var state: PhotoEditorState

    private fun image(w: Int = 300, h: Int = 200) =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(AColor.WHITE) }

    @Before
    fun setUp() {
        state = PhotoEditorState(CoroutineScope(Dispatchers.Unconfined))
    }

    // ── loadImage ─────────────────────────────────────────────────────────────

    @Test
    fun `first load keeps restored edits`() {
        // Process-death restore: elements come back from saved state, then
        // the app re-decodes the same image and loads it.
        state.addText("restored")
        state.loadImage(image())
        assertEquals(1, state.editorState.elements.size)
    }

    @Test
    fun `loading a different image clears edits, selection and history`() {
        state.loadImage(image())
        state.addText("old photo")
        state.selectElement(state.editorState.elements.single().id)

        state.loadImage(image(100, 100))

        assertTrue(state.editorState.elements.isEmpty())
        assertNull(state.editorState.selectedElementId)
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
    }

    @Test
    fun `keepEdits = true keeps overlays when replacing the image`() {
        state.loadImage(image())
        state.addText("keep me")
        state.loadImage(image(100, 100), keepEdits = true)
        assertEquals(1, state.editorState.elements.size)
    }

    @Test
    fun `software bitmaps are loaded as-is`() {
        val bmp = image()
        state.loadImage(bmp)
        assertSame(bmp, state.editorState.backgroundBitmap)
    }

    // ── export API ────────────────────────────────────────────────────────────

    @Test
    fun `renderBitmap returns the full-resolution edit`() = runBlocking {
        state.loadImage(image(300, 200))
        state.updateDrawingStyle(strokeWidthRatio = 0.05f) // 10px, so the centre pixel is solid
        state.startDrawing(Offset(0f, 0.5f))
        state.continueDrawing(listOf(Offset(1f, 0.5f)))
        state.finishDrawing()

        val out = state.renderBitmap().getOrThrow()
        assertEquals(300, out.width)
        assertEquals(200, out.height)
        assertEquals(AColor.BLACK, out.getPixel(150, 100))
    }

    @Test
    fun `export without an image fails with NoImage`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val error = (state.export(context).exceptionOrNull() as EditorException).error
        assertEquals(EditorError.NoImage, error)
        assertEquals(EditorError.NoImage, (state.renderBitmap().exceptionOrNull() as EditorException).error)
    }

    @Test
    fun `export to a file writes a decodable image and returns a file uri`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        state.loadImage(image(300, 200))
        val file = File(context.filesDir, "out/edit.png")

        val uri = state.export(context, SaveDestination.File(file), ExportFormat.Png).getOrThrow()

        assertEquals("file", uri.scheme)
        assertEquals(file.absolutePath, uri.path)
        val decoded = BitmapFactory.decodeFile(file.absolutePath)
        assertEquals(300, decoded.width)
    }

    @Test
    fun `exportTo writes the bytes to a stream`() = runBlocking {
        state.loadImage(image(300, 200))
        val bytes = ByteArrayOutputStream()
        state.exportTo(bytes, ExportFormat.Jpeg(80)).getOrThrow()
        val decoded = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())
        assertEquals(200, decoded.height)
    }

    @Test
    fun `a write failure is reported as ExportFailed, not thrown`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        state.loadImage(image())
        // A directory can't be opened as an output file.
        val dir = File(context.filesDir, "a-directory").apply { mkdirs() }

        val result = state.export(context, SaveDestination.File(dir))

        val error = (result.exceptionOrNull() as EditorException).error
        assertTrue(error is EditorError.ExportFailed)
    }

    @Test
    fun `cancellation is propagated, not reported as a failure`() = runBlocking {
        state.loadImage(image(2000, 2000))
        val job = async(Dispatchers.Default) { state.renderBitmap() }
        job.cancel()
        try {
            job.await()
            // Finishing before the cancel landed is fine; a *failed* Result is not.
        } catch (e: CancellationException) {
            return@runBlocking
        }
    }

    // ── saveEditedImage (fire-and-forget) ─────────────────────────────────────

    @Test
    fun `saveEditedImage publishes the file path on success`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        state.loadImage(image())
        val file = File(context.filesDir, "saved.jpg")

        state.saveEditedImage(context, SaveDestination.File(file))
        waitUntil { !state.editorState.isLoading && state.saveSuccess != null }

        assertEquals(file.absolutePath, state.saveSuccess)
        assertNull(state.editorState.error)
    }

    @Test
    fun `saveEditedImage reports a typed error without an image`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        state.saveEditedImage(context)
        assertEquals(EditorError.NoImage, state.editorState.error)

        state.clearError()
        assertNull(state.editorState.error)
    }

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > end) fail("timed out")
            Thread.sleep(10)
        }
    }
}
