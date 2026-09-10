# Codebase Architecture & Overall Feature Survey Report

**Author**: Survey Explorer 1 (Codebase Architecture & Overall Feature Survey)  
**Target Date**: 2026-08-23  
**Working Directory**: `c:/Users/AJAYJKN/AndroidStudioProjects/AiPdfReaderEditor/.agents/survey_explorer_1_rep`  
**Parent**: Project Orchestrator (`9ae7d212-5a8f-4910-b2f9-a2343c3d9e0d`)

---

## 1. Observation

### 1.1 Project Structure & Build Configuration
- **Module Structure**: Single Android application module `:app` (Root `settings.gradle.kts` line 26: `include(":app")`).
- **Namespace & App ID**: `pavansaiajayx.aipdfreadereditor.app` (`app/build.gradle.kts` lines 13, 19).
- **SDK Targets**:
  - `compileSdk = 37` (release 37) (`app/build.gradle.kts` line 15)
  - `minSdk = 26` (`app/build.gradle.kts` line 20)
  - `targetSdk = 37` (`app/build.gradle.kts` line 21)
  - Java Version: `JavaVersion.VERSION_11` (`app/build.gradle.kts` lines 36-37)
- **Toolchain Versions (`gradle/libs.versions.toml`)**:
  - Android Gradle Plugin (AGP): `9.3.1`
  - Kotlin: `2.4.10` with Compose Compiler plugin (`org.jetbrains.kotlin.plugin.compose`) & Kotlin Serialization (`2.4.10`)
  - KSP: `2.3.10`
  - Dagger-Hilt: `2.60.1` (`hilt-android`, `hilt-compiler`, `hilt-navigation-compose: 1.4.0`)
  - Room 3: `3.0.1` (`androidx.room3:room3-runtime`, `androidx.room3:room3-compiler`, `androidx.sqlite:sqlite-bundled: 2.7.0`)
  - Compose BOM: `2026.08.00` (Material 3: `1.4.0`, Navigation Compose: `2.9.8`)
  - Firebase BOM: `34.17.0` (`firebase-ai`, `firebase-crashlytics`, `firebase-analytics`, `firebase-appcheck-debug`)
  - ML Kit: `play-services-mlkit-document-scanner: 16.0.0`, `play-services-mlkit-text-recognition: 19.0.1`
  - PDF Libraries: `com.tom-roush:pdfbox-android: 2.0.27.0`, `io.github.afreakyelf:Pdf-Viewer: 2.4.0`
  - Image Loading: `io.coil-kt.coil3:coil-compose: 3.5.0`
  - Ads & Economy: `play-services-ads: 25.4.0`, `play-review: 2.0.1`, `datastore-preferences: 1.2.1`

---

### 1.2 Architectural Patterns
1. **MVI (Model-View-Intent) & State Management**:
   - `PdfToolsViewModel` defines `PdfToolsState` (Sealed interface: `Idle`, `Processing`, `Success`, `Error`, `InsufficientCredits`) and `PdfToolsIntent` (Sealed interface representing user actions).
   - `MergePdfViewModel` defines `MergePdfIntent` and `MergePdfState` with `StateFlow` updates.
   - `PdfChatViewModel` and `PdfViewerViewModel` maintain reactive `StateFlow` and immutable state dataclasses.
2. **Dependency Injection (Hilt)**:
   - Application annotated with `@HiltAndroidApp` (`BaseApplication.kt`).
   - Singletons provided in `AppModule.kt`: `DataStore<Preferences>`, `CreditManager`, `FirebaseAnalytics`, `AnalyticsManager`, `AppDatabase` (Room 3 with `BundledSQLiteDriver`), and `DocumentDao`.
   - `AiEngine`, `PdfEngine`, `AdManager`, and `AppReviewManager` are injected with `@Singleton`.
3. **Jetpack Compose UI & Theming**:
   - Compose Navigation 2.9.8 with Type-Safe Kotlin Serialization routes (`RootSplashRoute`, `OnBoardingGraphRoute`, `MainGraphRoute`, `Screen.*` in `Routes.kt` & `Screen.kt`).
   - Dark theme enforcement via `AiPdfReaderEditorTheme` (`Theme.kt`, `Color.kt`, `Type.kt` using `Poppins` typography).
4. **Data & Persistence**:
   - Room 3 DB: `AppDatabase` (`data/local/AppDatabase.kt`) holding `DocumentEntity` (`documents` table).
   - Preference DataStore: `credits_store` managed by `CreditManager` (`core/economy/CreditManager.kt`).
