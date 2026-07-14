package com.muzamil.photoeditor.state

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorShape
import com.muzamil.photoeditor.domain.EditorState
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.export.saveEditedImage
import com.muzamil.photoeditor.history.HistoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Creates and remembers the editor's state holder, the Compose-idiomatic
 * entry point for hoisting editor state:
 * ```
 * val editorState = rememberPhotoEditorState()
 * PhotoEditor(state = editorState)
 * Button(onClick = { editorState.undo() }, enabled = editorState.canUndo) { ... }
 * ```
 *
 * The state is retained with [rememberSaveable] and a custom [Saver]:
 * elements, tool selection, and style defaults survive configuration changes
 * *and* process death. The background bitmap itself is deliberately not
 * saved (bitmaps are far too large for saved instance state) — the
 * `PhotoEditor` composable persists the picked image *uri* and re-decodes it
 * on restore. Undo/redo history is also not retained across recreation.
 */
@Composable
fun rememberPhotoEditorState(): PhotoEditorState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(saver = photoEditorStateSaver(scope)) {
        PhotoEditorState(scope)
    }
}

/**
 * Plain state holder for the photo editor. All UI state is exposed as
 * Compose snapshot state; every mutation goes through a method on this class.
 *
 * Create it with [rememberPhotoEditorState].
 */
