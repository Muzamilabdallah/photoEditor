package com.muzamil.photoeditor.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzamil.photoeditor.domain.EditElement
import com.muzamil.photoeditor.domain.EditorState
import com.muzamil.photoeditor.domain.EditorTool
import com.muzamil.photoeditor.domain.ShapeKind
import com.muzamil.photoeditor.export.saveEditedImage
import com.muzamil.photoeditor.history.HistoryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PhotoEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val historyManager = HistoryManager()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _saveSuccess = MutableStateFlow<String?>(null)
    val saveSuccess: StateFlow<String?> = _saveSuccess.asStateFlow()

    // Container size in device pixels — updated from onSizeChanged in the UI.
    // Replaces the old previewScaleFactor: the exporter derives the correct
    // crop scale itself from containerSize + bitmap dimensions.
    private val _containerSize = MutableStateFlow(IntSize.Zero)
    val containerSize: StateFlow<IntSize> = _containerSize.asStateFlow()

    init {
        historyManager.initialize(emptyList())
    }

    // ── Tool Selection ────────────────────────────────────────────────────────

    fun selectTool(tool: EditorTool) {
        _state.update { it.copy(currentTool = tool, selectedElementId = null) }
    }

    // ── Image ─────────────────────────────────────────────────────────────────

    fun loadImage(bitmap: Bitmap) {
        _state.update { it.copy(backgroundBitmap = bitmap) }
    }

    /**
     * Called from the UI whenever the preview container is measured.
     * Pass the value from `onSizeChanged { containerSize = it }` directly.
     */
    fun setContainerSize(size: IntSize) {
        if (size.width <= 0 || size.height <= 0) return
        if (_containerSize.value == size) return
        _containerSize.value = size
    }

    // ── Text ──────────────────────────────────────────────────────────────────

    fun addText(text: String, position: Offset = Offset(0.5f, 0.5f)) {
        addElement(
            EditElement.Text(
                text = text,
                offsetRatioX = position.x,
                offsetRatioY = position.y,
                color = _state.value.textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                zIndex = _state.value.elements.size,
            )
        )
    }

    fun updateTextContent(elementId: String, newText: String) {
        updateElement(elementId) { el ->
            if (el is EditElement.Text) el.copy(text = newText) else el
        }
    }

    fun updateTextStyle(color: Color? = null, size: TextUnit? = null) {
        _state.update {
            it.copy(
                textColor = color ?: it.textColor,
                textSize = size ?: it.textSize,
            )
        }
    }

    fun updateSelectedTextStyle(color: Color? = null, fontSize: TextUnit? = null) {
        val id = _state.value.selectedElementId ?: return
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
                zIndex = _state.value.elements.size,
            )
        )
    }

    fun updateSelectedEmojiSize(size: TextUnit) {
        val id = _state.value.selectedElementId ?: return
        updateElement(id) { el ->
            if (el is EditElement.Emoji) el.copy(size = size) else el
        }
    }

    // ── Shape ─────────────────────────────────────────────────────────────────

    fun addShape(kind: ShapeKind) {
        val s = _state.value
        addElement(
            EditElement.Shape(
                kind = kind,
                color = s.shapeColor,
                strokeWidth = s.shapeStrokeWidth,
                filled = s.shapeFilled,
                zIndex = s.elements.size,
            )
        )
    }

    fun updateShapeStyle(
        color: Color? = null,
        strokeWidth: Float? = null,
        filled: Boolean? = null,
    ) {
        _state.update {
            it.copy(
                shapeColor = color ?: it.shapeColor,
                shapeStrokeWidth = strokeWidth ?: it.shapeStrokeWidth,
                shapeFilled = filled ?: it.shapeFilled,
            )
        }
    }

    fun updateSelectedShapeStyle(
        color: Color? = null,
        strokeWidth: Float? = null,
        filled: Boolean? = null,
    ) {
        val id = _state.value.selectedElementId ?: return
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
        val s = _state.value
        _state.update {
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

    fun continueDrawing(point: Offset) {
        _state.update { s ->
            val last = s.elements.lastOrNull() as? EditElement.Drawing ?: return@update s
            s.copy(elements = s.elements.dropLast(1) + last.copy(points = last.points + point))
        }
    }

    fun finishDrawing() {
        pushToHistory()
    }

    fun updateDrawingStyle(color: Color? = null, strokeWidth: Float? = null) {
        _state.update {
            it.copy(
                drawingColor = color ?: it.drawingColor,
                drawingStrokeWidth = strokeWidth ?: it.drawingStrokeWidth,
            )
        }
    }

    fun updateSelectedDrawingStyle(color: Color? = null, strokeWidth: Float? = null) {
        val id = _state.value.selectedElementId ?: return
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
        _state.update {
            it.copy(selectedElementId = elementId.ifEmpty { null })
        }
    }

    fun deselectElement() {
        _state.update { it.copy(selectedElementId = null) }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteElement(elementId: String) {
        _state.update {
            it.copy(
                elements = it.elements.filter { el -> el.id != elementId },
                selectedElementId = if (it.selectedElementId == elementId) null
                else it.selectedElementId,
            )
        }
        pushToHistory()
    }

    fun deleteSelectedElement() {
        _state.value.selectedElementId?.let { deleteElement(it) }
    }

    // ── Clear All ─────────────────────────────────────────────────────────────

    fun clearAll() {
        _state.update { it.copy(elements = emptyList(), selectedElementId = null) }
        pushToHistory()
    }

    // ── Undo / Redo ───────────────────────────────────────────────────────────

    fun undo() {
        historyManager.undo()?.let { elements ->
            _state.update { it.copy(elements = elements, selectedElementId = null) }
            updateHistoryFlags()
        }
    }

    fun redo() {
        historyManager.redo()?.let { elements ->
            _state.update { it.copy(elements = elements, selectedElementId = null) }
            updateHistoryFlags()
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveEditedImage(
        context: Context,
        quality: Int = 95,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    ) {
        val bitmap = _state.value.backgroundBitmap
        val containerSize = _containerSize.value

        if (bitmap == null) {
            _state.update { it.copy(error = "No image to save") }
            return
        }
        if (containerSize == IntSize.Zero) {
            _state.update { it.copy(error = "Preview not ready — try again") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            context.saveEditedImage(
                backgroundBitmap = bitmap,
                elements = _state.value.elements,
                containerSize = containerSize,   // ← replaces previewScaleFactor
                quality = quality,
                format = format,
            ).fold(
                onSuccess = { path ->
                    _state.update { it.copy(isLoading = false) }
                    _saveSuccess.value = path
                },
                onFailure = { err ->
                    Log.e("PhotoEditorVM", "saveEditedImage failed", err)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to save image",
                        )
                    }
                },
            )
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        historyManager.clear()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun addElement(element: EditElement) {
        _state.update { it.copy(elements = it.elements + element) }
        pushToHistory()
    }

    private fun updateElement(id: String, transform: (EditElement) -> EditElement) {
        _state.update { s ->
            s.copy(elements = s.elements.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun pushToHistory() {
        historyManager.pushState(_state.value.elements)
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        _canUndo.value = historyManager.canUndo()
        _canRedo.value = historyManager.canRedo()
    }
}