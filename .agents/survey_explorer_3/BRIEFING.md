# BRIEFING — 2026-08-23T04:22:15Z

## Mission
Deeply investigate R3 (Memory Safety & Lifecycle-Bound PDF Rendering) and R4 (Flicker-Free UI & Gesture Selection in Grids) across the codebase, identifying architecture, current implementations, leaks, inefficiencies, gesture collision issues, and producing a comprehensive technical handoff report.

## 🔒 My Identity
- Archetype: explorer
- Roles: Memory, Rendering & Gesture System Specialist
- Working directory: c:/Users/AJAYJKN/AndroidStudioProjects/AiPdfReaderEditor/.agents/survey_explorer_3
- Original parent: 9ae7d212-5a8f-4910-b2f9-a2343c3d9e0d
- Milestone: Survey & Investigation

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Structured handoff report in `.agents/survey_explorer_3/handoff.md`
- Communicate back to parent via `send_message`

## Current Parent
- Conversation ID: 9ae7d212-5a8f-4910-b2f9-a2343c3d9e0d
- Updated: 2026-08-23T04:22:15Z

## Investigation State
- **Explored paths**:
  - `core/pdf/PdfRendererPool.kt`
  - `core/pdf/PdfEngine.kt`
  - `core/pdf/PdfEdit.kt`
  - `ui/tools/grid/PdfGridComponents.kt`
  - `ui/tools/grid/DeletePagesScreen.kt` & `DeletePagesViewModel.kt`
  - `ui/tools/grid/ExtractPagesScreen.kt` & `ExtractPagesViewModel.kt`
  - `ui/tools/grid/SplitPdfScreen.kt` & `SplitPdfViewModel.kt`
  - `ui/viewer/PdfViewerScreen.kt` & `PdfViewerViewModel.kt`
  - `ui/tools/MergePdfScreen.kt` & `MergePdfViewModel.kt`
  - `ui/tools/PdfToolsScreen.kt` & `PdfToolsViewModel.kt`
  - `ui/home/HomeScreen.kt` & `HomeViewModel.kt`
  - `ui/chat/PdfChatScreen.kt` & `PdfChatViewModel.kt`
  - `ui/navigation/AppNavigation.kt`
  - `BaseApplication.kt` & `di/AppModule.kt`
  - `build.gradle.kts` & `libs.versions.toml`
- **Key findings**:
  - **R3 (Memory Safety & Lifecycle)**:
    1. Global static `globalPdfRendererPool` accumulates `PdfRenderer` and `ParcelFileDescriptor` instances with zero lifecycle disposal.
    2. Zero occurrences of `DisposableEffect` / `onDispose` in Compose screens and zero occurrences of `onCleared` in ViewModels.
    3. `LruCache` invokes `oldValue.recycle()` on eviction while Compose is actively rendering that Bitmap, causing `Canvas: trying to use a recycled bitmap` crashes.
    4. `PdfRendererPool` has no `close(uri)` or error-discard mechanism (broken renderers are returned to the pool).
    5. `imagesToPdf` decodes unconstrained bitmaps from `InputStream` without `inSampleSize`, vulnerable to OOM.
  - **R4 (Flicker & Gestures)**:
    1. Coil `AsyncImage` with in-memory Bitmaps causes asynchronous request dispatch and visual flashing on item selection recomposition. Direct Compose `Image(bitmap.asImageBitmap())` eliminates all flicker.
    2. Redundant empty `pointerInput` loops in `PdfThumbnailItem`.
    3. `detectTapGestures` in thumbnail items consumes touch events, preventing drag detection.
    4. Drag-to-select is completely missing in `PdfThumbnailGrid` and grid ViewModels despite parameter signatures.
- **Unexplored areas**: None. All memory, rendering, and gesture paths in the codebase have been thoroughly inspected.

## Key Decisions Made
- Structured complete technical recommendations and code blueprints for implementation agents covering `PdfRendererPool`, Compose lifecycle hooks, safe byte-counted LRU caching, direct Compose `Image` drawing, and a conflict-free gesture system.

## Artifact Index
- `.agents/survey_explorer_3/DISPATCH.md` — Initial task dispatch details
- `.agents/survey_explorer_3/BRIEFING.md` — Persistent working memory
- `.agents/survey_explorer_3/progress.md` — Liveness & progress tracking
- `.agents/survey_explorer_3/handoff.md` — Final structured handoff report
