# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

WhatsApp-style photo editor delivered as **two published Android libraries** plus a thin demo app. Built with Jetpack Compose. Kotlin 2.0.21, AGP 8.13.0, compileSdk 36, minSdk 24, Java 11 toolchain, Compose BOM 2024.09.00.

- `:photoeditor-core` — the UI-agnostic engine: canvas composable, element model, gestures, undo/redo, state holder, export. **No material3 dependency** — keep it that way; Mode A consumers must not inherit Material. Gradle namespace `com.muzamil.photoeditor.core`.
- `:photoeditor-ui-default` — the optional built-in UI: tool panels, bottom nav, top bar, token-based theme. Depends on core via `api` (consumers get core types transitively). Namespace `com.muzamil.photoeditor.uidefault`. **Dependency direction is one-way (`ui-default → core`), never the reverse.**
- `:app` — demo consumer with three sample screens: `DefaultUiSample` (Mode B, built-in UI), `CustomizedUiSample` (Mode B with custom emojis/swatches + `colorsFromMaterialTheme()` bridge), and `CustomUiSample` (Mode A, hand-rolled toolbar over core only — keep its imports core-only; it exists to prove the core API is sufficient).

All three share Kotlin package root `com.muzamil.photoeditor.*` (fine — AGP namespaces differ, so R classes don't collide), but `internal` visibility does NOT cross module boundaries: ui-default can only use core's public API.

Publishing: both library modules apply `maven-publish` (release variant + sources jar). Project-level `group`/`version` are set in each module's build file so ui-default's generated POM declares core with the right GAV. `jitpack.yml` builds both via `publishToMavenLocal` on JDK 17. Validate locally with `./gradlew :photoeditor-core:publishToMavenLocal :photoeditor-ui-default:publishToMavenLocal` and check `~/.m2/repository/com/github/Muzamilabdallah/`.

## Common commands

All commands use the Gradle wrapper at the repo root. (If `java` isn't on PATH, use Android Studio's JBR: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.)

- Build everything: `./gradlew :photoeditor-core:assembleDebug :photoeditor-ui-default:assembleDebug :app:assembleDebug`
- Install demo on device/emulator: `./gradlew :app:installDebug`
- Unit tests (JVM): `./gradlew :photoeditor-core:testDebugUnitTest` (the real test suite; ui-default and app have none of substance)
- Run a single unit test class: `./gradlew :photoeditor-core:testDebugUnitTest --tests "com.muzamil.photoeditor.geometry.CropGeometryTest"` (append `.methodName` for one test)
- Lint: `./gradlew :photoeditor-core:lintDebug :photoeditor-ui-default:lintDebug :app:lintDebug`
- Clean: `./gradlew clean`

## Architecture

Single-Activity Compose app for editing a picked image by overlaying text, emojis, freehand drawings, and predefined shapes, then exporting the composite to MediaStore.

README.md documents consumer-facing install/wiring for both modes; THEMING.md documents the token system. Don't re-document those here.

### Data flow (Mode B)

`MainActivity`/`DefaultUiSample` (`:app` — owns photo picking and uri persistence) → `PhotoEditor` (public entry, ui-default `ui/PhotoEditor.kt` — theme + default state holder; **no picker**: consumers pass `bitmap` or wire `onPickImage`, nullable = hides pick affordances) → `EditorHostContent` (adapter, `ui/EditorHost.kt`) → `PhotoEditorScreen` (stateless; receives the canvas as a **slot** — core's `EditorCanvas` is `internal`, so the host injects the public `PhotoEditorCanvas(state)`).

Mode A consumers skip all of that and use core's public `PhotoEditorCanvas` (`canvas/PhotoEditorCanvas.kt`) — the editing surface alone, auto-wired to the state holder.

State is owned by `PhotoEditorState` (core, `state/PhotoEditorState.kt`), a plain Compose state holder created via `rememberPhotoEditorState()` — deliberately **not** an AAC ViewModel. It exposes `editorState`/`canUndo`/`canRedo`/`saveSuccess`/`selectedElementBounds`/`isInteracting` as snapshot state; all mutations go through its methods. `isInteracting` is true for as long as a canvas gesture is in flight (freehand stroke, or element drag/rotate/scale) — the canvas reports it, and UIs fade their chrome out while it's set so nothing covers the stroke. Retention across rotation/process death via custom `Saver` (`state/PhotoEditorStateSaver.kt`) — bitmap intentionally excluded (too large); persisting the image *uri* and re-decoding on restore is the consumer's job. Undo history is not retained.

### Shape tool state

`ShapeToolState` (core, `domain/ShapeToolState.kt`): `selectedShape`/`selectedColor`/`strokeWidthPx`/`isFilled` — the pending style for the next shape, held in `EditorState.shapeTool` and mutated via `PhotoEditorState.updateShapeTool(...)`. `EditorTool.Shape` is a plain `data object` marker; the selected shape lives ONLY in `shapeTool.selectedShape` (single source of truth shared by built-in and custom UIs). `state.addShape()` (no args) places the tool's selected shape. `EditorShape` enum: Rect, Circle, Line, Triangle, Star, Heart, Arrow. `EditorColors` (core) is the 8-color shared palette.

### Element model (core, `domain/EditElement.kt`)

`EditElement` is sealed: `Text`, `Emoji`, `Drawing`, `Shape`. Coordinates are ratios in `0f..1f`, but **not all in the same space** (known issue; unifying on bitmap space is planned for 0.2.0):

- `Drawing.points` — each `Offset` is `(x, y)` as a fraction of the **source bitmap** (mapped via `CropGeometry`).
- `Text`/`Emoji`/`Shape` — `offsetRatioX`/`offsetRatioY` (the element's center) as a fraction of the **preview container**.
- `Shape.widthRatio`/`heightRatio` — bounding box as a fraction of the **preview container**.

The exporter reconciles the two spaces using the live `containerSize` (see `ImageExporter`'s header comment). Stroke widths for `Drawing` and `Shape` are stored in **bitmap-space pixels** (not ratios); text/emoji sizes are in `sp`.

### Rendering paths (two places, must stay in sync)

1. **On-screen preview** — core `canvas/EditorCanvas.kt` does all gesture handling and ratio↔pixel math. Image displayed with `ContentScale.Crop`; `CropGeometry` is the single source of truth for the screen↔ratio mapping (do **not** use the `minOf`-based `scaleFactor` for position math; it only scales stroke widths and text/emoji sizes). `DraggableElement` does rotation-compensated pan math. `Shape` renders via `ShapeElementContent.kt`, which draws paths from the public `EditorShapePaths` (also used by ui-default's shape chips and available to custom UIs).

2. **Export** — `Context.saveEditedImage` in core `export/ImageExporter.kt`: re-draws all elements onto a copy of the bitmap with `android.graphics.Canvas`, sorted by `zIndex`, then writes via MediaStore (API 29+) or legacy public dir (<29). Its shape path builders mirror `EditorShapePaths` geometries around the origin.

**When adding or modifying an element type or its transform, update both rendering paths**, plus `PhotoEditorState.updateElementTransform` and the element's branch in `state/PhotoEditorStateSaver.kt`. When adding an `EditorShape` entry: `EditorShapePaths` (preview + chips) and `ImageExporter` (export) both need it; the panel grid picks it up automatically.

### History (undo/redo)

`history/HistoryManager` (core): snapshots of `List<EditElement>` in two deques, bounded at 50. Transform updates during a drag do **not** push to history — only the terminal action does. History does not track background bitmap or style defaults.

### `:photoeditor-core` layout (`src/main/java/com/muzamil/photoeditor/`)

- `domain/` — `EditElement.kt` (sealed elements + `EditorShape` + `EditorState` + `EditorTool`), `ShapeToolState.kt`, `EditorColors.kt`. Pure data.
- `canvas/` — `PhotoEditorCanvas.kt` (public surface), `EditorCanvas.kt`, `DraggableElement.kt`, `TextElementContent.kt` (uses `BasicText`, NOT material3), `ShapeElementContent.kt` (internal), `EditorShapePaths.kt` (public path builder).
- `image/BitmapDecoding.kt` — public `decodeEditableBitmap`: software allocation (hardware bitmaps crash the exporter's pixel copy), ≤4096px downsampling.
- `export/` — `ImageExporter.kt`, `DrawingPathBuilder.kt` (bezier smoothing shared by preview + export), `SaveDestination.kt` (public sealed: `Gallery(subFolder)` / `File(file)`).
- `geometry/CropGeometry.kt` — ContentScale.Crop screen↔ratio transform (internal, unit-tested).
- `history/HistoryManager.kt` — undo/redo deque (internal).
- `state/` — `PhotoEditorState.kt` (public holder + `rememberPhotoEditorState()`), `PhotoEditorStateSaver.kt` (internal).
- Tests live here: `src/test/.../{geometry,history,state}/`.

### `:photoeditor-ui-default` layout (`src/main/java/com/muzamil/photoeditor/ui/`)

- `PhotoEditor.kt` — public entry: theme + host; optional `colors`/`typography` token overrides, `onClose` (null hides the close button), `emojis`, `swatchColors`.
- `PhotoEditorScreen.kt` — the layout (a plain `Column`, no Material `Scaffold`), panel orchestration, empty state; takes `canvasContent` slot. `internal`. **The bars take layout space rather than overlaying the canvas**: top bar → `weight(1f)` canvas band → bottom toolbar, so the band is exactly the reachable editing area and nothing can be drawn or parked behind the chrome. Keep that inset **constant** — element offsets are fractions of the measured container (see `ImageExporter`'s header comment), so a container that resized when a sheet opened would shift every element. That's also why the chrome fades via `graphicsLayer { alpha }` while `isInteracting`, never via `AnimatedVisibility`. Tool sheets (`ToolSheets`) float over the bottom of the band with no dismiss scrim — the canvas stays live underneath, and they close via their own header, Shape's Done, picking an emoji, or switching tabs.
- `EditorHost.kt` — `EditorHostContent` adapter (internal): state holder → screen params + canvas slot.
- `shapetool/ShapeToolPanel.kt` — **public** stateless Shape panel (header, 4-column shape grid, color swatches, stroke slider + live px pill, fill toggle) + `@Preview`s (light/dark/selection states). Binds to core's `ShapeToolState` via callbacks only.
- `emojitool/EmojiToolPanel.kt` — **public** stateless Emoji panel: grid of tappable strings, `emojis: List<String> = PhotoEditorDefaults.emojis` (any string works — text-sticker shortcut; core is list-agnostic).
- `components/` — `EditorTopBar.kt` (**public** stateless top bar: accent confirm circle at start; add-image/undo/redo/close utility cluster at end; takes core's `EditorHistoryState`; null `onAddImage`/`onClose` hide the buttons), `EditorToolbar.kt` (bottom nav tabs), `EditorStylePanel.kt` (Text/Emoji/Draw/selected-element styling + delete-selected), `ColorSwatches.kt` (`ColorSwatchRow` — THE shared selection language for colors: 34dp swatch, double ring + checkmark, luminance-computed checkmark contrast), `TextInputDialog.kt`. Internal except `EditorTopBar`.
- `icons/EditorIcons.kt` — internal hand-built `ImageVector`s (Undo/Redo `autoMirror`, AddPhoto, ImagePlaceholder) so the lib avoids the material-icons-extended dependency; keep it that way.
- `theme/` — `PhotoEditorTheme.kt` (public tokens: `PhotoEditorColors` (9 slots), `PhotoEditorShapes`, the CompositionLocals, `lightPhotoEditorColors()`/`darkPhotoEditorColors()`, `PhotoEditorTheme(colors, typography, shapes)`; derives an internal Material colorScheme so M3 dialogs/sliders match), `PhotoEditorTypography.kt` (8 text-style tokens, no fontFamily/no color — color comes from `PhotoEditorColors` at call sites), `PhotoEditorDefaults.kt` (`emojis` (Unicode ≤ 13), `swatchColors` = re-export of core's `EditorColors.Palette`, `lightColors()`/`darkColors()`, opt-in `colorsFromMaterialTheme()` bridge — the lib never reads `MaterialTheme` implicitly). **Zero hardcoded colors or sp sizes in components** — everything reads the locals (`grep -rnE "\.sp\b" photoeditor-ui-default/src` must only hit `theme/`); keep it that way.

Design rule: every selectable element (shape chips, swatches, nav tabs) uses one selection language — `accentContainer` bg + `accent` border/tint when selected, `surfaceVariant` + `onSurfaceVariant` when not, with a non-color cue (border width, checkmark, label weight).

### Permissions

**Neither library module declares any permission** — keep it that way so consumers never inherit one. MediaStore writes (and the demo's `PickVisualMedia` picker) need no permission on API 29+. `SaveDestination.Gallery` legacy writes on API 24–28 need `WRITE_EXTERNAL_STORAGE` — declaring it (with `maxSdkVersion=28`) and requesting it at runtime is the consumer's job; the demo app's manifest shows the entry. `SaveDestination.File` (app-private paths) needs no permission.
