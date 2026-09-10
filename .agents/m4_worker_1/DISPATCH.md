# Dispatch for m4_worker_1

## Objective
Implement Milestone 4 (R4: Flicker-Free UI & Conflict-Free Gesture Selection in Grids) and complete M3/M4 integration across UI screens and ViewModels.

## Mandatory Reading
- `c:/Users/AJAYJKN/AndroidStudioProjects/AiPdfReaderEditor/.agents/ORIGINAL_REQUEST.md`
- `c:/Users/AJAYJKN/AndroidStudioProjects/AiPdfReaderEditor/PROJECT.md`
- `c:/Users/AJAYJKN/AndroidStudioProjects/AiPdfReaderEditor/.agents/survey_explorer_3/handoff.md`

## Scope & Target Files
1. `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/ui/tools/grid/PdfGridComponents.kt`:
   - Direct Compose `Image(bitmap = bitmap.asImageBitmap(), ...)` 0ms synchronous rendering (no Coil AsyncImage on bitmap).
   - Full-screen high-res preview dialog on long-press with dismiss.
   - Conflict-free drag-to-select on `LazyVerticalGrid` using `detectDragGesturesAfterLongPress` with layoutInfo hit-testing (`offset.x.toInt() in info.offset.x..(info.offset.x + info.size.width) && offset.y.toInt() in info.offset.y..(info.offset.y + info.size.height)`).
   - Safe heap-based byte-counted `PdfThumbnailCache` with `clearForUri(uri)`.
   - `DisposableEffect(uri)` lifecycle hook calling `pdfRendererPool.closeUri(uri)` and `PdfThumbnailCache.clearForUri(uri)`.
2. `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/ui/tools/grid/DeletePagesViewModel.kt`:
   - Add `selectRange(startIndex: Int, endIndex: Int)`, `selectAll()`, `clearSelection()`.
3. `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/ui/tools/grid/ExtractPagesViewModel.kt`:
   - Add `selectRange(startIndex: Int, endIndex: Int)`, `selectAll()`, `clearSelection()`.
4. `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/ui/tools/grid/SplitPdfViewModel.kt`:
   - Add `selectRange(startIndex: Int, endIndex: Int)`, `selectAll()`, `clearSelection()`.
5. `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/ui/tools/grid/DeletePagesScreen.kt`, `ExtractPagesScreen.kt`, `SplitPdfScreen.kt`:
   - Pass `onDragSelectRange = { start, end -> viewModel.selectRange(start, end) }` to `PdfThumbnailGrid`.

## Build & Test Verification
Run `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` to verify clean build and tests pass.
Document all results in `handoff.md`.

## Mandatory Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A forensic auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
