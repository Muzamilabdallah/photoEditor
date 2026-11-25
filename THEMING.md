# Theming the default UI

Everything `photoforge-ui` draws — panels, chips, swatches, sliders, the bottom nav — reads its colors, text styles, and corner shapes from three token sets provided by `PhotoEditorTheme`:

- `PhotoEditorColors` — `surface`, `surfaceVariant`, `onSurface`, `onSurfaceVariant`, `accent`, `onAccent`, `accentContainer`, `onAccentContainer`, `outline`
- `PhotoEditorTypography` — `screenTitle`, `panelTitle`, `sectionLabel`, `body`, `bodySecondary`, `chipLabel`, `valueLabel`, `button`
- `PhotoEditorShapes` — `panel` (16.dp), `chip` (12.dp), `control` (8.dp) corner shapes

There are no hardcoded colors or font sizes in the panels, so restyling never requires forking.

## Colors: three tiers

1. **Do nothing** — the editor uses its own palette and follows the system light/dark setting. The library never reads your `MaterialTheme` implicitly.
2. **Follow your app's Material theme (opt-in)** — bridge the current `ColorScheme` (including dynamic color) onto the editor tokens:

   ```kotlin
   PhotoEditor(colors = PhotoEditorDefaults.colorsFromMaterialTheme())
   ```

3. **Full control** — construct `PhotoEditorColors(...)` yourself (no Material dependency required), or start from the built-in sets and change what you need:

```kotlin
PhotoEditor(
    state = state,
    onPickImage = ::pickImage,
    colors = darkPhotoEditorColors().copy(
        accent = Color(0xFF25D366),          // brand green
        accentContainer = Color(0xFF103A2A),
        onAccentContainer = Color(0xFFA7F3C9),
    ),
)
```

## Typography

The text tokens carry size and weight only — text *color* always comes from `PhotoEditorColors`, so the two are independently themeable. No font is bundled; styles inherit the system (or host app) font unless you set one:

```kotlin
PhotoEditor(
    typography = PhotoEditorTypography().copy(
        panelTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        button = TextStyle(fontFamily = BrandFont, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    ),
)
```

## Content palette vs chrome tokens

`PhotoEditorColors` themes the editor's *chrome*. What users draw with is a separate, overridable list — `PhotoEditorDefaults.swatchColors` — shared by every color picker (Shape, Draw, Text), so a color picked in one tool is available in the others. The emoji grid is customizable the same way:

```kotlin
PhotoEditor(
    swatchColors = listOf(BrandTeal, PastelPink, Color.Black, Color.White),
    emojis = listOf("🍕", "🍔", "☕", "⭐", "SALE"),  // any string renders at sticker size
)
```

Swatch checkmark contrast is computed from luminance, so pastels and neons work without configuration. See `app/src/main/java/com/muzamil/photoeditor/CustomizedUiSample.kt` for a complete example of all three overrides.

Or wrap any part of your UI in the theme directly — useful when composing the panels yourself:

```kotlin
PhotoEditorTheme(
    colors = lightPhotoEditorColors().copy(accent = BrandBlue),
    shapes = PhotoEditorShapes(chip = RoundedCornerShape(6.dp)),
) {
    /* editor UI here */
}
```

The theme also derives an internal Material 3 color scheme from the same tokens so the few M3 components used inside (dialogs, sliders) match automatically — you never need to configure Material yourself.

## Replace a panel entirely, keep the canvas

The built-in panels are just stateless composables over core types. If the Shape panel (or all of the chrome) doesn't fit your design, drop to `photoforge-core` and reuse the canvas — it handles every editing gesture on its own:

```kotlin
import io.github.photoforge.canvas.PhotoEditorCanvas
import io.github.photoforge.domain.EditorShape
import io.github.photoforge.domain.ShapeToolState

val state = rememberPhotoEditorState()
val shapeTool: ShapeToolState = state.editorState.shapeTool

Column {
    PhotoEditorCanvas(state, Modifier.weight(1f))

    MyOwnShapePanel(                       // your composable, your design
        selectedShape = shapeTool.selectedShape,
        selectedColor = shapeTool.selectedColor,
        strokeWidthRatio = shapeTool.strokeWidthRatio, // fraction of the photo's shorter side
        isFilled = shapeTool.isFilled,
        onShapeSelected = { state.updateShapeTool(shape = it) },
        onColorSelected = { state.updateShapeTool(color = it) },
        onStrokeChanged = { state.updateShapeTool(strokeWidthRatio = it) },
        onFilledChanged = { state.updateShapeTool(filled = it) },
        onDone = { state.addShape() },     // places shapeTool.selectedShape
    )
}
```

`ShapeToolState`, `EditorShape`, and the `EditorColors` palette all live in core, so your panel binds to exactly the same state the built-in `ShapeToolPanel` uses — and `EditorShapePaths.path(shape, w, h)` gives you the same icon geometry for your picker. A complete working example is `app/src/main/java/com/muzamil/photoeditor/CustomUiSample.kt`.
