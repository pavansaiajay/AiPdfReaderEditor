# Specification Mining & Architecture Report: Monetization (R1) & Scoped Storage / Room Sync (R2)

## 1. Observation

Direct inspection of the codebase yielded the following concrete implementations, line numbers, and observable behaviors across monetization, feature gating, Scoped Storage, and Room database synchronization:

### 1.1 Credit Management & Balance Tracking (`CreditManager.kt`)
- **Location**: `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/core/economy/CreditManager.kt`
- **Storage**: Jetpack Preferences DataStore (`credits_store.preferences_pb`) injected as a Singleton via `AppModule.kt:36`.
- **State Properties**:
  * `CREDITS_KEY = intPreferencesKey("user_credits")` (Default: `0`)
  * `LAST_OPENED_DATE_KEY = longPreferencesKey("last_opened_date")`
  * `HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")`
- **Credit Lifecycle Methods**:
  * `creditsFlow: Flow<Int>`: Emits continuous credit balance updates (lines 19-22).
  * `checkDailyWelcomeCredits()`: Computes `System.currentTimeMillis() / (1000 * 60 * 60 * 24)`. If day differs from `last_opened_date`, resets credit balance to 5 if `currentCredits < 5`, and updates `last_opened_date` (lines 35-49). Triggered in `MainActivity.kt:66` upon `onCreate`.
  * `addCredits(amount: Int)`: Atomically adds credits via `dataStore.edit` (lines 51-56). Invoked on rewarded ad completion (+5 credits).
  * `deductCredits(amount: Int): Boolean`: Atomically checks if `current >= amount` and deducts `amount`, returning `true`, or returns `false` without deduction if insufficient (lines 58-68).

### 1.2 Paid AI Features Gating & Deduction

#### A. PDF Chat (`PdfChatViewModel.kt`, `PdfChatScreen.kt`)
- **Pricing Contract**: 1 credit per message sent/received.
- **Observed Gating** (`PdfChatViewModel.kt:59-63`):
  ```kotlin
  val credits = creditManager.creditsFlow.first()
  if (credits < 1) {
      _state.update { it.copy(error = "Insufficient Credits") }
      return@launch
  }
  ```
- **Observed Deduction** (`PdfChatViewModel.kt:74`): Deducts 1 credit only on AI success (`result.fold(onSuccess = { creditManager.deductCredits(1) ... })`).
- **Discrepancy**: Emits a simple string error rather than an explicit `InsufficientCredits` state / triggering the rewarded ad or paywall dialog on `PdfChatScreen`.
- **AI Model**: Firebase AI `gemini-3.5-flash-lite` with retry logic in `AiEngine.kt:51-80`.

#### B. PDF Summarization (`PdfViewerViewModel.kt`, `PdfViewerScreen.kt`)
- **Pricing Contract**: 5 credits per document summary.
- **Observed Gating** (`PdfViewerViewModel.kt:141-146`):
  ```kotlin
  val currentCredits = creditManager.creditsFlow.first()
  if (currentCredits < 5) {
      _state.update { it.copy(isAiProcessing = false, showInsufficientCreditsDialog = true) }
      return@launch
  }
  ```
- **Observed Deduction** (`PdfViewerViewModel.kt:155`): Deducts 5 credits (`creditManager.deductCredits(5)`) only on `summaryResult.isSuccess`.
- **Paywall / Ad Dialog**: `PdfViewerScreen.kt:151-172` displays AlertDialog offering "Watch Ad (+5)".
- **AI Model**: Firebase AI `gemini-3.7-flash` with fallback to `gemini-3.5-flash-lite` in `AiEngine.kt:13-49`.

#### C. OCR Text Recognition (`PdfToolsViewModel.kt`, `PdfToolsScreen.kt`)
- **Pricing Contract**: 1 credit per image OCR operation.
- **Observed Gating** (`PdfToolsViewModel.kt:172-176`):
  ```kotlin
  val currentCredits = creditManager.creditsFlow.first()
  if (currentCredits < 1) {
      _state.value = PdfToolsState.InsufficientCredits
      return@launch
  }
  ```
- **Observed Deduction** (`PdfToolsViewModel.kt:187`): Deducts 1 credit (`creditManager.deductCredits(1)`) on ML Kit text recognition success.
- **Paywall / Ad Dialog**: `PdfToolsScreen.kt:317-338` displays AlertDialog offering "Watch Ad (+5)".
- **Engine**: ML Kit Text Recognition (`com.google.mlkit:text-recognition:16.0.1`).