5. **AI & Cloud Integration**:
   - Firebase Vertex AI (`com.google.firebase.ai.ai` in `AiEngine.kt`): Primary model `gemini-3.7-flash`, fallback model `gemini-3.5-flash-lite`.

---

### 1.3 Complete Inventory of Codebase Components

| Package / Area | Files | Key Classes / Interfaces | Responsibilities |
|---|---|---|---|
| **Root Application** | `BaseApplication.kt`<br>`MainActivity.kt` | `BaseApplication`<br>`MainActivity` | App init (Ads, PDFBox, Firebase, AppCheck). Single-activity host with splash screen and Edge-to-Edge. |
| **DI** | `di/AppModule.kt` | `AppModule` | Hilt bindings for DataStore, CreditManager, Room, Analytics. |
| **Core Economy** | `core/economy/CreditManager.kt` | `CreditManager` | Manages DataStore user credit balance, daily welcome bonus (5 credits), credit deduction, onboarding flag. |
| **Core AI** | `core/ai/AiEngine.kt` | `AiEngine` | Summarization with retry & fallback (`gemini-3.7-flash` -> `gemini-3.5-flash-lite`), multi-turn chat sessions. |
| **Core PDF Engine** | `core/pdf/PdfEngine.kt`<br>`core/pdf/PdfEdit.kt`<br>`core/pdf/PdfRendererPool.kt`<br>`android/print/PdfPrinter.kt` | `PdfEngine`<br>`PdfEdit`<br>`PdfRendererPool`<br>`PdfPrinter` | Low-level PDFBox & Android PDF operations: Merge, Split, Compress, Encrypt, Decrypt, Images<->PDF, Watermark, OCR, Delete/Reorder/Rotate/Extract pages, Flatten, HTML->PDF, Concurrent PDF rendering pool. |
| **Core Ads / Analytics** | `core/ads/AdManager.kt`<br>`core/ads/InlineBannerAd.kt`<br>`core/analytics/AnalyticsManager.kt`<br>`core/reviews/AppReviewManager.kt` | `AdManager`<br>`InlineBannerAd`<br>`AnalyticsManager`<br>`AppReviewManager` | Rewarded ads (+5 credits), AdMob Banner, Firebase Analytics events, Google Play in-app review flow. |
| **Data Layer** | `data/local/DocumentEntity.kt`<br>`data/local/DocumentDao.kt`<br>`data/local/AppDatabase.kt` | `DocumentEntity`<br>`DocumentDao`<br>`AppDatabase` | Room 3 database storing recent documents (`id`, `fileName`, `uri`, `timestamp`, `thumbnailPath`). |
| **Navigation** | `ui/navigation/Routes.kt`<br>`ui/navigation/Screen.kt`<br>`ui/navigation/AppNavigation.kt` | `Screen` sealed hierarchy<br>`AppNavigation` | Type-safe Navigation Compose routes: Splash, Onboarding, Home, Viewer, MultiFormat, Chat, Tools, Merge, Split, Extract, Delete. |
| **UI: Onboarding & Home** | `ui/onboarding/OnboardingScreen.kt`<br>`ui/home/HomeScreen.kt`<br>`ui/home/HomeViewModel.kt`<br>`core/preferences/PreferencesViewModel.kt` | `OnboardingScreen`<br>`HomeScreen`<br>`HomeViewModel`<br>`PreferencesViewModel` | 3-page onboarding pager, Home dashboard with credit balance, rewarded ad launcher, document picker, and recent files list. |
| **UI: Viewer & Chat** | `ui/viewer/PdfViewerScreen.kt`<br>`ui/viewer/PdfViewerViewModel.kt`<br>`ui/viewer/MultiFormatViewerScreen.kt`<br>`ui/chat/PdfChatScreen.kt`<br>`ui/chat/PdfChatViewModel.kt` | `PdfViewerScreen`<br>`PdfViewerViewModel`<br>`MultiFormatViewerScreen`<br>`PdfChatScreen`<br>`PdfChatViewModel` | PDF Viewer with drawing/highlighting/text/signature annotations, search, AI summary bottom sheet; WebView document reader; Gemini-powered interactive PDF chat. |
| **UI: PDF Tools (Classic & Grid)** | `ui/tools/PdfToolsScreen.kt`<br>`ui/tools/PdfToolsViewModel.kt`<br>`ui/tools/MergePdfScreen.kt`<br>`ui/tools/MergePdfViewModel.kt`<br>`ui/tools/grid/PdfThumbnailGrid`<br>`ui/tools/grid/DeletePagesScreen.kt`<br>`ui/tools/grid/DeletePagesViewModel.kt`<br>`ui/tools/grid/ExtractPagesScreen.kt`<br>`ui/tools/grid/ExtractPagesViewModel.kt`<br>`ui/tools/grid/SplitPdfScreen.kt`<br>`ui/tools/grid/SplitPdfViewModel.kt` | `PdfToolsScreen`<br>`PdfToolsViewModel`<br>`MergePdfScreen`<br>`MergePdfViewModel`<br>`DeletePagesScreen`<br>`ExtractPagesScreen`<br>`SplitPdfScreen`<br>`PdfThumbnailGrid` | 17+ PDF utility tools (Merge, Split, Compress, Encrypt, Decrypt, Watermark, OCR, Scan, etc.), Drag-and-drop Merge list, Interactive thumbnail grid for Delete/Extract/Split page selection. |

