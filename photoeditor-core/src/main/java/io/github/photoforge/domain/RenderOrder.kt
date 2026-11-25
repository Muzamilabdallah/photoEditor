package io.github.photoforge.domain

/**
 * The order elements are painted in, bottom to top. The on-screen preview
 * (`EditorCanvas`) and the exporter (`ImageExporter`) both use this, so a
 * stroke drawn over a text is over it in both places. `sortedBy` is stable:
 * elements with equal [EditElement.zIndex] keep their list order.
 */
internal fun List<EditElement>.inRenderOrder(): List<EditElement> = sortedBy { it.zIndex }

/**
 * The zIndex for a new element: one above the current top. Using the list
 * size instead would reuse an existing zIndex after a delete.
 */
internal fun List<EditElement>.nextZIndex(): Int = (maxOfOrNull { it.zIndex } ?: -1) + 1
