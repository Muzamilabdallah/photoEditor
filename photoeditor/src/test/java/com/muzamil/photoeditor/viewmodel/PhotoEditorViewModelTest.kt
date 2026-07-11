package com.muzamil.photoeditor.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.domain.ShapeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotoEditorViewModelTest {

    private lateinit var viewModel: PhotoEditorViewModel

    private val state get() = viewModel.state.value
    private val elements get() = state.elements

    @Before
    fun setUp() {
        viewModel = PhotoEditorViewModel()
    }

    // ── Elements ──────────────────────────────────────────────────────────────

    @Test
    fun `addText adds a text element with the default style`() {
        viewModel.addText("hello", Offset(0.25f, 0.75f))

        val text = elements.single() as EditElement.Text
        assertEquals("hello", text.text)
        assertEquals(0.25f, text.offsetRatioX, 0f)
        assertEquals(0.75f, text.offsetRatioY, 0f)
        assertEquals(state.textColor, text.color)
        assertTrue(viewModel.canUndo.value)
    }

    @Test
    fun `addEmoji and addShape append with increasing zIndex`() {
        viewModel.addEmoji("😀")
        viewModel.addShape(ShapeKind.Circle)
        viewModel.addText("t")

        assertEquals(listOf(0, 1, 2), elements.map { it.zIndex })
        assertTrue(elements[1] is EditElement.Shape)
    }

    @Test
    fun `deleteElement removes the element and its selection`() {
        viewModel.addEmoji("😀")
        val id = elements.single().id
        viewModel.selectElement(id)

        viewModel.deleteElement(id)

        assertTrue(elements.isEmpty())
        assertNull(state.selectedElementId)
    }

    @Test
    fun `clearAll removes everything and is undoable`() {
        viewModel.addText("a")
        viewModel.addEmoji("😀")
        viewModel.clearAll()
        assertTrue(elements.isEmpty())

        viewModel.undo()
        assertEquals(2, elements.size)
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    @Test
    fun `startDrawing creates a drawing using the current defaults`() {
        viewModel.updateDrawingStyle(color = Color.Red, strokeWidth = 12f)
        viewModel.startDrawing(Offset(0.1f, 0.2f))

        val drawing = elements.single() as EditElement.Drawing
        assertEquals(listOf(Offset(0.1f, 0.2f)), drawing.points)
        assertEquals(Color.Red, drawing.color)
        assertEquals(12f, drawing.strokeWidth, 0f)
    }

    @Test
    fun `continueDrawing appends a whole batch of points`() {
        viewModel.startDrawing(Offset(0.1f, 0.1f))
        viewModel.continueDrawing(listOf(Offset(0.2f, 0.2f), Offset(0.3f, 0.3f)))
        viewModel.continueDrawing(listOf(Offset(0.4f, 0.4f)))

        val drawing = elements.single() as EditElement.Drawing
        assertEquals(4, drawing.points.size)
        assertEquals(Offset(0.4f, 0.4f), drawing.points.last())
    }

    @Test
    fun `continueDrawing with an empty batch changes nothing`() {
        viewModel.startDrawing(Offset(0.1f, 0.1f))
        val before = state
        viewModel.continueDrawing(emptyList())
        assertEquals(before, state)
    }

    @Test
    fun `continueDrawing without an in-progress drawing is a no-op`() {
        viewModel.addText("not a drawing")
        viewModel.continueDrawing(listOf(Offset(0.5f, 0.5f)))

        assertTrue(elements.single() is EditElement.Text)
    }

    @Test
    fun `a stroke is one undo step - undo removes the whole drawing`() {
        viewModel.startDrawing(Offset(0.1f, 0.1f))
        viewModel.continueDrawing(listOf(Offset(0.2f, 0.2f), Offset(0.3f, 0.3f)))
        viewModel.finishDrawing()

        viewModel.undo()
        assertTrue(elements.isEmpty())

        viewModel.redo()
        val drawing = elements.single() as EditElement.Drawing
        assertEquals(3, drawing.points.size)
    }

    // ── Transform ─────────────────────────────────────────────────────────────

    @Test
    fun `updateElementTransform clamps text scale to half through 3x`() {
        viewModel.addText("t")
        val id = elements.single().id

        viewModel.updateElementTransform(id, 0.5f, 0.5f, 0f, scale = 99f)
        assertEquals(3f, elements.single().scale, 0f)

        viewModel.updateElementTransform(id, 0.5f, 0.5f, 0f, scale = 0.01f)
        assertEquals(0.5f, elements.single().scale, 0f)
    }

    @Test
    fun `updateElementTransform moves and rotates the element`() {
        viewModel.addEmoji("😀")
        val id = elements.single().id

        viewModel.updateElementTransform(id, 0.9f, 0.1f, rotation = 45f, scale = 2f)

        val emoji = elements.single()
        assertEquals(0.9f, emoji.offsetRatioX, 0f)
        assertEquals(0.1f, emoji.offsetRatioY, 0f)
        assertEquals(45f, emoji.rotation, 0f)
        assertEquals(2f, emoji.scale, 0f)
    }

    @Test
    fun `transform updates do not create history entries`() {
        viewModel.addText("t")
        val id = elements.single().id
        viewModel.updateElementTransform(id, 0.9f, 0.9f, 0f, 1f)

        viewModel.undo()
        // Undo reverts the *add*, not the transform.
        assertTrue(elements.isEmpty())
    }

    // ── Selection & tools ─────────────────────────────────────────────────────

    @Test
    fun `selectElement with an empty id clears the selection`() {
        viewModel.addText("t")
        viewModel.selectElement(elements.single().id)
        assertEquals(elements.single().id, state.selectedElementId)

        viewModel.selectElement("")
        assertNull(state.selectedElementId)
    }

    @Test
    fun `selecting a tool deselects any element`() {
        viewModel.addText("t")
        viewModel.selectElement(elements.single().id)

        viewModel.selectTool(EditorTool.Drawing)

        assertEquals(EditorTool.Drawing, state.currentTool)
        assertNull(state.selectedElementId)
    }

    // ── Undo / redo flags ─────────────────────────────────────────────────────

    @Test
    fun `undo and redo flags follow the history state`() {
        assertFalse(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        viewModel.addText("t")
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        viewModel.undo()
        assertFalse(viewModel.canUndo.value)
        assertTrue(viewModel.canRedo.value)

        viewModel.redo()
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    @Test
    fun `updateSelectedTextStyle only touches the selected element`() {
        viewModel.addText("a")
        viewModel.addText("b")
        val first = elements[0].id
        viewModel.selectElement(first)

        viewModel.updateSelectedTextStyle(color = Color.Green, fontSize = 40.sp)

        val styled = elements[0] as EditElement.Text
        val untouched = elements[1] as EditElement.Text
        assertEquals(Color.Green, styled.color)
        assertEquals(40.sp, styled.fontSize)
        assertEquals(state.textColor, untouched.color)
    }

    @Test
    fun `updateShapeStyle changes defaults for the next shape`() {
        viewModel.updateShapeStyle(color = Color.Blue, strokeWidth = 20f, filled = true)
        viewModel.addShape(ShapeKind.Star)

        val shape = elements.single() as EditElement.Shape
        assertEquals(Color.Blue, shape.color)
        assertEquals(20f, shape.strokeWidth, 0f)
        assertTrue(shape.filled)
    }

    // ── Container size ────────────────────────────────────────────────────────

    @Test
    fun `setContainerSize ignores degenerate sizes`() {
        viewModel.setContainerSize(IntSize(0, 100))
        viewModel.setContainerSize(IntSize(100, 0))
        assertEquals(IntSize.Zero, viewModel.containerSize.value)

        viewModel.setContainerSize(IntSize(1080, 1920))
        assertEquals(IntSize(1080, 1920), viewModel.containerSize.value)
    }
}
