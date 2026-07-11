package com.muzamil.photoeditor.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.domain.ShapeKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotoEditorStateTest {

    private lateinit var state: PhotoEditorState

    private val editorState get() = state.editorState
    private val elements get() = editorState.elements

    @Before
    fun setUp() {
        state = PhotoEditorState(CoroutineScope(Dispatchers.Unconfined))
    }

    // ── Elements ──────────────────────────────────────────────────────────────

    @Test
    fun `addText adds a text element with the default style`() {
        state.addText("hello", Offset(0.25f, 0.75f))

        val text = elements.single() as EditElement.Text
        assertEquals("hello", text.text)
        assertEquals(0.25f, text.offsetRatioX, 0f)
        assertEquals(0.75f, text.offsetRatioY, 0f)
        assertEquals(editorState.textColor, text.color)
        assertTrue(state.canUndo)
    }

    @Test
    fun `addEmoji and addShape append with increasing zIndex`() {
        state.addEmoji("😀")
        state.addShape(ShapeKind.Circle)
        state.addText("t")

        assertEquals(listOf(0, 1, 2), elements.map { it.zIndex })
        assertTrue(elements[1] is EditElement.Shape)
    }

    @Test
    fun `deleteElement removes the element and its selection`() {
        state.addEmoji("😀")
        val id = elements.single().id
        state.selectElement(id)

        state.deleteElement(id)

        assertTrue(elements.isEmpty())
        assertNull(editorState.selectedElementId)
    }

    @Test
    fun `clearAll removes everything and is undoable`() {
        state.addText("a")
        state.addEmoji("😀")
        state.clearAll()
        assertTrue(elements.isEmpty())

        state.undo()
        assertEquals(2, elements.size)
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    @Test
    fun `startDrawing creates a drawing using the current defaults`() {
        state.updateDrawingStyle(color = Color.Red, strokeWidth = 12f)
        state.startDrawing(Offset(0.1f, 0.2f))

        val drawing = elements.single() as EditElement.Drawing
        assertEquals(listOf(Offset(0.1f, 0.2f)), drawing.points)
        assertEquals(Color.Red, drawing.color)
        assertEquals(12f, drawing.strokeWidth, 0f)
    }

    @Test
    fun `continueDrawing appends a whole batch of points`() {
        state.startDrawing(Offset(0.1f, 0.1f))
        state.continueDrawing(listOf(Offset(0.2f, 0.2f), Offset(0.3f, 0.3f)))
        state.continueDrawing(listOf(Offset(0.4f, 0.4f)))

        val drawing = elements.single() as EditElement.Drawing
        assertEquals(4, drawing.points.size)
        assertEquals(Offset(0.4f, 0.4f), drawing.points.last())
    }

    @Test
    fun `continueDrawing with an empty batch changes nothing`() {
        state.startDrawing(Offset(0.1f, 0.1f))
        val before = editorState
        state.continueDrawing(emptyList())
        assertEquals(before, editorState)
    }

    @Test
    fun `continueDrawing without an in-progress drawing is a no-op`() {
        state.addText("not a drawing")
        state.continueDrawing(listOf(Offset(0.5f, 0.5f)))

        assertTrue(elements.single() is EditElement.Text)
    }

    @Test
    fun `a stroke is one undo step - undo removes the whole drawing`() {
        state.startDrawing(Offset(0.1f, 0.1f))
        state.continueDrawing(listOf(Offset(0.2f, 0.2f), Offset(0.3f, 0.3f)))
        state.finishDrawing()

        state.undo()
        assertTrue(elements.isEmpty())

        state.redo()
        val drawing = elements.single() as EditElement.Drawing
        assertEquals(3, drawing.points.size)
    }

    // ── Transform ─────────────────────────────────────────────────────────────

    @Test
    fun `updateElementTransform clamps text scale to half through 3x`() {
        state.addText("t")
        val id = elements.single().id

        state.updateElementTransform(id, 0.5f, 0.5f, 0f, scale = 99f)
        assertEquals(3f, elements.single().scale, 0f)

        state.updateElementTransform(id, 0.5f, 0.5f, 0f, scale = 0.01f)
        assertEquals(0.5f, elements.single().scale, 0f)
    }

    @Test
    fun `updateElementTransform moves and rotates the element`() {
        state.addEmoji("😀")
        val id = elements.single().id

        state.updateElementTransform(id, 0.9f, 0.1f, rotation = 45f, scale = 2f)

        val emoji = elements.single()
        assertEquals(0.9f, emoji.offsetRatioX, 0f)
        assertEquals(0.1f, emoji.offsetRatioY, 0f)
        assertEquals(45f, emoji.rotation, 0f)
        assertEquals(2f, emoji.scale, 0f)
    }

    @Test
    fun `transform updates do not create history entries`() {
        state.addText("t")
        val id = elements.single().id
        state.updateElementTransform(id, 0.9f, 0.9f, 0f, 1f)

        state.undo()
        // Undo reverts the *add*, not the transform.
        assertTrue(elements.isEmpty())
    }

    // ── Selection & tools ─────────────────────────────────────────────────────

    @Test
    fun `selectElement with an empty id clears the selection`() {
        state.addText("t")
        state.selectElement(elements.single().id)
        assertEquals(elements.single().id, editorState.selectedElementId)

        state.selectElement("")
        assertNull(editorState.selectedElementId)
    }

    @Test
    fun `selecting a tool deselects any element`() {
        state.addText("t")
        state.selectElement(elements.single().id)

        state.selectTool(EditorTool.Drawing)

        assertEquals(EditorTool.Drawing, editorState.currentTool)
        assertNull(editorState.selectedElementId)
    }

    // ── Undo / redo flags ─────────────────────────────────────────────────────

    @Test
    fun `undo and redo flags follow the history state`() {
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)

        state.addText("t")
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)

        state.undo()
        assertFalse(state.canUndo)
        assertTrue(state.canRedo)

        state.redo()
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    @Test
    fun `updateSelectedTextStyle only touches the selected element`() {
        state.addText("a")
        state.addText("b")
        val first = elements[0].id
        state.selectElement(first)

        state.updateSelectedTextStyle(color = Color.Green, fontSize = 40.sp)

        val styled = elements[0] as EditElement.Text
        val untouched = elements[1] as EditElement.Text
        assertEquals(Color.Green, styled.color)
        assertEquals(40.sp, styled.fontSize)
        assertEquals(editorState.textColor, untouched.color)
    }

    @Test
    fun `updateShapeStyle changes defaults for the next shape`() {
        state.updateShapeStyle(color = Color.Blue, strokeWidth = 20f, filled = true)
        state.addShape(ShapeKind.Star)

        val shape = elements.single() as EditElement.Shape
        assertEquals(Color.Blue, shape.color)
        assertEquals(20f, shape.strokeWidth, 0f)
        assertTrue(shape.filled)
    }

    // ── Container size ────────────────────────────────────────────────────────

    @Test
    fun `setContainerSize ignores degenerate sizes`() {
        state.setContainerSize(IntSize(0, 100))
        state.setContainerSize(IntSize(100, 0))
        assertEquals(IntSize.Zero, state.containerSize)

        state.setContainerSize(IntSize(1080, 1920))
        assertEquals(IntSize(1080, 1920), state.containerSize)
    }
}
