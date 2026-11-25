# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses [Semantic Versioning](https://semver.org/) (pre-1.0: minor versions may break the API).

## [Unreleased] — 0.2.0

**Breaking.** Element coordinates and sizes changed meaning, so this is a
new minor version. Saved editor state from 0.1.x is discarded on restore
(the editor starts empty) rather than restored to the wrong place.

### Changed
- **Everything is relative to the photo.** Text, emoji and shape positions
  are now fractions of the bitmap (they were fractions of the preview
  container), and every size — font, emoji, shape box, stroke width — is a
  fraction of the bitmap's shorter side (they were sp or bitmap pixels).
  Edits no longer drift on rotation, window resize or restore, and a given
  stroke or text size looks the same on every photo.
- **The preview shows the whole photo** (`ContentScale.Fit`, letterboxed)
  instead of cropping it, so nothing is exported that wasn't visible while
  editing. Elements stay inside the photo. Shape outlines are the same
  thickness on screen and in the file.
- Renamed to make the unit change a compile error rather than a silent one:
  `Text.fontSize` → `fontSizeRatio`, `Emoji.size` → `sizeRatio`,
  `Drawing`/`Shape.strokeWidth` → `strokeWidthRatio`,
  `EditorState.textSize` → `textSizeRatio`,
  `EditorState.drawingStrokeWidth` → `drawingStrokeWidthRatio`,
  `ShapeToolState.strokeWidthPx` → `strokeWidthRatio`. The matching
  `PhotoEditorState.update…` parameters take ratios (`Float`) instead of
  `TextUnit`/pixels. `ShapeToolPanel.onStrokeWidthChanged` now reports a ratio.
- Built-in size sliders show unitless levels (ratio × 1000) instead of sp/px.
- `addText` now uses the text size default (`updateTextStyle(sizeRatio = …)`);
  it used to ignore it.

### Added
- `renderEdits(bitmap, elements)`: renders an edit without any UI, e.g. in a
  `Worker` or a test.
- Undo/redo now records moves, rotations, scales and style changes; one
  gesture or slider drag is one step. New `PhotoEditorState.commitPendingChanges()`
  for custom continuous controls.

### Fixed
- Strokes drawn over a text/emoji/shape show over it on screen, as in the export.
- New elements never reuse a `zIndex` after a delete.
- Saved emoji sit at the same height as on screen.
- In Draw mode a stroke can start on top of an element instead of dragging it.

### Removed
- `Context.saveEditedImage`'s `containerSize` parameter, and the
  "Preview not ready" save error: export no longer needs a laid-out preview.
- `EditorState.previewScaleFactor`, which was never written or read.
- Repository hygiene: `.idea/` is no longer tracked; issue/PR templates added.

## [0.1.1]

- Current published version (JitPack: `com.github.Muzamilabdallah`).

[Unreleased]: https://github.com/Muzamilabdallah/photoEditor/compare/0.1.1...HEAD
[0.1.1]: https://github.com/Muzamilabdallah/photoEditor/releases/tag/0.1.1