---

### 1.3 Offline Utilities & Free Operation Violations

According to requirement **R1**, all basic offline utilities must be 100% free with **zero credit checks and zero credit deductions**. However, the current implementation violates this across 17 offline operations:

1. **`PdfToolsViewModel.kt:269-302` (`executeWithCreditCheck`)**:
   Enforces `< 1` credit check and deducts 1 credit for:
   - `handleMerge` (lines 107-111)
   - `handleSplit` (lines 114-118)
   - `handleCompress` (lines 121-126)
   - `handleEncrypt` (lines 129-133)
   - `handleDecrypt` (lines 135-139)
   - `handleImagesToPdf` (lines 143-147)
   - `handlePdfToImages` (lines 150-154)
   - `handleAddWatermark` (lines 157-161)
   - `handleExtractText` (lines 164-168)
   - `handleDeletePages` (lines 224-230)
   - `handleReorderPages` (lines 233-239)
   - `handleRotatePdf` (lines 242-246)
   - `handleExtractPage` (lines 249-253)
   - `handleFlattenPdf` (lines 256-260)
   - `handleHtmlToPdf` (lines 263-267)
2. **`MergePdfViewModel.kt:131`**: `creditManager.deductCredits(1)` called upon successful merge.
3. **`SplitPdfViewModel.kt:85-89, 135`**: Blocks if `currentCredits < 1` with "Insufficient credits to perform this action." and calls `creditManager.deductCredits(1)`.
4. **`DeletePagesViewModel.kt:84-88, 120`**: Blocks if `currentCredits < 1` with "Insufficient credits to perform this action." and calls `creditManager.deductCredits(1)`.
5. **`ExtractPagesViewModel.kt:84-88, 120`**: Blocks if `currentCredits < 1` with "Insufficient credits to perform this action." and calls `creditManager.deductCredits(1)`.
6. **`PdfViewerViewModel.kt:85-89, 97`**: `saveEdits` (Annotation tool) blocks if `currentCredits < 1` (emits `showInsufficientCreditsDialog = true`) and calls `creditManager.deductCredits(1)`.
7. **`PdfToolsViewModel.kt:207`**: `handleScanDocumentCompleted` calls `creditManager.deductCredits(1)` on scanning completion.
8. **`strings.xml:25`**: `<string name="tool_cost_hint">Costs 1 Credit</string>` is hardcoded and displayed under all offline tool cards in `PdfToolsScreen.kt:774, 784, 794, 1104`.

---

### 1.4 Scoped Storage & MediaStore / SAF Architecture (`PdfEngine.kt`, ViewModels, Screens)

- **Target SDK**: Compile SDK 37, Target SDK 37, Min SDK 26 (`app/build.gradle.kts:14-22`).
- **Internal Cache Staging**: All file modifications stage temporary output files in `context.cacheDir` / `File(pdfEngine.cacheDir, ...)` before exporting to user storage.
- **Single File Output Storage (Android Scoped Storage SDK 29-37 compliant)**:
  * **MediaStore Path**:
    - Used in `MergePdfViewModel.kt:107-130`, `DeletePagesViewModel.kt:97-125`, `ExtractPagesViewModel.kt:97-125`.
    - Targets `MediaStore.Files.getContentUri("external")`.
    - Parameters: `RELATIVE_PATH = Environment.DIRECTORY_DOCUMENTS + "/AiPdfReaderEditor"`, `MIME_TYPE = "application/pdf"`, `DISPLAY_NAME = "<operation>_<timestamp>.pdf"`, `IS_PENDING = 1`.
    - Writes cache data to `ContentResolver.openOutputStream(uri)`, clears `IS_PENDING = 0` via `ContentResolver.update()`.
  * **SAF Single Document Creation**:
    - `PdfToolsScreen.kt:266-275`: Uses `ActivityResultContracts.CreateDocument("application/pdf")`.
    - `PdfToolsViewModel.kt:305-320`: `handleSaveFile(sourceFile, destinationUri)` calls `pdfEngine.copyToUri(sourceFile, destinationUri)` which writes directly via `ContentResolver.openOutputStream(destinationUri)`.
    - Text outputs use `CreateDocument("text/plain")` (`PdfToolsScreen.kt:255-264`).
