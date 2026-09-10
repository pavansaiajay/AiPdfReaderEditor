# Project: AiPdfReaderEditor Android Application

## Architecture
- **Framework & Language**: Android (Kotlin 2.4.10, AGP 9.3.1, compileSdk 37, minSdk 26, targetSdk 37)
- **UI & Presentation**: Jetpack Compose (BOM 2026.08.00, Material 3 1.4.0, Navigation Compose 2.9.8) with MVI (Model-View-Intent) architecture.
- **Dependency Injection**: Dagger-Hilt 2.60.1.
- **Persistence & Economy**: Room 3 (3.0.1 with Bundled SQLite driver), Jetpack DataStore Preferences (`CreditManager`).
- **AI & Document Processing**: Firebase Vertex AI (`gemini-3.7-flash` / `gemini-3.5-flash-lite`), ML Kit (Document Scanner, Text Recognition), PDFBox Android (2.0.27.0), Android Native `PdfRenderer`.
- **Storage Strategy**: Android Scoped Storage (SDK 29-37) via MediaStore (`Documents/AiPdfReaderEditor`) and Storage Access Framework (SAF `CreateDocument` & `OpenDocumentTree`).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Free Offline Merge | Merge multiple PDFs into single PDF with 0 credits checked/deducted | M1 | Survey |
| 2 | Free Offline Split | Split PDF pages into separate files with 0 credits checked/deducted | M1 | Survey |
| 3 | Free Offline Delete Pages | Delete selected pages from PDF with 0 credits checked/deducted | M1 | Survey |
| 4 | Free Offline Extract Pages | Extract selected pages to new PDF with 0 credits checked/deducted | M1 | Survey |
| 5 | Free Offline Compress | Compress PDF images with 0 credits checked/deducted | M1 | Survey |
| 6 | Free Offline Reorder Pages | Reorder page sequences with 0 credits checked/deducted | M1 | Survey |
| 7 | Free Offline Rotate PDF | Rotate PDF pages (90/180/270°) with 0 credits checked/deducted | M1 | Survey |
| 8 | Free Offline Images to PDF | Convert images to standardized A4 PDF with 0 credits | M1 | Survey |
| 9 | Free Offline PDF to Images | Render PDF pages to JPEG images with 0 credits | M1 | Survey |
| 10 | Free Offline Annotate/Draw | Save drawn/text annotations to PDF with 0 credits | M1 | Survey |
| 11 | Free Offline Document Scan | Hardware camera document scanner with 0 credits | M1 | Survey |
| 12 | Free Offline Encrypt PDF | Password protect PDF with 0 credits | M1 | Survey |
| 13 | Free Offline Decrypt PDF | Remove PDF password with 0 credits | M1 | Survey |
| 14 | Free Offline Watermark | Add text watermark with 0 credits | M1 | Survey |
| 15 | Free Offline Flatten PDF | Flatten AcroForm fields with 0 credits | M1 | Survey |
| 16 | Free Offline HTML to PDF | Convert HTML/URL to PDF with 0 credits | M1 | Survey |
| 17 | Free Offline Text Stripper | Extract text via PDFBox with 0 credits | M1 | Survey |
| 18 | Free Offline Text Search | In-document text search with 0 credits | M1 | Survey |
| 19 | Paid AI PDF Chat | Interactive Q&A (1 credit, InsufficientCredits paywall, deduct on success) | M1 | Survey |
| 20 | Paid AI PDF Summarize | Full document summary (5 credits, OutOfCredits dialog, deduct on success) | M1 | Survey |
| 21 | Paid AI OCR Text Recognition | Image OCR (1 credit, OutOfCredits dialog, deduct on success) | M1 | Survey |
| 22 | Scoped Storage Single Save | MediaStore/SAF save to Documents/AiPdfReaderEditor | M2 | Survey |
| 23 | Scoped Storage Batch Save | SAF directory tree selection (`OpenDocumentTree`) for batch outputs | M2 | Survey |
| 24 | Room DB Sync Single Output | Insert single saved PDF records into `DocumentDao` | M2 | Survey |
| 25 | Room DB Sync Batch Outputs | Insert all batch generated files into `DocumentDao` | M2 | Survey |
| 26 | Room DB Sync Annotations | Update/Insert annotated PDF records into `DocumentDao` | M2 | Survey |
| 27 | Scoped PdfRendererPool | Lifecycle-scoped `PdfRendererPool` with `closeUri(uri)` and healthy cleanup | M3 | Survey |
| 28 | Lifecycle UI Binding | Compose `DisposableEffect { onDispose }` and ViewModel `onCleared()` hooks | M3 | Survey |
| 29 | Safe Bitmap Cache | Byte-counted LRU memory cache without premature `oldValue.recycle()` crashes | M3 | Survey |
| 30 | OOM-Safe Image Decoding | Downsampled `inSampleSize` decoding in `imagesToPdf` | M3 | Survey |
| 31 | Flicker-Free Direct Compose Drawing | Direct `Image(bitmap.asImageBitmap())` in `PdfThumbnailItem` (0ms draw) | M4 | Survey |
| 32 | Long-Press Preview Overlay | Conflict-free full-screen high-res preview dialog | M4 | Survey |
| 33 | Drag-to-Select Gestures | `detectDragGesturesAfterLongPress` with layout hit-testing on grid | M4 | Survey |
| 34 | ViewModel Range Selection | `selectRange(start, end)`, `selectAll()`, `clearSelection()` in grid ViewModels | M4 | Survey |
| 35 | Full E2E Test Pass & Build | Clean `./gradlew assembleDebug` and 100% E2E test verification | M5 | Survey |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Monetization & Feature Gating (R1) | Separate free offline utilities from paid AI features; correct credit gating & deduction | none | DONE |
| M2 | Scoped Storage & Database Sync (R2) | Scoped Storage compliance (MediaStore & SAF batch tree); sync all outputs to Room `DocumentDao` | M1 | DONE |
| M3 | Memory Safety & Lifecycle Rendering (R3) | Lifecycle-bound `PdfRendererPool`, close streams, safe LRU cache, OOM prevention | none | DONE |
| M4 | Flicker-Free UI & Gesture Selection (R4) | Direct Compose image rendering, conflict-free tap/long-press/drag-select grid system | M3 | DONE |
| M5 | E2E Verification & Hardening | Full opaque-box E2E test suite execution, forensic audit, assembleDebug build | M1, M2, M3, M4 | DONE |

