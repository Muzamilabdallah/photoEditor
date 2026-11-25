package io.github.photoforge.state

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import io.github.photoforge.domain.EditElement
import io.github.photoforge.domain.EditorShape
import io.github.photoforge.domain.EditorTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoEditorStateSaverTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val saver = photoEditorStateSaver(scope)
    private val acceptAll = SaverScope { true }

    private fun roundTrip(state: PhotoEditorState): PhotoEditorState {
        val saved = with(saver) { acceptAll.save(state) }
        return saver.restore(requireNotNull(saved) { "saver produced null" })
            ?: error("restore produced null")
    }

    @Test
    fun `all element types survive a save-restore round trip`() {
        val state = PhotoEditorState(scope)
        state.addText("hello", Offset(0.3f, 0.4f))
        state.addEmoji("🎉")
        state.updateShapeTool(color = Color.Blue, strokeWidthRatio = 0.014f, filled = true)
        state.addShape(EditorShape.Star)
        state.startDrawing(Offset(0.1f, 0.1f))
        state.continueDrawing(listOf(Offset(0.2f, 0.25f), Offset(0.3f, 0.35f)))
        state.finishDrawing()

        val restored = roundTrip(state)

        assertEquals(state.editorState.elements, restored.editorState.elements)
    }

    @Test
    fun `tool selection and style defaults survive the round trip`() {
        val state = PhotoEditorState(scope)
        state.selectTool(EditorTool.Shape)
        state.updateDrawingStyle(color = Color.Red, strokeWidthRatio = 0.009f)
        state.updateTextStyle(color = Color.Yellow, sizeRatio = 0.032f)
        state.updateShapeTool(
            shape = EditorShape.Heart,
            color = Color.Cyan,
            strokeWidthRatio = 0.003f,
            filled = true,
        )

        val restored = roundTrip(state).editorState

        assertEquals(EditorTool.Shape, restored.currentTool)
        assertEquals(Color.Red, restored.drawingColor)
        assertEquals(0.009f, restored.drawingStrokeWidthRatio, 0f)
        assertEquals(Color.Yellow, restored.textColor)
        assertEquals(0.032f, restored.textSizeRatio, 0f)
        assertEquals(EditorShape.Heart, restored.shapeTool.selectedShape)
        assertEquals(Color.Cyan, restored.shapeTool.selectedColor)
        assertEquals(0.003f, restored.shapeTool.strokeWidthRatio, 0f)
        assertEquals(true, restored.shapeTool.isFilled)
    }

    @Test
    fun `selection survives the round trip`() {
        val state = PhotoEditorState(scope)
        state.addText("t")
        val id = state.editorState.elements.single().id
        state.selectElement(id)

        assertEquals(id, roundTrip(state).editorState.selectedElementId)
    }

    @Test
    fun `text styling details survive - color size weight and transform`() {
        val state = PhotoEditorState(scope)
        state.addText("styled")
        val id = state.editorState.elements.single().id
        state.selectElement(id)
        state.updateSelectedTextStyle(color = Color.Magenta, fontSizeRatio = 0.044f)
        state.updateElementTransform(id, 0.8f, 0.2f, rotation = 30f, scale = 2.5f)

        val text = roundTrip(state).editorState.elements.single() as EditElement.Text
        assertEquals(Color.Magenta, text.color)
        assertEquals(0.044f, text.fontSizeRatio, 0f)
        assertEquals(FontWeight.Bold, text.fontWeight)
        assertEquals(0.8f, text.offsetRatioX, 0f)
        assertEquals(30f, text.rotation, 0f)
        assertEquals(2.5f, text.scale, 0f)
    }

    @Test
    fun `drawing points survive exactly`() {
        val state = PhotoEditorState(scope)
        val points = List(500) { Offset(it / 500f, 1f - it / 500f) }
        state.startDrawing(points.first())
        state.continueDrawing(points.drop(1))
        state.finishDrawing()

        val drawing = roundTrip(state).editorState.elements.single() as EditElement.Drawing
        assertEquals(points, drawing.points)
    }

    @Test
    fun `restored state has no bitmap and empty history`() {
        val state = PhotoEditorState(scope)
        state.addText("t")

        val restored = roundTrip(state)

        assertNull(restored.editorState.backgroundBitmap)
        // History is intentionally not persisted.
        assertFalse(restored.canUndo)
        assertFalse(restored.canRedo)
    }

    @Test
    fun `saved payload starts with the format version`() {
        val saved = with(saver) { acceptAll.save(PhotoEditorState(scope)) } as List<*>
        assertEquals(SAVE_FORMAT_VERSION, saved.first())
    }

    @Test
    fun `state saved by 0_1_x is dropped instead of restored to the wrong place`() {
        // 0.1.x layout: no version; first entry is the selected id (String?),
        // and text/emoji/shape positions were fractions of the preview
        // container, which no longer means anything.
        val legacy = listOf<Any?>(
            null, "none", Color.Black.hashCode(), 5f, 0, 24f,
            "Rect", 0, 8f, false,
            arrayListOf(arrayListOf<Any?>("emoji", "id", 0, 0.5f, 0.5f, 0f, 1f, "😀", 48f)),
        )
        assertNull(saver.restore(legacy))
    }

    @Test
    fun `state from a newer unknown format is dropped too`() {
        assertNull(saver.restore(listOf(SAVE_FORMAT_VERSION + 1, null, "none")))
    }

    @Test
    fun `restored sizes are ratios, not screen units`() {
        val state = PhotoEditorState(scope)
        state.addText("t")
        state.addEmoji("😀")
        val restored = roundTrip(state).editorState.elements
        val text = restored[0] as EditElement.Text
        val emoji = restored[1] as EditElement.Emoji
        assertTrue(text.fontSizeRatio in 0f..1f)
        assertTrue(emoji.sizeRatio in 0f..1f)
    }

    @Test
    fun `saved payload contains only bundle-safe types`() {
        val state = PhotoEditorState(scope)
        state.addText("t")
        state.addEmoji("😀")
        state.addShape(EditorShape.Arrow)
        state.startDrawing(Offset(0.1f, 0.1f))
        state.finishDrawing()

        val saved = with(saver) { acceptAll.save(state) }

        fun assertBundleSafe(value: Any?) {
            when (value) {
                null, is String, is Int, is Float, is Long, is Boolean, is FloatArray -> Unit
                is List<*> -> value.forEach(::assertBundleSafe)
                else -> throw AssertionError("Not bundle-safe: ${value::class} = $value")
            }
        }
        assertBundleSafe(saved)
    }
}
