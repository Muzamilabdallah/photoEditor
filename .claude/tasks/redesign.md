# Task: Redesign the Shape tool panel in the photoEditor library

You are working in my Android photo editor library (Jetpack Compose). The library will be published as open source with two independently installable modules: a UI-agnostic core (canvas/engine) and an optional default UI module. This task (a) redesigns the Shape tool bottom panel in the default UI module, and (b) restructures the project so consumers can install the core alone and bring their own UI, or install both and get the built-in design.

## Module split — two published artifacts (do this first)

Restructure the project so it publishes two Maven artifacts:

```
:photoeditor-core        →  com.<mygroup>:photoeditor-core
:photoeditor-ui-default  →  com.<mygroup>:photoeditor-ui-default
```

Consumer installation must work in exactly two modes:

```kotlin
// Mode A — canvas only, consumer builds their own toolbar UI
implementation("com.<mygroup>:photoeditor-core:<version>")

// Mode B — canvas + built-in panels (Shape, Text, Draw, Emoji)
implementation("com.<mygroup>:photoeditor-core:<version>")
implementation("com.<mygroup>:photoeditor-ui-default:<version>")
```

Rules for the split:

- **`:photoeditor-core`** contains the editor canvas composable, the drawing engine, undo/redo, export, and ALL tool state models and enums — including `ShapeToolState`, `EditorShape`, and default color constants. State models live in core (not the UI module) because a consumer in Mode A drives the canvas with these same types from their own custom UI. Core must have zero dependency on the UI module and no opinion about how panels look.
- **`:photoeditor-ui-default`** contains only presentation: `ShapeToolPanel`, the other tool panels, the bottom tool nav, and `PhotoEditorTheme`. It depends on `:photoeditor-core` (`api`, so consumers get core types transitively in Mode B) and reads/writes state exclusively through core's public types and callbacks. Nothing in core may reference anything in this module.
- **Dependency direction is one-way:** `ui-default → core`, never the reverse. Enforce it — if the current code has UI classes referenced from engine code, extract interfaces into core and move the implementations.
- Update the sample app to demonstrate BOTH modes: one sample screen using the default UI, and one minimal sample driving the canvas with a custom hand-rolled toolbar to prove the core API is sufficient on its own.
- Wire Maven publishing (maven-publish or vanniktech plugin, matching whatever the repo already uses) so both artifacts publish with correct POM dependencies — `ui-default`'s POM must declare `core` as a dependency.
- Update the README with an "Installation" section showing both modes and a one-paragraph explanation of when to pick each.

## Architecture requirements

1. **Stateless, hoisted panel composable.** `ShapeToolState` lives in `:photoeditor-core` (see module rules above); the panel below lives in `:photoeditor-ui-default` and must be a plain `@Composable` with no internal ViewModel and no singletons:

```kotlin
data class ShapeToolState(
    val selectedShape: EditorShape = EditorShape.Rect,
    val selectedColor: Color = EditorColors.Red,
    val strokeWidthPx: Float = 8f,
    val isFilled: Boolean = false,
)

enum class EditorShape { Rect, Circle, Line, Triangle, Star, Heart, Arrow }

@Composable
fun ShapeToolPanel(
    state: ShapeToolState,
    onShapeSelected: (EditorShape) -> Unit,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onFilledChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
)
```

2. **Design tokens via a theme, zero hardcoded colors.** Create a `PhotoEditorTheme` with a CompositionLocal so consumers restyle the default UI in a few lines instead of forking:

```kotlin
@Immutable
data class PhotoEditorColors(
    val surface: Color,        // panel background
    val surfaceVariant: Color, // unselected chip background
    val onSurface: Color,      // primary text/icons
    val onSurfaceVariant: Color, // secondary text/icons
    val accent: Color,         // selection tint, active states, Done button
    val onAccent: Color,       // text/icon on accent
    val accentContainer: Color, // light accent tint for selected chip backgrounds
    val onAccentContainer: Color,
    val outline: Color,        // hairline borders
)

@Immutable
data class PhotoEditorShapes(
    val panel: CornerBasedShape,   // 16.dp
    val chip: CornerBasedShape,    // 12.dp
    val control: CornerBasedShape, // 8.dp
)

// Provide LocalPhotoEditorColors / LocalPhotoEditorShapes with sensible
// dark + light defaults; PhotoEditorTheme(colors, shapes) { content }
```

3. Add `@Preview` composables for the panel in light and dark, and for each selection state.

## Visual design spec — apply exactly

The core principle: **one selection language everywhere**. Every selectable element (shape chips, color swatches, bottom nav tabs) uses the same pattern — `accentContainer` background + `accent` border/tint when selected; `surfaceVariant` background + `onSurfaceVariant` content when not. Remove the current inconsistency (filled circle for Rect vs ring for color vs blue text for nav).

### 1. Panel header
- Row: close icon button (X, 36.dp circular touch target) on the start, centered title "Shape" (15.sp, Medium), and a "Done" pill button on the end (accent background, onAccent text, 20.dp corner radius, 6.dp vertical / 16.dp horizontal padding, 14.sp Medium).