## Interface Contracts
### `CreditManager` ↔ ViewModels
- `creditsFlow: Flow<Int>`: Read-only balance stream.
- `deductCredits(amount: Int): Boolean`: Atomic check & deduct.
- Free tools must NEVER call `creditsFlow.first() < 1` or `deductCredits()`.
- AI Chat / OCR: Gated at `< 1`, deducts 1 on success.
- AI Summarize: Gated at `< 5`, deducts 5 on success.

### `PdfRendererPool` ↔ Grid UI
- `suspend fun renderPage(context: Context, uri: Uri, pageIndex: Int, targetWidth: Int = 400): Bitmap?`
- `suspend fun closeUri(uri: Uri)`: Closes and disposes all native renderers & PFDs for specific URI.
- `suspend fun closeAll()`: Closes all pooled renderers.

### `DocumentDao` ↔ Storage Exporters
- `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(document: DocumentEntity)`
- Every single save and batch save must record `DocumentEntity(fileName = name, uri = uriString, timestamp = System.currentTimeMillis())`.

## Code Layout
- `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/`
  - `core/economy/`: `CreditManager.kt`
  - `core/ai/`: `AiEngine.kt`
  - `core/pdf/`: `PdfEngine.kt`, `PdfRendererPool.kt`, `PdfEdit.kt`
  - `data/local/`: `DocumentEntity.kt`, `DocumentDao.kt`, `AppDatabase.kt`
  - `ui/tools/`: `PdfToolsScreen.kt`, `PdfToolsViewModel.kt`, `MergePdfScreen.kt`, `MergePdfViewModel.kt`
  - `ui/tools/grid/`: `PdfGridComponents.kt`, `DeletePagesScreen.kt`, `DeletePagesViewModel.kt`, `ExtractPagesScreen.kt`, `ExtractPagesViewModel.kt`, `SplitPdfScreen.kt`, `SplitPdfViewModel.kt`
  - `ui/viewer/`: `PdfViewerScreen.kt`, `PdfViewerViewModel.kt`
  - `ui/chat/`: `PdfChatScreen.kt`, `PdfChatViewModel.kt`
  - `ui/home/`: `HomeScreen.kt`, `HomeViewModel.kt`
