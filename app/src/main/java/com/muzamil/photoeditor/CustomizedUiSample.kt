package com.muzamil.photoeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.photoforge.image.decodeEditableBitmap
import io.github.photoforge.state.rememberPhotoEditorState
import io.github.photoforge.ui.PhotoEditor
import io.github.photoforge.ui.theme.PhotoEditorDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mode B sample with everything customized, proving the override points:
 *
 * - **Custom emoji list** — a domain-specific set (a food business) instead
 *   of [PhotoEditorDefaults.emojis], including a plain-text "SALE" sticker.
 * - **Custom swatch palette** — brand colors including light pastels, which
 *   exercise the luminance-based checkmark contrast (dark check on pastels).
 * - **Material theme bridge** — the editor takes its chrome colors from the
 *   app's `MaterialTheme` via [PhotoEditorDefaults.colorsFromMaterialTheme]
 *   instead of the editor-native palette.
 */
@Composable
fun CustomizedUiSample(onClose: (() -> Unit)? = null) {
    val context = LocalContext.current
    val state = rememberPhotoEditorState()
    var pickedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedImageUri = uri
    }

    LaunchedEffect(pickedImageUri) {
        val uri = pickedImageUri ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            decodeEditableBitmap(context.contentResolver, uri)
        }?.let(state::loadImage)
    }

    // A branded app theme; the editor follows it through the bridge below.
    MaterialTheme(colorScheme = BrandColorScheme) {
        PhotoEditor(
            state = state,
            onPickImage = {
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onClose = onClose,
            colors = PhotoEditorDefaults.colorsFromMaterialTheme(),
            emojis = FoodEmojis,
            swatchColors = BrandSwatches,
        )
    }
}

private val BrandColorScheme = darkColorScheme(
    primary = Color(0xFF00BFA5),
    onPrimary = Color(0xFF00332C),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFF76F8E2),
)

private val FoodEmojis = listOf(
    "🍕", "🍔", "🌮", "🍣", "🍜", "🥗", "🍰", "🍩",
    "☕", "🧃", "🥤", "🍦", "🔥", "⭐", "✅", "SALE",
)

/** Includes light pastels to show the computed checkmark contrast. */
private val BrandSwatches = listOf(
    Color(0xFF00BFA5), // brand teal
    Color(0xFFF8BBD0), // pastel pink
    Color(0xFFB2DFDB), // pastel mint
    Color(0xFFFFF9C4), // pastel lemon
    Color(0xFF1A1B1E), // near-black
    Color(0xFFFFFFFF), // white
)
