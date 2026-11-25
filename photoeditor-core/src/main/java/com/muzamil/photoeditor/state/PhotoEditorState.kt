package com.muzamil.photoeditor.state

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorHistoryState
import com.muzamil.photoeditor.domain.EditorShape
import com.muzamil.photoeditor.domain.EditorState
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.domain.nextZIndex
import com.muzamil.photoeditor.export.SaveDestination
import com.muzamil.photoeditor.export.saveEditedImage
import com.muzamil.photoeditor.history.HistoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

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

    /**
     * [canUndo] and [canRedo] bundled as one immutable value, for UIs that
     * take history availability as a single parameter (e.g. `EditorTopBar`).
     */
    val historyState: EditorHistoryState by derivedStateOf {
        EditorHistoryState(canUndo = canUndo, canRedo = canRedo)
    }

    /** Location of the last successful save (uri string or file path), until acknowledged. */
    var saveSuccess: String? by mutableStateOf(null)
        private set

    /**
     * On-screen bounding box of the selected element in canvas-container
     * pixels — the axis-aligned bounds of the element after scale and
     * rotation, reported live by the canvas. Null when nothing is selected
     * (or the canvas hasn't measured yet). Anchor contextual UI (selection
     * toolbars, handles drawn outside the canvas) to this rect; it tracks the
     * element through drags and transforms.
     */
    var selectedElementBounds: Rect? by mutableStateOf(null)
        private set

    /**
     * True while a canvas gesture is in flight — a freehand stroke, or an
     * element drag/rotate/scale. Fade your editor chrome out while this is set
     * so nothing covers the image mid-gesture; it flips back to false as soon
     * as the finger lifts. Transient: not retained across process death.
     */
    var isInteracting: Boolean by mutableStateOf(false)
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
        selectedElementBounds = null
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

    internal fun setInteracting(interacting: Boolean) {
        isInteracting = interacting
    }

    // ── Text ──────────────────────────────────────────────────────────────────

    /**
     * Adds a text element and returns its id, so callers can immediately
     * [selectElement] it (e.g. to open a style panel bound to the new text).
     */
    fun addText(text: String, position: Offset = Offset(0.5f, 0.5f)): String {
        val element = EditElement.Text(
            text = text,
            offsetRatioX = position.x,
            offsetRatioY = position.y,
            color = editorState.textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            zIndex = editorState.elements.nextZIndex(),
        )
        addElement(element)
        return element.id
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

    /**
     * Adds an emoji element and returns its id, so callers can immediately
     * [selectElement] it (mirroring [addText], so a freshly placed emoji comes
     * up selected and ready to move/resize).
     */
    fun addEmoji(emoji: String): String {
        val element = EditElement.Emoji(
            emoji = emoji,
            offsetRatioX = 0.5f,
            offsetRatioY = 0.5f,
            size = 48.sp,
            zIndex = editorState.elements.nextZIndex(),
        )
        addElement(element)
        return element.id
    }

    fun updateSelectedEmojiSize(size: TextUnit) {
        val id = editorState.selectedElementId ?: return
        updateElement(id) { el ->
            if (el is EditElement.Emoji) el.copy(size = size) else el
        }
    }

    // ── Shape ─────────────────────────────────────────────────────────────────

    /**
     * Adds [shape] styled by the current [EditorState.shapeTool] and returns
     * its id, so callers can immediately [selectElement] it (mirroring
     * [addText]). Defaults to the tool's selected shape, so `addShape()` places
     * whatever the Shape panel shows.
     */
    fun addShape(shape: EditorShape = editorState.shapeTool.selectedShape): String {
        val s = editorState
        val element = EditElement.Shape(
            kind = shape,
            color = s.shapeTool.selectedColor,
            strokeWidth = s.shapeTool.strokeWidthPx,
            filled = s.shapeTool.isFilled,
            zIndex = s.elements.nextZIndex(),
        )
        addElement(element)
        return element.id
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
                    zIndex = s.elements.nextZIndex(),
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
        if (editorState.selectedElementId != elementId.ifEmpty { null }) {
            // Stale bounds belong to the previous selection; the canvas
            // reports fresh ones on the next frame.
            selectedElementBounds = null
        }
        update { it.copy(selectedElementId = elementId.ifEmpty { null }) }
    }

    fun deselectElement() {
        selectedElementBounds = null
        update { it.copy(selectedElementId = null) }
    }

    /** Called by the canvas whenever the selected element's screen bounds change. */
    internal fun reportSelectedElementBounds(bounds: Rect) {
        if (editorState.selectedElementId != null) selectedElementBounds = bounds
    }

    // ── Duplicate ─────────────────────────────────────────────────────────────

    /**
     * Duplicates [elementId] with all its style and transform, nudged
     * slightly down-right so the copy is visibly distinct, and returns the
     * new element's id (null if the id is unknown). Callers typically select
     * the returned id so contextual UI moves to the copy.
     */
    fun duplicateElement(elementId: String): String? {
        val original = editorState.elements.firstOrNull { it.id == elementId } ?: return null
        val newId = UUID.randomUUID().toString()
        val newX = (original.offsetRatioX + DUPLICATE_NUDGE_RATIO).coerceAtMost(0.95f)
        val newY = (original.offsetRatioY + DUPLICATE_NUDGE_RATIO).coerceAtMost(0.95f)
        val zIndex = editorState.elements.nextZIndex()
        val copy = when (original) {
            is EditElement.Text -> original.copy(
                id = newId, offsetRatioX = newX, offsetRatioY = newY, zIndex = zIndex,
            )

            is EditElement.Emoji -> original.copy(
                id = newId, offsetRatioX = newX, offsetRatioY = newY, zIndex = zIndex,
            )

            is EditElement.Shape -> original.copy(
                id = newId, offsetRatioX = newX, offsetRatioY = newY, zIndex = zIndex,
            )

            is EditElement.Drawing -> original.copy(
                id = newId,
                zIndex = zIndex,
                points = original.points.map {
                    Offset(
                        (it.x + DUPLICATE_NUDGE_RATIO).coerceAtMost(1f),
                        (it.y + DUPLICATE_NUDGE_RATIO).coerceAtMost(1f),
                    )
                },
            )
        }
        addElement(copy)
        return newId
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteElement(elementId: String) {
        if (editorState.selectedElementId == elementId) selectedElementBounds = null
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
        selectedElementBounds = null
        update { it.copy(elements = emptyList(), selectedElementId = null) }
        pushToHistory()
    }

    // ── Undo / Redo ───────────────────────────────────────────────────────────

    fun undo() {
        historyManager.undo()?.let { elements ->
            selectedElementBounds = null
            update { it.copy(elements = elements, selectedElementId = null) }
            updateHistoryFlags()
        }
    }

    fun redo() {
        historyManager.redo()?.let { elements ->
            selectedElementBounds = null
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

    private companion object {
        /** How far a duplicate is offset from its source, in container ratio. */
        const val DUPLICATE_NUDGE_RATIO = 0.03f
    }
}