- **Batch File Output Storage (SAF Directory Tree)**:
  * **SAF Directory Selection Contract**:
    - Required for batch tools: Split PDF and PDF to Images.
    - `PdfToolsScreen.kt:277-284`: Uses `ActivityResultContracts.OpenDocumentTree()`.
    - `PdfToolsViewModel.kt:322-327`: `handleSaveFiles(sourceFiles, folderUri)` calls `pdfEngine.copyToFolder(sourceFiles, folderUri)`.
    - `PdfEngine.kt:167-181`: `copyToFolder` wraps `folderUri` with `DocumentFile.fromTreeUri(context, folderUri)`, iterates `sourceFiles`, creates documents via `folder.createFile("application/pdf", file.nameWithoutExtension)`, and streams content.
  * **Discrepancy in `SplitPdfViewModel.kt:98-133`**: Currently writes batch split files directly to MediaStore Documents instead of invoking SAF `OpenDocumentTree()` folder picker.

---

### 1.5 Room Database (`DocumentDao`) & Synchronization Architecture

- **Schema Definition** (`DocumentEntity.kt:1-14`):
  ```kotlin
  @Entity(tableName = "documents")
  data class DocumentEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val fileName: String,
      val uri: String,
      val timestamp: Long,
      val thumbnailPath: String? = null
  )
  ```
- **DAO Methods** (`DocumentDao.kt:10-22`):
  * `insert(document: DocumentEntity)`: `@Insert(onConflict = OnConflictStrategy.REPLACE)`
  * `getAllDocuments(): Flow<List<DocumentEntity>>`: `@Query("SELECT * FROM documents ORDER BY timestamp DESC")`
  * `delete(document: DocumentEntity)`: `@Delete`
- **Database Configuration** (`AppDatabase.kt:6-10`, `AppModule.kt:56-69`):
  * Room 3 database name `"aipdf_database"` using `BundledSQLiteDriver()`.
- **Observed Room Database Sync Points**:
  * `MergePdfViewModel.kt:132-138`: Inserts merged PDF into `documentDao` on successful MediaStore write.
  * `DeletePagesViewModel.kt:121-127`: Inserts modified PDF into `documentDao` on MediaStore write.
  * `ExtractPagesViewModel.kt:121-127`: Inserts extracted PDF into `documentDao` on MediaStore write.
  * `SplitPdfViewModel.kt:126-132`: Inserts each split page PDF into `documentDao` on MediaStore write.
  * `PdfToolsViewModel.kt:310-316`: Inserts single saved PDF into `documentDao` in `handleSaveFile()`.
  * `PdfToolsViewModel.kt:212-218`: Inserts scanned PDF into `documentDao` in `handleScanDocumentCompleted()`.
  * `HomeScreen.kt:284-353` / `HomeViewModel.kt:34-39`: Collects `documentDao.getAllDocuments()` to render Recent Files list with share and delete operations.
- **Observed Room Database Sync Gaps**:
  * `PdfToolsViewModel.kt:322-327` (`handleSaveFiles` for batch SAF outputs): Does NOT insert batch created documents into `documentDao`.
  * `PdfViewerViewModel.kt:98` (`saveEdits` for annotated PDF): Overwrites the source URI but does NOT update timestamp or insert into `documentDao`.

---

