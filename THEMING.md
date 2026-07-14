# Theming the default UI

Everything `photoeditor-ui-default` draws — panels, chips, swatches, sliders, the bottom nav — reads its colors and corner shapes from two token sets provided by `PhotoEditorTheme`:

- `PhotoEditorColors` — `surface`, `surfaceVariant`, `onSurface`, `onSurfaceVariant`, `accent`, `onAccent`, `accentContainer`, `onAccentContainer`, `outline`
- `PhotoEditorShapes` — `panel` (16.dp), `chip` (12.dp), `control` (8.dp) corner shapes

There are no hardcoded colors in the panels, so restyling never requires forking.

## Override the theme in ~5 lines

Pass custom colors straight to the `PhotoEditor` composable (start from the built-in light/dark sets and change what you need):

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

Or wrap any part of your UI in the theme directly — useful when composing the panels yourself:

```kotlin
PhotoEditorTheme(
    colors = lightPhotoEditorColors().copy(accent = BrandBlue),
    shapes = PhotoEditorShapes(chip = RoundedCornerShape(6.dp)),
) {
    /* editor UI here */
}
```

By default `PhotoEditorTheme` follows the system light/dark setting. The theme also derives an internal Material 3 color scheme from the same tokens so the few M3 components used inside (dialogs, sliders) match automatically — you never need to configure Material yourself.

## Replace a panel entirely, keep the canvas

The built-in panels are just stateless composables over core types. If the Shape panel (or all of the chrome) doesn't fit your design, drop to `photoeditor-core` and reuse the canvas — it handles every editing gesture on its own:

```kotlin
import com.muzamil.photoeditor.canvas.PhotoEditorCanvas
import com.muzamil.photoeditor.domain.EditorShape
import com.muzamil.photoeditor.domain.ShapeToolState

val state = rememberPhotoEditorState()
val shapeTool: ShapeToolState = state.editorState.shapeTool

Column {
    PhotoEditorCanvas(state, Modifier.weight(1f))

    MyOwnShapePanel(                       // your composable, your design
        selectedShape = shapeTool.selectedShape,
        selectedColor = shapeTool.selectedColor,
        strokeWidth = shapeTool.strokeWidthPx,
        isFilled = shapeTool.isFilled,
        onShapeSelected = { state.updateShapeTool(shape = it) },
        onColorSelected = { state.updateShapeTool(color = it) },
        onStrokeChanged = { state.updateShapeTool(strokeWidth = it) },
        onFilledChanged = { state.updateShapeTool(filled = it) },
        onDone = { state.addShape() },     // places shapeTool.selectedShape
    )
}
```

`ShapeToolState`, `EditorShape`, and the `EditorColors` palette all live in core, so your panel binds to exactly the same state the built-in `ShapeToolPanel` uses — and `EditorShapePaths.path(shape, w, h)` gives you the same icon geometry for your picker. A complete working example is `app/src/main/java/com/muzamil/photoeditor/CustomUiSample.kt`.