---

### 1.4 Detailed Mapping of Requirements (R1, R2, R3, R4) & Observed Violations

#### Requirement R1: Separate Free Offline Operations from Paid AI Features
- **Observed Violations**:
  1. `PdfToolsViewModel.kt` (lines 269–303): `executeWithCreditCheck(toolName)` was indiscriminately wrapped around 15+ offline utilities (`merge`, `split`, `compress`, `encrypt`, `decrypt`, `images_to_pdf`, `pdf_to_images`, `add_watermark`, `extract_text`, `delete_pages`, `reorder_pages`, `rotate_pdf`, `extract_page`, `flatten_pdf`, `html_to_pdf`). Each checks `credits < 1` and calls `creditManager.deductCredits(1)`.
  2. `PdfToolsViewModel.kt` (line 207): `handleScanDocumentCompleted` calls `creditManager.deductCredits(1)`. Document Scanning via ML Kit document scanner is an offline utility and must be 100% free.
  3. `PdfToolsScreen.kt` (lines 896–910): Scan document button had an inline check `if (credits >= 1)` before launching scanner intent.
  4. `PdfViewerViewModel.kt` (lines 85–97): `saveEdits()` (annotations/drawing/signatures) checks `credits < 1` and calls `creditManager.deductCredits(1)`. Annotation saving is an offline operation and must be 100% free.
  5. `MergePdfViewModel.kt` (line 131): `creditManager.deductCredits(1)` inside `mergePdfs`.
  6. `DeletePagesViewModel.kt` (lines 84–89, 120): Checks credits and deducts 1 credit.
  7. `ExtractPagesViewModel.kt` (lines 84–89, 120): Checks credits and deducts 1 credit.
  8. `SplitPdfViewModel.kt` (lines 86–90, 135): Checks credits and deducts 1 credit.
  9. `strings.xml` (line 25): `<string name="tool_cost_hint">Costs 1 Credit</string>` is shown across all offline tool cards in `PdfToolsScreen.kt` (lines 774, 784, 794, 804, 814, 824, 834, 844, 854, 864, 874, 884, 894, 919, 929, 939, 949).
- **Correct Paid AI Implementations**:
  - `PdfChatViewModel.kt` (lines 59–64, 74): Correctly checks `credits < 1`, halts on failure, and deducts 1 credit upon success.
  - `PdfViewerViewModel.kt` (lines 141–157): `summarizePdf` correctly checks `credits < 5` and deducts 5 credits upon success.
  - `PdfToolsViewModel.kt` (lines 170–202): `handleOcrImage` checks `credits < 1` and deducts 1 credit on success.

#### Requirement R2: Scoped Storage & Database Synchronization
- **Observations & Gaps**:
  1. `PdfToolsViewModel.kt` (lines 322–327): `handleSaveFiles` saves batch files via `pdfEngine.copyToFolder(sourceFiles, folderUri)` (SAF Tree URI), but FAILS to synchronize the created files to `DocumentDao`.
  2. `PdfToolsViewModel.kt` (lines 305–320): `handleSaveFile` correctly inserts into `DocumentDao` with filename, URI, and timestamp.
  3. `SplitPdfViewModel.kt` (lines 98–134): Saves split files directly into MediaStore (`DIRECTORY_DOCUMENTS + "/AiPdfReaderEditor"`) instead of delegating to a SAF directory tree picker (`ActivityResultContracts.OpenDocumentTree()`) for user-selected folder destination.
  4. `MergePdfViewModel.kt` (lines 107–139), `DeletePagesViewModel.kt` (lines 97–128), and `ExtractPagesViewModel.kt` (lines 97–128) properly use Scoped Storage MediaStore APIs (`DIRECTORY_DOCUMENTS + "/AiPdfReaderEditor"`, `IS_PENDING` flag) and insert into `DocumentDao`.
  5. `PdfEngine.kt` (lines 154–181): `copyToUri` and `copyToFolder` use `DocumentFile.fromTreeUri` and `contentResolver.openOutputStream`, adhering to Scoped Storage guidelines without raw file path assumptions.