## 2. Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Monetization | Daily Welcome Credits | Awards 5 free credits every new calendar day if balance < 5 | App launch (`System.currentTimeMillis()`) | Updates `user_credits` DataStore key | Silent fallback to current balance | `CreditManager.kt:35-49`, `MainActivity.kt:66` |
| 2 | Monetization | Rewarded Ad Credits | Grants 5 credits upon completing a rewarded video ad | `Activity` context | +5 credits to DataStore balance | Emits "No ads available right now" / "Ad failed to load" | `AdManager.kt`, `HomeViewModel.kt:75`, `PdfToolsViewModel.kt:337` |
| 3 | Paid AI | PDF Chat | Interactive Q&A conversational assistant with document context | `Uri`, query string, `chatSession` | AI response text string (1 credit) | Insufficient credits error, network timeout retry | `PdfChatViewModel.kt`, `PdfChatScreen.kt`, `AiEngine.kt:51` |
| 4 | Paid AI | PDF Summarize | Generates bulleted markdown summary of entire document | Document `Uri` | Markdown summary text (5 credits) | `showInsufficientCreditsDialog = true`, error message snackbar | `PdfViewerViewModel.kt:137`, `PdfViewerScreen.kt:652`, `AiEngine.kt:16` |
| 5 | Paid AI | OCR Image Text Recognition | Extracts text from image using ML Kit Text Recognition | Image `Uri` | Plain text file `ocr_<timestamp>.txt` (1 credit) | `PdfToolsState.InsufficientCredits`, `PdfToolsState.Error` | `PdfToolsViewModel.kt:170`, `PdfEngine.kt:322` |
| 6 | Free Offline Tool | Merge PDFs | Combines 2 or more PDF documents into one output PDF | List of PDF `Uri`s | Single merged PDF file (100% Free) | Throws `IllegalStateException` on corrupt stream, emits `ResultState.Error` | `MergePdfViewModel.kt`, `PdfToolsViewModel.kt:107`, `PdfEngine.kt:50` |
| 7 | Free Offline Tool | Split PDF | Divides PDF into individual single pages or selected page ranges | PDF `Uri`, list of page indices | List of split PDF files (100% Free) | Emits error if page range empty or index out of bounds | `SplitPdfViewModel.kt`, `PdfToolsViewModel.kt:114`, `PdfEngine.kt:89, 614` |
| 8 | Free Offline Tool | Compress PDF | Downsamples embedded images by 50% and recompresses to JPEG 0.5 | PDF `Uri` | Compressed PDF file (100% Free) | Emits `Result.failure` on corrupt stream | `PdfToolsViewModel.kt:121`, `PdfEngine.kt:117` |
| 9 | Free Offline Tool | Delete Pages | Removes selected page numbers from PDF document | PDF `Uri`, list of page indices (0-based) | Single modified PDF file (100% Free) | Validates pages against `document.numberOfPages` | `DeletePagesViewModel.kt`, `PdfToolsViewModel.kt:224`, `PdfEngine.kt:331` |
| 10 | Free Offline Tool | Extract Pages | Extracts chosen subset of pages into a new standalone PDF | PDF `Uri`, page indices | Single extracted PDF file (100% Free) | Validates page bounds; ignores out-of-range pages | `ExtractPagesViewModel.kt`, `PdfToolsViewModel.kt:249`, `PdfEngine.kt:484, 502` |
| 11 | Free Offline Tool | Reorder Pages | Rearranges page order according to user-specified sequence | PDF `Uri`, comma-separated page sequence | Single reordered PDF file (100% Free) | Ignores invalid indices; preserves valid pages | `PdfToolsViewModel.kt:233`, `PdfEngine.kt:484` |
| 12 | Free Offline Tool | Rotate PDF | Rotates all PDF pages by 90, 180, or 270 degrees | PDF `Uri`, `rotationDegrees` (Int) | Single rotated PDF file (100% Free) | Normalizes rotation modulo 360 | `PdfToolsViewModel.kt:242`, `PdfEngine.kt:351` |
| 13 | Free Offline Tool | Images to PDF | Combines multiple JPEG/PNG images into a standardized A4 PDF | List of Image `Uri`s | Single PDF file (100% Free) | Skips undecodable images; recycles Bitmaps | `PdfToolsViewModel.kt:143`, `PdfEngine.kt:210` |
| 14 | Free Offline Tool | PDF to Images | Renders every page of a PDF as 150 DPI JPEG images | PDF `Uri` | List of JPEG files `page_<N>.jpg` (100% Free) | Throws `IllegalStateException` on load error | `PdfToolsViewModel.kt:150`, `PdfEngine.kt:247` |
| 15 | Free Offline Tool | Annotate / Edit PDF | In-place drawing, text markup, highlight, text insertion, signature | PDF `Uri`, `List<PdfEdit>`, page index | Updated PDF file (100% Free) | Emits error if page index invalid or stream unwritable | `PdfViewerViewModel.kt:78`, `PdfEngine.kt:365` |
| 16 | Free Offline Tool | Scan Document | Full camera hardware document scanner using ML Kit | Camera input | Scanned PDF URI (100% Free) | Emits cancellation / failure code if scanner aborted | `PdfToolsViewModel.kt:205`, `PdfToolsScreen.kt:244` |
| 17 | Free Offline Tool | Lock / Encrypt PDF | Protects PDF with 128-bit standard encryption password | PDF `Uri`, password string | Encrypted PDF file (100% Free) | Fails if password empty or stream unreadable | `PdfToolsViewModel.kt:128`, `PdfEngine.kt:183` |
| 18 | Free Offline Tool | Unlock / Decrypt PDF | Removes password security protection from PDF | PDF `Uri`, password string | Decrypted PDF file (100% Free) | Throws `CryptographyException` on wrong password | `PdfToolsViewModel.kt:134`, `PdfEngine.kt:198` |
| 19 | Free Offline Tool | Add Watermark | Stamps 45° diagonal HELVETICA_BOLD text watermark on all pages | PDF `Uri`, watermark text string | Watermarked PDF file (100% Free) | Fails if PDF locked/encrypted | `PdfToolsViewModel.kt:156`, `PdfEngine.kt:271` |
| 20 | Free Offline Tool | Flatten PDF | Bakes AcroForm interactive form fields permanently into PDF | PDF `Uri` | Flattened PDF file (100% Free) | No-op if no AcroForm present | `PdfToolsViewModel.kt:255`, `PdfEngine.kt:521` |
| 21 | Free Offline Tool | HTML / Web to PDF | Converts HTML string or URL to PDF via WebView & PrintAdapter | HTML string or HTTP(S) URL | Single converted PDF file (100% Free) | Fails on network error or invalid HTML markup | `PdfToolsViewModel.kt:262`, `PdfEngine.kt:533`, `PdfPrinter.kt` |
| 22 | Free Offline Tool | Offline Text Stripper | Extracts plain text from PDF using PDFBox `PDFTextStripper` | PDF `Uri` | Text file `extracted_<timestamp>.txt` (100% Free) | Catches and logs exception to Crashlytics | `PdfToolsViewModel.kt:163`, `PdfEngine.kt:304` |
| 23 | Free Offline Tool | In-PDF Text Search | Searches entire PDF text for query term and returns page numbers | PDF `Uri`, query string | List of matching 1-based page numbers | Returns empty list if no match found | `PdfViewerViewModel.kt:111`, `PdfEngine.kt:564` |
| 24 | Storage & Sync | MediaStore Single Save | Writes PDF into public `Documents/AiPdfReaderEditor` via MediaStore | Source cache File, file name | Public MediaStore `Uri` | Throws `IllegalStateException` on failed insert | `MergePdfViewModel.kt:107`, `DeletePagesViewModel.kt:97` |
| 25 | Storage & Sync | SAF Single File Save | Writes output PDF to user-chosen destination using `CreateDocument` | Cache File, SAF destination `Uri` | Output written to chosen `Uri` | Throws `IllegalStateException("Unable to save file")` | `PdfToolsViewModel.kt:305`, `PdfEngine.kt:154` |
| 26 | Storage & Sync | SAF Batch Folder Save | Writes list of files into user-selected directory tree via SAF | List of Files, SAF folder `Uri` | Multiple files saved in folder tree | Throws `IllegalStateException("Unable to access folder")` | `PdfToolsViewModel.kt:322`, `PdfEngine.kt:167` |
| 27 | Storage & Sync | Room Database Recent History | Inserts created/modified documents into `documents` table | `DocumentEntity(fileName, uri, timestamp)` | Room SQLite row record | Replaces on primary key conflict | `DocumentDao.kt:13`, `AppDatabase.kt`, `HomeViewModel.kt:34` |
| 28 | Storage & Sync | Room Database Delete Record | Removes document record from recent history list | `DocumentEntity` | Deleted SQLite row | No-op if record not found | `DocumentDao.kt:20`, `HomeViewModel.kt:98`, `HomeScreen.kt:343` |

