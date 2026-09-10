---
name: android-engineering
description: The constitutional Android engineering rules for AiPdfReaderEditor. Defines mandatory technology stack, coding standards, and architectural invariants.
---

# Android Engineering Constitution: AiPdfReaderEditor

## 1. The Technology Stack
- **Language**: Kotlin 2.4.10
- **UI Framework**: 100% Jetpack Compose (BOM 2026.08.00) + Material 3 (1.4.0)
- **Architecture**: Multi-Module Clean Architecture + MVVM + Unidirectional Data Flow (UDF)
- **State Management**: `StateFlow<UiState>` as single source of truth; one-shot events via `Channel<UiEvent>`
- **Dependency Injection**: Dagger-Hilt 2.60+ (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`)
- **Database**: Room 3 (`androidx.room3` with `sqlite-bundled` driver, KSP)
- **Key-Value Persistence**: Jetpack DataStore Preferences
- **Async Runtime**: Kotlin Coroutines & Flow (`Dispatchers.IO`, `Dispatchers.Default`)
- **Navigation**: Type-Safe Navigation Compose with Kotlinx Serialization (`@Serializable`)
- **AI & Cloud**: Firebase Vertex AI (`com.google.firebase:firebase-ai`), ML Kit Scanner & OCR
- **PDF Engine**: Apache PDFBox Android (`com.tom-roush:pdfbox-android`), Android Native `PdfRendererPool` with `Semaphore(4)`

## 2. Constitutional Invariants
1. **Never use Koin or manual service locators**: All dependency injection MUST be handled via Dagger-Hilt.
2. **Never place business logic in Composables**: UI components only render `UiState` and emit `UiAction` callbacks to ViewModels.
3. **Never bypass Scoped Storage**: Use `MediaStore.Files` for single file exports and SAF `OpenDocumentTree` for batch exports. Zero raw `File` path APIs on Android 10+.
4. **Never blindly upgrade Gradle dependencies**: Diagnose incompatibilities first. Never upgrade AGP, Kotlin, KSP, or Compose BOM to make an error disappear.
5. **No God Composables**: Break screens into modular sub-composables. No single Composable function may exceed 150 lines.
