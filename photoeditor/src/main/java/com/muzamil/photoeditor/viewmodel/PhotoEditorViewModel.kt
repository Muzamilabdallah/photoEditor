package com.muzamil.photoeditor.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    init {
        historyManager.initialize(emptyList())
    }

    // ========================================
    // Tool Selection
    // ========================================

    fun selectTool(tool: EditorTool) {
        _state.update { it.copy(currentTool = tool, selectedElementId = null) }
    }

    // ========================================
    // Text Operations
    // ========================================

    fun addText(text: String, position: Offset = Offset(0.5f, 0.5f)) {
        val newElement = EditElement.Text(
            text = text,
            offsetRatioX = position.x,
            offsetRatioY = position.y,
            color = _state.value.textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            zIndex = _state.value.elements.size
        )

        addElement(newElement)
    }

    // ========================================
    // Emoji Operations
    // ========================================

    fun addEmoji(emoji: String) {
        val newElement = EditElement.Emoji(
            emoji = emoji,
            offsetRatioX = 0.5f,
            offsetRatioY = 0.5f,
            size = 48.sp,
            zIndex = _state.value.elements.size
        )

        addElement(newElement)
    }

    // ========================================
    // Shape Operations
    // ========================================

    fun addShape(kind: ShapeKind) {
        val s = _state.value
        val newElement = EditElement.Shape(
            kind = kind,
            color = s.shapeColor,
            strokeWidth = s.shapeStrokeWidth,
            filled = s.shapeFilled,
            zIndex = s.elements.size
        )
        addElement(newElement)
    }

    fun updateShapeStyle(
        color: Color? = null,
        strokeWidth: Float? = null,
        filled: Boolean? = null
    ) {
        _state.update {
            it.copy(
                shapeColor = color ?: it.shapeColor,
                shapeStrokeWidth = strokeWidth ?: it.shapeStrokeWidth,
                shapeFilled = filled ?: it.shapeFilled
            )
        }
    }

    // ========================================
    // Transform Operations (Generic for all types)
    // ========================================

    fun updateElementTransform(
        id: String,
        offsetRatioX: Float,
        offsetRatioY: Float,
        rotation: Float,
        scale: Float
    ) {
        updateElement(id) { element ->
            when (element) {
                is EditElement.Text -> element.copy(
                    offsetRatioX = offsetRatioX,
                    offsetRatioY = offsetRatioY,
                    rotation = rotation,
                    scale = scale.coerceIn(0.5f, 3f)
                )
                is EditElement.Emoji -> element.copy(
                    offsetRatioX = offsetRatioX,
                    offsetRatioY = offsetRatioY,
                    rotation = rotation,
                    scale = scale.coerceIn(0.5f, 3f)
                )
                is EditElement.Shape -> element.copy(
                    offsetRatioX = offsetRatioX,
                    offsetRatioY = offsetRatioY,
                    rotation = rotation,
                    scale = scale.coerceIn(0.5f, 5f)
                )
                is EditElement.Drawing -> element // drawings don't transform via gesture
            }
        }
    }

    fun updateTextContent(elementId: String, newText: String) {
        updateElement(elementId) { element ->
            if (element is EditElement.Text) {
                element.copy(text = newText)
            } else element
        }
    }

    fun updateTextStyle(color: Color? = null, size: TextUnit? = null) {
        _state.update {
            it.copy(
                textColor = color ?: it.textColor,
                textSize = size ?: it.textSize
            )
        }
    }

    // ========================================
    // Drawing Operations
    // ========================================

    fun startDrawing(startPoint: Offset) {
        val newDrawing = EditElement.Drawing(
            points = listOf(startPoint),
            color = _state.value.drawingColor,
            strokeWidth = _state.value.drawingStrokeWidth,
            zIndex = _state.value.elements.size
        )

        _state.update {
            it.copy(elements = it.elements + newDrawing)
        }
    }

    fun continueDrawing(point: Offset) {
        _state.update { currentState ->
            val lastElement = currentState.elements.lastOrNull()

            if (lastElement is EditElement.Drawing) {
                val updatedDrawing = lastElement.copy(
                    points = lastElement.points + point
                )

                currentState.copy(
                    elements = currentState.elements.dropLast(1) + updatedDrawing
                )
            } else {
                currentState
            }
        }
    }

    fun finishDrawing() {
        pushToHistory()
    }

    fun updateDrawingStyle(color: Color? = null, strokeWidth: Float? = null) {
        _state.update {
            it.copy(
                drawingColor = color ?: it.drawingColor,
                drawingStrokeWidth = strokeWidth ?: it.drawingStrokeWidth
            )
        }
    }

    // ========================================
    // Selection
    // ========================================

    fun selectElement(elementId: String) {
        if (elementId.isEmpty()) {
            _state.update { it.copy(selectedElementId = null) }
        } else {
            _state.update { it.copy(selectedElementId = elementId) }
        }
    }

    fun deselectElement() {
        _state.update { it.copy(selectedElementId = null) }
    }

    // ========================================
    // Delete Operation
    // ========================================

    fun deleteElement(elementId: String) {
        _state.update {
            it.copy(
                elements = it.elements.filter { el -> el.id != elementId },
                selectedElementId = if (it.selectedElementId == elementId) null else it.selectedElementId
            )
        }
        pushToHistory()
    }

    fun deleteSelectedElement() {
        _state.value.selectedElementId?.let { id ->
            deleteElement(id)
        }
    }

    // ========================================
    // History Operations (Undo/Redo)
    // ========================================

    fun undo() {
        historyManager.undo()?.let { elements ->
            _state.update {
                it.copy(
                    elements = elements,
                    selectedElementId = null
                )
            }
            updateHistoryFlags()
        }
    }

    fun redo() {
        historyManager.redo()?.let { elements ->
            _state.update {
                it.copy(
                    elements = elements,
                    selectedElementId = null
                )
            }
            updateHistoryFlags()
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

    // ========================================
    // Clear All
    // ========================================

    fun clearAll() {
        _state.update {
            it.copy(
                elements = emptyList(),
                selectedElementId = null
            )
        }
        pushToHistory()
    }

    // ========================================
    // Helper Methods
    // ========================================

    private fun addElement(element: EditElement) {
        _state.update {
            it.copy(elements = it.elements + element)
        }
        pushToHistory()
    }

    fun loadImage(bitmap: Bitmap) {
        _state.update {
            it.copy(backgroundBitmap = bitmap)
        }
    }

    fun setPreviewScaleFactor(scaleFactor: Float) {
        if (scaleFactor <= 0f) return
        _state.update {
            if (it.previewScaleFactor == scaleFactor) it
            else it.copy(previewScaleFactor = scaleFactor)
        }
    }

    private fun updateElement(elementId: String, transform: (EditElement) -> EditElement) {
        _state.update { currentState ->
            currentState.copy(
                elements = currentState.elements.map { element ->
                    if (element.id == elementId) transform(element) else element
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveEditedImage(
        context: Context,
        quality: Int = 95,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ) {
        val backgroundBitmap = _state.value.backgroundBitmap

        if (backgroundBitmap == null) {
            _state.update { it.copy(error = "No image to save") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            context.saveEditedImage(
                backgroundBitmap = backgroundBitmap,
                elements = _state.value.elements,
                previewScaleFactor = _state.value.previewScaleFactor,
                quality = quality,
                format = format
            ).fold(
                onSuccess = { path ->
                    _state.update { it.copy(isLoading = false) }
                    _saveSuccess.value = path
                },
                onFailure = { throwable ->
                    Log.d("TAG", "saveEditedImage: $throwable")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to save image"
                        )
                    }
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        historyManager.clear()
    }
}