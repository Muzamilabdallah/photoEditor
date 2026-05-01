package com.muzamil.photoeditor.history

import com.muzamil.photoeditor.domain.EditElement

internal class HistoryManager {
    private val undoStack = ArrayDeque<List<EditElement>>()
    private val redoStack = ArrayDeque<List<EditElement>>()
    private var currentState: List<EditElement> = emptyList()
    
    fun initialize(elements: List<EditElement>) {
        currentState = elements
        undoStack.clear()
        redoStack.clear()
    }
    
    fun pushState(elements: List<EditElement>) {
        // Save current state to undo stack
        undoStack.addLast(currentState)
        currentState = elements
        
        // Clear redo stack when new action is performed
        redoStack.clear()
        
        // Limit undo stack size
        if (undoStack.size > MAX_HISTORY_SIZE) {
            undoStack.removeFirst()
        }
    }
    
    fun undo(): List<EditElement>? {
        if (undoStack.isEmpty()) return null
        
        // Move current state to redo stack
        redoStack.addLast(currentState)
        
        // Pop from undo stack
        currentState = undoStack.removeLast()
        
        return currentState
    }
    
    fun redo(): List<EditElement>? {
        if (redoStack.isEmpty()) return null
        
        // Move current state to undo stack
        undoStack.addLast(currentState)
        
        // Pop from redo stack
        currentState = redoStack.removeLast()
        
        return currentState
    }
    
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        currentState = emptyList()
    }
    
    companion object {
        private const val MAX_HISTORY_SIZE = 50
    }
}