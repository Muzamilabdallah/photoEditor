package com.muzamil.photoeditor.history

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.muzamil.photoeditor.domain.EditElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HistoryManagerTest {

    private lateinit var history: HistoryManager

    @Before
    fun setUp() {
        history = HistoryManager()
        history.initialize(emptyList())
    }

    private fun drawing(point: Offset) = EditElement.Drawing(
        points = listOf(point),
        color = Color.Black,
        strokeWidth = 5f,
    )

    @Test
    fun `fresh history can neither undo nor redo`() {
        assertFalse(history.canUndo())
        assertFalse(history.canRedo())
        assertNull(history.undo())
        assertNull(history.redo())
    }

    @Test
    fun `undo returns the state before the last push`() {
        val first = listOf(drawing(Offset.Zero))
        history.pushState(first)
        history.pushState(first + drawing(Offset(0.5f, 0.5f)))

        assertEquals(first, history.undo())
        assertEquals(emptyList<EditElement>(), history.undo())
        assertFalse(history.canUndo())
    }

    @Test
    fun `redo restores what undo removed`() {
        val state = listOf(drawing(Offset.Zero))
        history.pushState(state)
        history.undo()

        assertTrue(history.canRedo())
        assertEquals(state, history.redo())
        assertFalse(history.canRedo())
        assertTrue(history.canUndo())
    }

    @Test
    fun `a new push clears the redo stack`() {
        history.pushState(listOf(drawing(Offset.Zero)))
        history.undo()
        assertTrue(history.canRedo())

        history.pushState(listOf(drawing(Offset(1f, 1f))))
        assertFalse(history.canRedo())
        assertNull(history.redo())
    }

    @Test
    fun `undo depth is bounded at 50 states`() {
        repeat(80) { i ->
            history.pushState(List(i + 1) { drawing(Offset.Zero) })
        }
        var undoCount = 0
        while (history.undo() != null) undoCount++
        assertEquals(50, undoCount)
    }

    @Test
    fun `oldest states are evicted first when the bound is hit`() {
        repeat(60) { i ->
            history.pushState(List(i + 1) { drawing(Offset.Zero) })
        }
        var last: List<EditElement>? = null
        while (true) {
            last = history.undo() ?: break
        }
        // 60 pushes, 50 kept → the deepest reachable state has 10 elements.
        assertEquals(10, requireNotNull(last).size)
    }

    @Test
    fun `clear drops both stacks`() {
        history.pushState(listOf(drawing(Offset.Zero)))
        history.undo()
        history.clear()
        assertFalse(history.canUndo())
        assertFalse(history.canRedo())
    }

    @Test
    fun `initialize resets to the given baseline`() {
        history.pushState(listOf(drawing(Offset.Zero)))
        val baseline = listOf(drawing(Offset(0.3f, 0.3f)))
        history.initialize(baseline)
        assertFalse(history.canUndo())

        history.pushState(baseline + drawing(Offset(0.6f, 0.6f)))
        assertEquals(baseline, history.undo())
    }
}