#### Requirement R3: Memory Safety & Lifecycle-Bound PDF Rendering
- **Observations & Leaks**:
  1. `PdfGridComponents.kt` (line 43): Instantiates a global top-level singleton `val globalPdfRendererPool = PdfRendererPool()`.
  2. `PdfRendererPool.kt` (lines 20–73): Manages a map of `mutableMapOf<String, MutableList<PdfRenderer>>()`. When a renderer is created, `context.contentResolver.openFileDescriptor(uri, "r")` is called. `PdfRendererPool.closeAll()` closes all `PdfRenderer` instances (which closes underlying `ParcelFileDescriptor`s). However, `closeAll()` is NEVER called on navigation away, `onDispose`, or ViewModel `onCleared()`, leading to leaked file descriptors and memory when switching PDFs or screens.
  3. `thumbnailCache` (`PdfGridComponents.kt` line 35): Static `LruCache<String, Bitmap>(50)` only recycles on eviction, keeping up to 50 unmanaged bitmaps in memory across screens.
  4. `PdfEngine.kt` Bitmap & Stream Hygiene:
     - `compressPdf` (lines 134, 138): Explicitly recycles original and scaled bitmaps.
     - `imagesToPdf` (line 238): Explicitly recycles decoded bitmaps.
     - `pdfToImages` (line 262): Explicitly recycles rendered bitmaps.
     - `mergePdfs` (lines 73–80): Closes all `InputStream` handles in a `finally` block.

#### Requirement R4: Flicker-Free UI & Gesture Selection in Grids
- **Observations & Artifacts**:
  1. Image Rendering in Grid (`PdfGridComponents.kt` lines 173–178):
     ```kotlin
     AsyncImage(
         model = bitmap,
         contentDescription = "Page ${pageIndex + 1}",
         modifier = Modifier.fillMaxSize(),
         contentScale = ContentScale.Crop,
     )
     ```
     Passing an already decoded in-memory `Bitmap` into Coil's `AsyncImage` causes recomposition overhead and re-evaluation during state changes (such as selecting/unselecting pages), resulting in thumbnail flashing/flicker. Drawing directly with Compose `Image(bitmap = bitmap.asImageBitmap(), ...)` or optimized memory caching eliminates reload loops.
  2. Gesture Collisions & Missing Drag Selection (`PdfGridComponents.kt` lines 139–170):
     - `PdfThumbnailItem` stacks two separate `.pointerInput(Unit)` blocks: one with `awaitPointerEventScope` and one with `detectTapGestures`. `detectTapGestures` swallows touch events.
     - `PdfThumbnailGrid` defines `onDragSelectStart` and `onDragSelectUpdate` in its signature (lines 51–52), but NEVER binds them to any gesture detector on the `LazyVerticalGrid`!
     - `DeletePagesScreen`, `ExtractPagesScreen`, and `SplitPdfScreen` currently only support single-tap selection (`onPageSelected`) without drag-to-select range selection or smooth hold-to-preview integration.

---

## 2. Logic Chain

