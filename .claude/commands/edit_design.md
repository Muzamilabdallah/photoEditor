I have a Jetpack Compose photo editor. The current layout uses a Scaffold with a bottomBar
that contains the style panel (colors + stroke slider) and the toolbar stacked in a Column.
This causes the canvas to shrink whenever the style panel appears.

I need you to refactor the layout so the canvas never resizes — all overlays float on top of it.

## Target layout structure

Replace the Scaffold bottomBar with a single root Box:

Box(Modifier.fillMaxSize()) {
EditorCanvas(Modifier.fillMaxSize())   // always full screen, never moves
StylePanel(...)                        // overlays canvas above toolbar
ShapeSheet(...)                        // bottom sheet over canvas
EditorToolbar(Modifier.align(BottomCenter))
}

## Files to edit

- PhotoEditorScreen.kt  — remove Scaffold bottomBar, replace content with Box overlay layout
- EditorCanvas.kt       — already fillMaxSize, no changes needed
- EditorToolbar.kt      — keep as-is, just positioned at BottomCenter
  - EditorStylePanel.kt   — add semi-transparent background, position above toolbar
- Any new composables you need to extract

## Style panel (EditorStylePanel) requirements

- position: Alignment.BottomCenter, padding bottom = toolbar height (72.dp)
- animate in/out with: AnimatedVisibility + slideInVertically/slideOutVertically
- background: Color(0xF0161616) with top border 0.5px
- show only when currentTool == EditorTool.Drawing OR currentTool == EditorTool.Text
- OR when selectedElementId is non-null and the selected element is Text/Drawing/Emoji
- content: color row (8 swatches) + one slider (stroke width or font size depending on tool)
- does NOT push the canvas up — it overlays it

## Shape sheet requirements

- position: Alignment.BottomCenter, slides up from bottom
- animate with: AnimatedVisibility + slideInVertically { it } / slideOutVertically { it }
- background: Color(0xF5161616), borderRadius top 16.dp
- show when currentTool is EditorTool.Shape
- content: 4x2 grid of shape buttons (Rect, Circle, Line, Triangle, Star, Heart, Arrow, Pentagon)
  + color row + stroke slider below the grid
- bottom padding = 72.dp so content clears the toolbar
- tapping a shape calls onAddShape(kind) then dismisses the sheet

## Toolbar requirements

- stays inside the root Box at Alignment.BottomCenter
- height ~72.dp, background Color(0xF0161616) with top border
- 5 buttons: Select, Text, Emoji, Draw, Shape
- tapping Text → shows TextInputDialog (existing behaviour)
- tapping Emoji → shows EmojiPickerDialog (existing behaviour)
- tapping Draw → activates draw tool + shows StylePanel overlay
- tapping Shape → shows ShapeSheet overlay
- tapping Select → dismisses all overlays

## Visibility state to add in PhotoEditorScreen

var showStylePanel by remember — true when tool is Draw/Text or element selected
var showShapeSheet by remember — true only when tool is Shape

## Do NOT change

- EditorCanvas internal logic
- ViewModel
- Any domain classes
- The gesture handling inside EditorCanvas

Keep all existing callbacks (onStartDrawing, onContinueDrawing, onUpdateElement, etc.) wired exactly as they are today.