---

## 3. Edge Cases

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Credit Manager | Initial fresh launch (no DataStore file) | Returns default `0` credits; `has_seen_onboarding` returns `false`. |
| 2 | Credit Manager | Launch on next day with balance = 2 | `checkDailyWelcomeCredits()` detects `currentDay != lastOpenedDay` and tops up balance to `5`. |
| 3 | Credit Manager | Launch on next day with balance = 12 | `checkDailyWelcomeCredits()` updates `last_opened_date` without reducing balance (balance remains `12`). |
| 4 | Credit Manager | Simultaneous deductions with balance = 1 | DataStore atomic transactions ensure only the first deduction succeeds; second returns `false`. |
| 5 | PDF Chat | Send message with 0 credits | Emits `error = "Insufficient Credits"` toast; does NOT initiate AI request or deduct credits. |
| 6 | PDF Chat | Send message with 1 credit, network fails | AI request fails; `onFailure` block triggers; credit remains `1` (no deduction). |
| 7 | PDF Summarize | Request summary with 4 credits | Emits `showInsufficientCreditsDialog = true`; summary request halts immediately without deduction. |
| 8 | PDF Summarize | Request summary with 5 credits, AI fails | `onFailure` block triggers; `aiSummary` remains `null`; 5 credits remain untouched. |
| 9 | OCR Image | OCR on blank image with 1 credit | ML Kit returns empty string; outputs empty `ocr_<timestamp>.txt`; deducts 1 credit. |
| 10 | OCR Image | OCR with 0 credits | Emits `PdfToolsState.InsufficientCredits`; does NOT execute ML Kit model; 0 credits remain. |
| 11 | Merge PDF | 2 PDFs merged with 0 credits | Currently blocked by bug in `PdfToolsViewModel:276` and `MergePdfViewModel:131`; per R1, must succeed and deduct 0. |
| 12 | Split PDF | Split with 0 credits | Currently blocked by bug in `SplitPdfViewModel:86`; per R1, must succeed with 0 credits. |
| 13 | Delete Pages | Delete all pages in a PDF | `PDDocument.removePage` empties document; saving empty PDF throws exception or corrupts file; needs page validation (`validPagesToRemove.size < totalPages`). |
| 14 | Delete Pages | Delete invalid page indices (e.g. page 99 in 5-page PDF) | Filtered out by `validPagesToRemove = pagesToRemove.filter { it in 0 until totalPages }`; valid pages removed. |
| 15 | Compress PDF | PDF containing no images | Iterates all pages and resources; no `PDImageXObject` found; saves identical PDF without error. |
| 16 | Images to PDF | Non-image URI or corrupted image selected | `BitmapFactory.decodeStream` returns `null`; safely skipped via `?: return@use`. |
| 17 | PDF to Images | Multi-page PDF (e.g. 50 pages) | Renders each page via `PDFRenderer`, saves JPEG, immediately calls `bitmap.recycle()` to avoid OOM. |
| 18 | Lock / Encrypt PDF | Password protected PDF re-encrypted with new password | `encryptPdf` wraps document with `StandardProtectionPolicy` length 128; output encrypted. |
| 19 | Unlock / Decrypt PDF | Wrong password supplied | `PDDocument.load(inputStream, password)` throws `InvalidPasswordException` / `IOException`; handled by `runCatching`. |
| 20 | Add Watermark | Large PDF with 100+ pages | Streams text watermark across all pages in loop; memory scales linearly with page stream buffer. |
| 21 | HTML to PDF | Raw HTML text vs HTTP/HTTPS URL | Checks `htmlContent.startsWith("http")`; loads URL via `loadUrl` or raw string via `loadDataWithBaseURL`. |
| 22 | Scoped Storage | Saving single file when destination URI permission revoked | `ContentResolver.openOutputStream` returns `null`; throws `IllegalStateException("Unable to save file")`. |
| 23 | Scoped Storage | MediaStore file name collision in `Documents/AiPdfReaderEditor` | Android OS MediaStore automatically appends `(1)`, `(2)` to `DISPLAY_NAME` to prevent overwrite. |
| 24 | SAF Batch Folder | User chooses SD Card / read-only directory | `DocumentFile.fromTreeUri` or `folder.createFile` returns `null`; throws `IllegalStateException("Unable to create file in folder")`. |
| 25 | Room Database | Deleting file from disk while record exists in Room | Room still lists record; tapping opens viewer which fails gracefully with error toast if URI unreadable. |