@Stable
class PhotoEditorState internal constructor(
    private val scope: CoroutineScope,
    initial: EditorState = EditorState(),
) {

    /** The complete editor state driving the UI. Read-only from outside. */
    var editorState: EditorState by mutableStateOf(initial)
        private set

    var canUndo: Boolean by mutableStateOf(false)
        private set

    var canRedo: Boolean by mutableStateOf(false)
        private set

    /** Location of the last successful save (uri string or file path), until acknowledged. */
    var saveSuccess: String? by mutableStateOf(null)
        private set

    // Preview container size in device pixels — the exporter derives the crop
    // scale from it. Updated from onSizeChanged in the UI.
    internal var containerSize: IntSize by mutableStateOf(IntSize.Zero)
        private set

    private val historyManager = HistoryManager().apply { initialize(initial.elements) }

    private inline fun update(transform: (EditorState) -> EditorState) {
        editorState = transform(editorState)
    }

    // ── Tool Selection ────────────────────────────────────────────────────────

    fun selectTool(tool: EditorTool) {
        update { it.copy(currentTool = tool, selectedElementId = null) }
    }

    // ── Image ─────────────────────────────────────────────────────────────────

    fun loadImage(bitmap: Bitmap) {
        update { it.copy(backgroundBitmap = bitmap) }
    }

    internal fun setContainerSize(size: IntSize) {
        if (size.width <= 0 || size.height <= 0) return
        containerSize = size
    }

    // ── Text ──────────────────────────────────────────────────────────────────

    fun addText(text: String, position: Offset = Offset(0.5f, 0.5f)) {
        addElement(
            EditElement.Text(
                text = text,
                offsetRatioX = position.x,
                offsetRatioY = position.y,
                color = editorState.textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                zIndex = editorState.elements.size,
            )
        )
    }

    fun updateTextContent(elementId: String, newText: String) {
        updateElement(elementId) { el ->
            if (el is EditElement.Text) el.copy(text = newText) else el
        }
    }

    fun updateTextStyle(color: Color? = null, size: TextUnit? = null) {
        update {
            it.copy(
                textColor = color ?: it.textColor,
                textSize = size ?: it.textSize,
            )
        }
    }

    fun updateSelectedTextStyle(color: Color? = null, fontSize: TextUnit? = null) {
        val id = editorState.selectedElementId ?: return
        updateElement(id) { el ->
            if (el is EditElement.Text) el.copy(
                color = color ?: el.color,
                fontSize = fontSize ?: el.fontSize,
            ) else el
        }
    }

    // ── Emoji ─────────────────────────────────────────────────────────────────

    fun addEmoji(emoji: String) {
        addElement(
            EditElement.Emoji(
                emoji = emoji,
                offsetRatioX = 0.5f,
                offsetRatioY = 0.5f,
                size = 48.sp,
                zIndex = editorState.elements.size,
            )
        )
    }

    fun updateSelectedEmojiSize(size: TextUnit) {
        val id = editorState.selectedElementId ?: return
        updateElement(id) { el ->
            if (el is EditElement.Emoji) el.copy(size = size) else el
        }
    }

    // ── Shape ─────────────────────────────────────────────────────────────────

    /**
     * Adds [shape] styled by the current [EditorState.shapeTool]. Defaults to
     * the tool's selected shape, so `addShape()` places whatever the Shape
     * panel shows.
     */
    fun addShape(shape: EditorShape = editorState.shapeTool.selectedShape) {
        val s = editorState
        addElement(
            EditElement.Shape(
                kind = shape,
                color = s.shapeTool.selectedColor,
                strokeWidth = s.shapeTool.strokeWidthPx,
                filled = s.shapeTool.isFilled,
                zIndex = s.elements.size,
            )
        )
    }

    /** Updates the Shape tool's pending style; nulls leave a field unchanged. */
    fun updateShapeTool(
        shape: EditorShape? = null,
        color: Color? = null,
        strokeWidth: Float? = null,
        filled: Boolean? = null,
    ) {
        update {
            it.copy(
                shapeTool = it.shapeTool.copy(
                    selectedShape = shape ?: it.shapeTool.selectedShape,
                    selectedColor = color ?: it.shapeTool.selectedColor,
                    strokeWidthPx = strokeWidth ?: it.shapeTool.strokeWidthPx,
                    isFilled = filled ?: it.shapeTool.isFilled,
                )
            )
        }
    }

    fun updateSelectedShapeStyle(
        color: Color? = null,
        strokeWidth: Float? = null,
        filled: Boolean? = null,
    ) {
        val id = editorState.selectedElementId ?: return
        updateElement(id) { el ->
            if (el is EditElement.Shape) el.copy(
                color = color ?: el.color,
                strokeWidth = strokeWidth ?: el.strokeWidth,
                filled = filled ?: el.filled,
            ) else el
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    fun startDrawing(startPoint: Offset) {
        val s = editorState
        update {
            it.copy(
                elements = it.elements + EditElement.Drawing(
                    points = listOf(startPoint),
                    color = s.drawingColor,
                    strokeWidth = s.drawingStrokeWidth,
                    zIndex = s.elements.size,
                )
            )
        }
    }

    /**
     * Appends a batch of points to the in-progress drawing. Batched (one call
     * per drag event, including historical samples) so each gesture event
     * costs a single state update regardless of how many points it carries.
     */
    fun continueDrawing(points: List<Offset>) {
        if (points.isEmpty()) return
        update { s ->
            val last = s.elements.lastOrNull() as? EditElement.Drawing ?: return@update s
            s.copy(elements = s.elements.dropLast(1) + last.copy(points = last.points + points))
        }
    }

    fun finishDrawing() {
        pushToHistory()
    }

    fun updateDrawingStyle(color: Color? = null, strokeWidth: Float? = null) {
        update {
            it.copy(
                drawingColor = color ?: it.drawingColor,
                drawingStrokeWidth = strokeWidth ?: it.drawingStrokeWidth,
            )
        }
    }

    fun updateSelectedDrawingStyle(color: Color? = null, strokeWidth: Float? = null) {
        val id = editorState.selectedElementId ?: return
        updateElement(id) { el ->
            if (el is EditElement.Drawing) el.copy(
                color = color ?: el.color,
                strokeWidth = strokeWidth ?: el.strokeWidth,
            ) else el
        }
    }

    // ── Transform (drag / pinch / rotate) ────────────────────────────────────

    fun updateElementTransform(
        id: String,
        offsetRatioX: Float,
        offsetRatioY: Float,
        rotation: Float,
        scale: Float,
    ) {
        updateElement(id) { el ->
            when (el) {
                is EditElement.Text -> el.copy(
                    offsetRatioX = offsetRatioX,
                    offsetRatioY = offsetRatioY,
                    rotation = rotation,
                    scale = scale.coerceIn(0.5f, 3f),
                )

                is EditElement.Emoji -> el.copy(
                    offsetRatioX = offsetRatioX,
                    offsetRatioY = offsetRatioY,
                    rotation = rotation,
                    scale = scale.coerceIn(0.5f, 3f),
                )

                is EditElement.Shape -> el.copy(
                    offsetRatioX = offsetRatioX,
                    offsetRatioY = offsetRatioY,
                    rotation = rotation,
                    scale = scale.coerceIn(0.5f, 5f),
                )

                is EditElement.Drawing -> el // drawings don't transform via gesture
            }
        }
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    fun selectElement(elementId: String) {
        update { it.copy(selectedElementId = elementId.ifEmpty { null }) }
    }

    fun deselectElement() {
        update { it.copy(selectedElementId = null) }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteElement(elementId: String) {
        update {
            it.copy(
                elements = it.elements.filter { el -> el.id != elementId },
                selectedElementId = if (it.selectedElementId == elementId) null
                else it.selectedElementId,
            )
        }
        pushToHistory()
    }

    fun deleteSelectedElement() {
        editorState.selectedElementId?.let { deleteElement(it) }
    }

    // ── Clear All ─────────────────────────────────────────────────────────────

    fun clearAll() {
        update { it.copy(elements = emptyList(), selectedElementId = null) }
        pushToHistory()
    }

    // ── Undo / Redo ───────────────────────────────────────────────────────────

    fun undo() {
        historyManager.undo()?.let { elements ->
            update { it.copy(elements = elements, selectedElementId = null) }
            updateHistoryFlags()
        }
    }

    fun redo() {
        historyManager.redo()?.let { elements ->
            update { it.copy(elements = elements, selectedElementId = null) }
            updateHistoryFlags()
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveEditedImage(
        context: Context,
        quality: Int = 95,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        destination: SaveDestination = SaveDestination.Gallery(),
    ) {
        val bitmap = editorState.backgroundBitmap
        val containerSize = containerSize

        if (bitmap == null) {
            update { it.copy(error = "No image to save") }
            return
        }
        if (containerSize == IntSize.Zero) {
            update { it.copy(error = "Preview not ready — try again") }
            return
        }

        scope.launch {
            update { it.copy(isLoading = true, error = null) }

            context.saveEditedImage(
                backgroundBitmap = bitmap,
                elements = editorState.elements,
                containerSize = containerSize,
                quality = quality,
                format = format,
                destination = destination,
            ).fold(
                onSuccess = { path ->
                    update { it.copy(isLoading = false) }
                    saveSuccess = path
                },
                onFailure = { err ->
                    Log.e("PhotoEditorState", "saveEditedImage failed", err)
                    update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to save image",
                        )
                    }
                },
            )
        }
    }

    fun clearSaveSuccess() {
        saveSuccess = null
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun addElement(element: EditElement) {
        update { it.copy(elements = it.elements + element) }
        pushToHistory()
    }

    private fun updateElement(id: String, transform: (EditElement) -> EditElement) {
        update { s ->
            s.copy(elements = s.elements.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun pushToHistory() {
        historyManager.pushState(editorState.elements)
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        canUndo = historyManager.canUndo()
        canRedo = historyManager.canRedo()
    }
}