### 2. Shape picker
- Replace the two loose rows with a **4-column grid** (`LazyVerticalGrid` or a fixed `Row`/`Column` layout, 8.dp gaps) containing all 7 shapes: Rect, Circle, Line, Triangle, Star, Heart, Arrow.
- Each chip: icon (20.dp) above an 11.sp label, 10.dp vertical padding, 12.dp corner radius.
- Selected: 2.dp `accent` border + `accentContainer` background + `accent` icon/label tint, label weight Medium.
- Unselected: 0.5.dp `outline` border + `surfaceVariant` background + `onSurfaceVariant` tint.
- Grid must accommodate future shapes (polygon, rounded rect, speech bubble) without layout changes.

### 3. Color swatches
- Section label "Color" (12.sp, onSurfaceVariant) above the row.
- Swatches: 34.dp circles, 10.dp gaps, wrap to next line if needed. Keep the existing 8 colors (black, white, red, orange, yellow, green, blue, purple).
- Selected swatch: **double ring + checkmark** — 2.dp gap of panel surface color, then 2.dp `accent` ring (use `Modifier.border` twice or a `drawBehind`), plus a centered check icon (16.dp). Checkmark color: dark (near-black) on white/yellow swatches, white on all others. The checkmark is required — a ring alone fails when the swatch color is close to the ring color.
- All swatches get a 0.5.dp `outline` border so white is visible on light surfaces.

### 4. Stroke control
- Header row: label "Stroke" (12.sp, onSurfaceVariant) on the start; a **live value pill** on the end showing e.g. "8 px" (13.sp Medium, `accentContainer` background, `onAccentContainer` text, 10.dp corner radius, 2.dp/10.dp padding).
- Below: a single standard `Slider` (range 1f..40f, steps producing integer values), full width, accent-colored active track and thumb. Remove the current two-piece control that looks like a toggle glued to a slider.
- The pill updates live as the slider moves; always display a rounded integer.

### 5. Filled toggle
- Row separated by a 0.5.dp top divider (`outline`), 12.dp vertical padding.
- Start: two-line text — "Fill shape" (14.sp Medium, onSurface) over "Solid instead of outline" (12.sp, onSurfaceVariant). The description is required; "Filled" alone is ambiguous.
- End: a `Switch` (or custom 48×28.dp toggle, 14.dp radius). Off: `surfaceVariant` track, `onSurfaceVariant` knob. On: `accent` track, `onAccent` knob. Animate the knob position and colors (150ms).

### 6. Bottom tool nav (Text / Emoji / Draw / Shape)
- 4 equal-width tabs, each: icon inside a 40×26.dp pill above an 11.sp label.
- Active tab: `accentContainer` pill background + `accent` icon/label, label Medium. Inactive: transparent pill, `onSurfaceVariant` tint.
- Separated from the panel content by a 0.5.dp top divider.

### Accessibility
- Every icon-only button gets a `contentDescription` (close, each swatch as "Color <name>", toggle as "Fill shape").
- All touch targets ≥ 44.dp (use `Modifier.minimumInteractiveComponentSize()` where the visual is smaller).
- Selection state must not rely on color alone — the border-width change, checkmark, and label weight changes cover this; keep them.

## Constraints
- Kotlin + Jetpack Compose only, no XML. Material 3 components are fine internally but the public API must only expose the `PhotoEditorTheme` tokens, not require consumers to use M3.
- No new third-party dependencies.
- Keep the panel free of any reference to engine internals beyond core's public types (`ShapeToolState` + callbacks) — the panel must compile in `:photoeditor-ui-default` against `:photoeditor-core`'s public API only.
- Follow the existing code style in this repo; if the current Shape panel lives inside a Fragment or a monolithic composable, extract it into `:photoeditor-ui-default` as `shapetool/ShapeToolPanel.kt`, with `ShapeToolState.kt` in `:photoeditor-core` and theme files under `:photoeditor-ui-default`'s `theme/`.
- After implementing, run the existing build/lint tasks for BOTH modules and the sample app, and fix any failures.

## Deliverables
1. Two-module split: `:photoeditor-core` and `:photoeditor-ui-default` as independently publishable artifacts with one-way dependency (`ui-default → core`) and publishing config for both.
2. `ShapeToolState`, `EditorShape`, and default color constants in `:photoeditor-core`.
3. `PhotoEditorTheme` (colors, shapes, CompositionLocals, dark + light defaults) in `:photoeditor-ui-default`.
4. Stateless `ShapeToolPanel` implementing the visual spec above, in `:photoeditor-ui-default`.
5. Updated call site wiring state hoisting (remember/ViewModel stays at the caller).
6. Sample app demonstrating both installation modes: default UI, and custom toolbar over core alone.
7. Previews for light/dark and key states.
8. README "Installation" section covering both modes, plus a `THEMING.md` section showing how a consumer overrides the theme in ~5 lines and how to replace the panel entirely while reusing the canvas.
   Additional constraint: ShapeToolPanel is a plain layout, NOT a sheet. Do not wrap it in ModalBottomSheet, BottomSheetScaffold, or Dialog — it renders as container-free content sized by its modifier, with background/corners from the theme. The consumer chooses the container. In the sample app, anchor it inline above the canvas (Box + Modifier.align(Alignment.BottomCenter)), non-modal, no scrim — the canvas must stay visible and touchable while the panel is open.