```
[Observation: 15+ offline tools in PdfToolsViewModel, MergePdfViewModel, DeletePagesViewModel, ExtractPagesViewModel, SplitPdfViewModel check & deduct credits]
       │
       ▼ (Deduction 1: Violation of R1)
[All basic utilities must be made 100% free by removing credit checks and deductCredits() calls from offline operations; keep credit checks ONLY for Chat, Summarize, OCR]
       │
       ├──────────────────────────────────────────────────────────────────────────┐
       ▼                                                                          ▼
[Observation: PdfToolsViewModel.handleSaveFiles does not sync to Room]   [Observation: SplitPdfViewModel saves to MediaStore, not user SAF folder]
       │                                                                          │
       ▼ (Deduction 2: Gaps in R2)                                                ▼ (Deduction 3: Gaps in R2)
[Batch saving must record each generated file in DocumentDao]            [Batch split/images should use SAF OpenDocumentTree and sync all files to DocumentDao]
       │                                                                          │
       └──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
[Observation: globalPdfRendererPool in PdfGridComponents is top-level unmanaged without lifecycle disposal]
       │
       ▼ (Deduction 4: Violation of R3)
[PdfRendererPool must be lifecycle-bound or closed in DisposableEffect/onCleared to prevent file descriptor & memory accumulation]
       │
       ▼
[Observation: PdfThumbnailItem uses Coil AsyncImage for in-memory Bitmap + unhooked drag-selection callbacks]
       │
       ▼ (Deduction 5: Violation of R4)
[Replace AsyncImage with direct Image(bitmap.asImageBitmap()) and implement conflict-free drag-to-select + hold-to-preview gesture modifier on LazyVerticalGrid]
```

---

## 3. Caveats
- **Physical Device / Emulator Execution**: This survey was performed purely via read-only static codebase inspection and structural analysis as instructed. Build and test execution (`./gradlew assembleDebug`) will be performed during verification phases.
- **Office Document Viewer**: `MultiFormatViewerScreen.kt` currently falls back to text/HTML rendering via `WebView` and displays an error for `.docx`/`.xlsx`/`.pptx` unless an external office renderer is configured.

---

## 4. Conclusion

The application architecture is well-structured using modern Android best practices (Jetpack Compose, MVI, Dagger-Hilt, Room 3 with bundled SQLite, DataStore, and Firebase Vertex AI). However, four clear implementation gaps must be resolved to meet all production requirements:

1. **R1 (Free vs Paid Separation)**: Strip credit checks and deduction logic from all offline utilities (`PdfToolsViewModel`, `MergePdfViewModel`, `DeletePagesViewModel`, `ExtractPagesViewModel`, `SplitPdfViewModel`, and `PdfViewerViewModel.saveEdits`), while strictly preserving credit gating for AI Chat, AI Summary, and OCR. Update tool cards UI hints in `strings.xml` and `PdfToolsScreen`.
2. **R2 (Scoped Storage & DB Sync)**: Ensure all single file saves (MediaStore/SAF) and batch saves (`copyToFolder` via SAF tree) write `DocumentEntity` records to `DocumentDao` to keep the Home recent files list perfectly synchronized.
3. **R3 (Memory Safety & Lifecycle PDF Rendering)**: Refactor `PdfRendererPool` to be lifecycle-scoped (e.g. managed per screen via `DisposableEffect(onDispose = { ... })` or ViewModel `onCleared()`) to guarantee `closeAll()` is called and file descriptors are freed.
4. **R4 (Flicker-Free Grid & Gestures)**: Replace Coil `AsyncImage` with direct `Image(bitmap.asImageBitmap())` in `PdfThumbnailItem`, and implement a unified conflict-free gesture system supporting tap-to-select, long-press preview, and drag-to-select across all grid screens.

---

## 5. Verification Method

To verify these findings independently:
1. **R1 Gating Inspection**:
   - Grep `creditManager.deductCredits` and `executeWithCreditCheck` across `app/src/main/java`.
   - Inspect `PdfToolsViewModel.kt` lines 107–303, `MergePdfViewModel.kt` line 131, `DeletePagesViewModel.kt` line 120, `ExtractPagesViewModel.kt` line 120, `SplitPdfViewModel.kt` line 135, and `PdfViewerViewModel.kt` line 97.
2. **R2 DB Sync Inspection**:
   - Inspect `PdfToolsViewModel.kt` line 322 (`handleSaveFiles` lacks `documentDao.insert`).
   - Inspect `HomeScreen.kt` line 75 (`viewModel.recentFiles`).
3. **R3 Lifecycle Inspection**:
   - Inspect `PdfGridComponents.kt` line 43 (`globalPdfRendererPool` global definition) and verify lack of `closeAll()` invocations in `DisposableEffect` or `onCleared`.
4. **R4 UI & Gesture Inspection**:
   - Inspect `PdfGridComponents.kt` line 173 (`AsyncImage` on in-memory bitmap) and lines 51, 139–170 (`onDragSelectStart`/`Update` unused, conflicting `pointerInput`).
5. **Compilation Verification**:
   - Run `./gradlew assembleDebug` to confirm overall build health.