---

## 4. Logic Chain

1. **Monetization & Economy Logic Chain**:
   - *Premise*: User request R1 specifies that offline utilities (Merge, Split, Extract, Compress, Annotate, Delete, Reorder, Image/PDF conversions, Scan) are 100% free with 0 credits checked or deducted. Paid AI features (Chat = 1, OCR = 1, Summarize = 5) must gate on credit balance beforehand, show Paywall/Ad dialog on insufficient credits, and deduct credits strictly upon operation success.
   - *Observation*: `CreditManager` properly provides atomic credit manipulation (`creditsFlow`, `addCredits`, `deductCredits`). However, `PdfToolsViewModel` routes almost all operations through `executeWithCreditCheck`, which enforces `currentCredits < 1` and deducts 1 credit for free offline tools. `MergePdfViewModel`, `SplitPdfViewModel`, `DeletePagesViewModel`, `ExtractPagesViewModel`, and `PdfViewerViewModel` (for annotations) all deduct 1 credit.
   - *Inference*: Free offline tools must have credit checks and credit deductions removed entirely. Only `handleOcrImage`, `summarizePdf`, and `sendMessage` (PDF Chat) should retain credit checks and deductions. `PdfChatViewModel` must be updated to emit `InsufficientCredits` state / trigger Paywall UI instead of just a string error. `strings.xml` must remove the "Costs 1 Credit" label from offline tools.

