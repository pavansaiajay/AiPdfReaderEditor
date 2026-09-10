# 📜 MASTER ENGINEERING RULES: AiPdfReaderEditor

> **Status**: `ENFORCED CONSTITUTION`  
> **Applicability**: All Developers & Autonomous AI Agents working on this codebase.

---

# 1. CORE ARCHITECTURE CONSTITUTION

### The Stack
- **Language**: Kotlin 2.4.10
- **UI Framework**: 100% Jetpack Compose (BOM 2026.08.00) + Material 3 (1.4.0)
- **Architecture Pattern**: Multi-Module Clean Architecture + MVVM + Unidirectional Data Flow (UDF)
- **State Management**: `StateFlow<UiState>` with single source of truth; one-shot events via `Channel<UiEvent>`
- **Dependency Injection**: Dagger-Hilt 2.60+ (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Module`, `@InstallIn`)
- **Persistence**: Room 3 (`androidx.room3` with `sqlite-bundled` driver, KSP) & Jetpack DataStore Preferences
- **Async Runtime**: Kotlin Coroutines & Flow (`Dispatchers.IO`, `Dispatchers.Default`, `@IoDispatcher`)
- **Navigation**: Jetpack Navigation Compose 2.9.8 with Kotlinx Serialization (`@Serializable` type-safe routes)
- **AI & Cloud**: Firebase Vertex AI (`com.google.firebase:firebase-ai`), ML Kit Document Scanner & OCR
- **Image & PDF Pipeline**: Apache PDFBox Android (`com.tom-roush:pdfbox-android`), Android Native `PdfRendererPool` with `Semaphore(4)`, Coil 3

---

# 2. MULTI-MODULE DEPENDENCY RULES

1. **Features Never Depend on Features**:
   - `:feature:home` MUST NOT depend on `:feature:tools` or `:feature:viewer`.
   - All inter-feature communication and navigation MUST occur through type-safe routes defined in `:core:navigation`.
2. **Core Modules are Layered**:
   - `:core:ui` depends ONLY on `:core:designsystem` and `:core:common`.
   - `:core:model` is pure Kotlin; it has ZERO Android UI dependencies.
   - `:core:pdf` and `:core:ai` depend on `:core:model` and `:core:common`.
   - `:core:database` and `:core:datastore` depend on `:core:model`.
3. **Data Flows Downward**:
   - Composable UI -> ViewModel -> Domain UseCase (when justified) -> Repository -> DataSource.
   - Composables must NEVER directly call Repositories, Database DAOs, MediaStore, or Network APIs.

---

# 3. MONETIZATION & TOKEN-COST ECONOMY RULES

1. **The Free Utility Invariant**:
   - Offline PDF utilities (Merge, Split, Extract, Delete, Reorder, Rotate, Compress, Images to PDF, PDF to Images, Scan, Encrypt, Decrypt, Watermark, Flatten, HTML to PDF, Text Stripper) must **NEVER** check, gate, or deduct credits.
   - Calling `creditManager.creditsFlow.first()` or `deductCredits()` in offline ViewModels is a **build-rejection violation**.
2. **The Token-Cost AI Invariant**:
   - Paid AI operations (Chat, Summarize, OCR) must perform a pre-flight balance check (`credits >= 1`).
   - If balance is insufficient, immediately emit `InsufficientCredits` state and halt.
   - Calculate credit deduction dynamically based on `usageMetadata` (input + output tokens):
     $$\text{Cost} = \max\left(1, \left\lceil \frac{\text{Input Tokens} \times 1.0 + \text{Output Tokens} \times 3.0}{2,500} \right\rceil\right)$$
   - Deduct credits **strictly after** the API call succeeds. If the network call or generation fails, **ZERO credits must be deducted**.
3. **Blended Native List Ads**:
   - Do NOT place persistent bottom banner bars.
   - Ads must be styled as native cards inside recent document feeds or tool collections with a clear `[ Ad ]` badge.

---

# 4. MEMORY SAFETY & RENDERING RULES

1. **Native `PdfRenderer` Concurrency**:
   - Never instantiate raw `PdfRenderer` directly in a ViewModel or Composable.
   - All page rendering must route through `PdfRendererPool` guarded by `Semaphore(4)` and per-URI `Mutex`.
   - Always call `PdfRendererPool.closeUri(uri)` in `DisposableEffect.onDispose` or ViewModel `onCleared()`.
2. **Zero-Recycle Eviction in LRU Cache**:
   - In `PdfThumbnailCache.entryRemoved()`, do **NOT** invoke `oldValue.recycle()`. Let ART GC handle memory reclamation to prevent Compose Canvas render crashes.
3. **OOM Defense in Image Conversion**:
   - Always decode image bounds first with `inJustDecodeBounds = true`.
   - Use `calculateInSampleSize` to enforce $\le 2048 \times 2048$ dimensions and $\le 16\text{ MB}$ allocation.

---

# 5. STORAGE & SCOPED STORAGE RULES

1. **Zero Raw `File` APIs**:
   - Never use `java.io.File(path)` for saving output files to public storage on Android 10+ (API 29–37).
2. **Single-File MediaStore Protocol**:
   - Use `MediaStore.Files` targeting `Documents/AiPdfReaderEditor`.
   - Write using `IS_PENDING = 1`, buffer stream in 8 KB chunks, close, then set `IS_PENDING = 0`.
3. **Batch Output Protocol**:
   - Use SAF `OpenDocumentTree` for batch outputs (Split pages, PDF to Images). Prompt the user once for an export folder.
4. **Database History Sync**:
   - Every successfully exported file must be inserted into Room `DocumentDao` with timestamp descending.

---

# 6. CODE & COMPOSE HYGIENE

1. **No God Composables**:
   - Single Composable functions must NOT exceed 150 lines.
   - Break screens into distinct private sub-composables (e.g., `TopBar`, `ContentSection`, `ActionRow`, `PreviewDialog`).
2. **Strict Design System Adherence**:
   - Never use raw hex colors (e.g., `#00E5FF`, `#121212`) in feature Composables.
   - Use `MaterialTheme.colorScheme.*` or `AiPdfTheme` semantic tokens.
   - Never use arbitrary `13.dp`, `17.dp`, `23.dp` spacing. Use `Spacing.*` 4dp scale.
3. **Localization Compliance**:
   - Never hardcode user-facing strings in Composables.
   - Always use `stringResource(R.string.*)` or `UiText.StringResource`.

---

# 7. GRADLE & BUILD INTEGRITY

1. **No Blind Dependency Upgrades**:
   - Never bump Gradle, AGP, Kotlin, KSP, Compose BOM, or Hilt versions to "make an error disappear."
   - Always verify version compatibility matrix in `libs.versions.toml`.
2. **Verification Before Claiming Complete**:
   - Every change must pass `./gradlew assembleDebug` and unit tests before victory claims.

---

# END OF MASTER ENGINEERING RULES
