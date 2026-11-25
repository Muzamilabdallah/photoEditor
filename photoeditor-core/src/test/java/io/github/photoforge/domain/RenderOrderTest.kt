package io.github.photoforge.domain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class RenderOrderTest {

    private fun text(id: String, z: Int) = EditElement.Text(id = id, text = id, zIndex = z)
    private fun stroke(id: String, z: Int) = EditElement.Drawing(
        id = id, points = listOf(Offset(0.5f, 0.5f)), color = Color.Red, strokeWidthRatio = 0.004f, zIndex = z,
    )

    @Test
    fun `a stroke drawn after a text paints above it`() {
        val order = listOf(stroke("s", 1), text("t", 0)).inRenderOrder()
        assertEquals(listOf("t", "s"), order.map { it.id })
    }

    @Test
    fun `drawings interleave with overlays by zIndex`() {
        val elements = listOf(stroke("s0", 0), text("t1", 1), stroke("s2", 2), text("t3", 3))
        assertEquals(listOf("s0", "t1", "s2", "t3"), elements.reversed().inRenderOrder().map { it.id })
    }

    @Test
    fun `equal zIndex keeps list order`() {
        val order = listOf(text("a", 0), stroke("b", 0), text("c", 0)).inRenderOrder()
        assertEquals(listOf("a", "b", "c"), order.map { it.id })
    }

    @Test
    fun `nextZIndex is one above the top, not the list size`() {
        assertEquals(0, emptyList<EditElement>().nextZIndex())
        // After deleting z=1 from [0, 1, 2], size is 2 but 2 is taken.
        assertEquals(3, listOf(text("a", 0), text("c", 2)).nextZIndex())
    }
}
