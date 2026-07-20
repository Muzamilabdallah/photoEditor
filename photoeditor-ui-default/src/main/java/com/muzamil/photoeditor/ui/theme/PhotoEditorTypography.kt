package com.muzamil.photoeditor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Text-style tokens for the default editor UI, the typography counterpart to
 * [PhotoEditorColors]. No font family is set — styles inherit whatever the
 * host app provides (the library bundles no font). Color is deliberately not
 * part of these styles either: text color always comes from
 * [PhotoEditorColors] at the call site, so typography and color stay
 * independently themeable.
 *
 * Override the whole set via `PhotoEditorTheme(typography = ...)`.
 */
@Immutable
data class PhotoEditorTypography(
    /** Full-screen headings ("No image selected"). */
    val screenTitle: TextStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    /** Panel headers ("Shape", "Emoji"). */
    val panelTitle: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    /** Section labels within panels ("Color", "Stroke"). */
    val sectionLabel: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    /** Primary rows ("Fill shape"). */
    val body: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    /** Secondary descriptions ("Solid instead of outline"). */
    val bodySecondary: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    /** Shape chip and bottom-nav labels; selection may raise the weight. */
    val chipLabel: TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal),
    /** Value pills ("8 px"). */
    val valueLabel: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    /** Button labels ("Done", "Pick Image"). */
    val button: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

val LocalPhotoEditorTypography = staticCompositionLocalOf { PhotoEditorTypography() }