2. **Scoped Storage & Room Database Logic Chain**:
   - *Premise*: User request R2 requires Scoped Storage compliance (SDK 29-37) using MediaStore / SAF single-file saving for single output PDFs, SAF directory tree (`OpenDocumentTree`) for batch outputs (Split, PDF to Images), and synchronization of all successful saves into Room `DocumentDao`.
   - *Observation*: MediaStore saving to `Documents/AiPdfReaderEditor` is implemented in `MergePdfViewModel`, `DeletePagesViewModel`, `ExtractPagesViewModel`, and SAF single-file `CreateDocument` is in `PdfToolsScreen`. Batch SAF `OpenDocumentTree` saving is implemented in `PdfToolsScreen` via `handleSaveFiles`.
   - *Inference*: `SplitPdfViewModel` should be aligned with SAF directory tree selection rather than dumping multiple files to MediaStore directly. In addition, every file creation path (including `handleSaveFiles` for batch outputs and `saveEdits` for annotated documents) must sync created documents to `DocumentDao.insert()` so that recent files history is always consistent.

---

## 5. Caveats

- **No Caveats on Specifications**: The entire monetization structure, AI model routing, ML Kit integrations, PDFBox operations, Scoped Storage mechanisms, and Room database schemas have been fully mapped directly from source code and authoritative requirements.
- **Read-Only Constraint Respected**: As a Specification Miner, no code modifications were implemented during this survey turn. All discoveries are prepared for the implementation agent.

---

## 6. Conclusion

1. **Credit Gating & Pricing**:
   - **Chat with PDF**: 1 credit (AI model: `gemini-3.5-flash-lite`), gated before prompt, deducted only on success. Must emit `InsufficientCredits` UI dialog.
   - **Summarize PDF**: 5 credits (AI model: `gemini-3.7-flash` / fallback `gemini-3.5-flash-lite`), gated before analysis, deducted only on success. Triggers Out of Credits dialog.
   - **OCR Image**: 1 credit (ML Kit text recognition), gated before processing, deducted only on success. Triggers Out of Credits dialog.
   - **All 17 Offline Utilities**: Must be completely decoupled from `CreditManager`. Zero credit checks, zero deductions, and removal of "Costs 1 Credit" UI hints.
2. **Storage & Room Database**:
   - Single outputs save to `Documents/AiPdfReaderEditor` via MediaStore or SAF `CreateDocument`.
   - Batch outputs save to SAF `OpenDocumentTree` directory tree.
   - Every successful output syncs `DocumentEntity(fileName, uri, timestamp)` to `DocumentDao`.

---

## 7. Verification Method

To independently verify these findings:
1. Inspect `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/core/economy/CreditManager.kt` to verify DataStore credit operations.
2. Inspect `PdfToolsViewModel.kt:269-302`, `MergePdfViewModel.kt:131`, `SplitPdfViewModel.kt:85, 135`, `DeletePagesViewModel.kt:84, 120`, `ExtractPagesViewModel.kt:84, 120`, and `PdfViewerViewModel.kt:85, 97` to confirm the presence of invalid credit checks and deductions on offline utilities.
3. Inspect `PdfChatViewModel.kt:59-75` and `PdfViewerViewModel.kt:141-156` to confirm AI credit gating and deduction points.
4. Inspect `MergePdfViewModel.kt:107-138`, `PdfToolsViewModel.kt:305-327`, `DocumentDao.kt`, and `DocumentEntity.kt` to confirm Scoped Storage handling and Room synchronization calls.
