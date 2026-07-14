package com.muzamil.photoeditor.domain

import androidx.compose.ui.graphics.Color

/**
 * The editor's default drawing palette — shared by the built-in tool panels
 * and available to custom UIs so both offer the same colors.
 */
object EditorColors {
    val Black = Color.Black
    val White = Color.White
    val Red = Color.Red
    val Orange = Color(0xFFFF9800)
    val Yellow = Color.Yellow
    val Green = Color(0xFF4CAF50)
    val Blue = Color.Blue
    val Purple = Color(0xFF673AB7)

    /** All palette colors, in display order. */
    val Palette: List<Color> = listOf(
        Black, White, Red, Orange, Yellow, Green, Blue, Purple,
    )
}
