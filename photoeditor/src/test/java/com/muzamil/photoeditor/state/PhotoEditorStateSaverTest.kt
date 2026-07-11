package com.muzamil.photoeditor.state

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.domain.ShapeKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        state.updateShapeStyle(color = Color.Blue, strokeWidth = 14f, filled = true)
        state.addShape(ShapeKind.Star)
        state.startDrawing(Offset(0.1f, 0.1f))
        state.continueDrawing(listOf(Offset(0.2f, 0.25f), Offset(0.3f, 0.35f)))
        state.finishDrawing()

        val restored = roundTrip(state)

        assertEquals(state.editorState.elements, restored.editorState.elements)
    }

    @Test
    fun `tool selection and style defaults survive the round trip`() {
        val state = PhotoEditorState(scope)
        state.selectTool(EditorTool.Shape(ShapeKind.Heart))
        state.updateDrawingStyle(color = Color.Red, strokeWidth = 9f)
        state.updateTextStyle(color = Color.Yellow, size = 32.sp)
        state.updateShapeStyle(color = Color.Cyan, strokeWidth = 3f, filled = true)

        val restored = roundTrip(state).editorState

        assertEquals(EditorTool.Shape(ShapeKind.Heart), restored.currentTool)
        assertEquals(Color.Red, restored.drawingColor)
        assertEquals(9f, restored.drawingStrokeWidth, 0f)
        assertEquals(Color.Yellow, restored.textColor)
        assertEquals(32.sp, restored.textSize)
        assertEquals(Color.Cyan, restored.shapeColor)
        assertEquals(3f, restored.shapeStrokeWidth, 0f)
        assertEquals(true, restored.shapeFilled)
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
        state.updateSelectedTextStyle(color = Color.Magenta, fontSize = 44.sp)
        state.updateElementTransform(id, 0.8f, 0.2f, rotation = 30f, scale = 2.5f)

        val text = roundTrip(state).editorState.elements.single() as EditElement.Text
        assertEquals(Color.Magenta, text.color)
        assertEquals(44.sp, text.fontSize)
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
    fun `saved payload contains only bundle-safe types`() {
        val state = PhotoEditorState(scope)
        state.addText("t")
        state.addEmoji("😀")
        state.addShape(ShapeKind.Arrow)
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
