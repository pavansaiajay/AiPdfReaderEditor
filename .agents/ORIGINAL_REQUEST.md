# Original User Request

## 2026-08-23T04:17:29Z

A production-grade Android PDF application ("AiPdfReaderEditor") utilizing MVI, Hilt, Compose, and Firebase AI. The goal is to separate free offline features from paid AI features, secure scoped storage saving, eliminate rendering lag/leaks, and provide flicker-free gesture controls.

Working directory: c:/Users/AJAYJKN/AndroidStudioProjects/AiPdfReaderEditor
Integrity mode: demo

## Requirements

### R1. Separate Free Offline Operations from Paid AI Features
- All basic offline utilities (Merge, Split, Extract, Compress, Annotate, Delete, Reorder, Image/PDF conversions, Scan) must be 100% free. No credits should be checked or deducted.
- Paid AI operations (Chat, Summarize, OCR) must check the user's credit balance beforehand. If the credit balance is insufficient (less than 1 for Chat/OCR, less than 5 for Summarize), operations must halt and trigger the Paywall/Ad UI by emitting an explicit `InsufficientCredits` state.
- Deduct credits (1 credit for Chat/OCR, 5 credits for Summarize) only after a successful AI operation. Make sure AI models are routed efficiently (e.g. Gemini 3.5 Lite or 3.7 Flash).

### R2. Scoped Storage & Database Synchronization
- All file saving operations must comply with Android Scoped Storage (Android 10+ / SDK 29-37) without using deprecated File path APIs.
- Use MediaStore or SAF to save single output PDFs (Merge, Compress, Encrypt, Decrypt, Images to PDF, Add Watermark, Rotate, Flatten, HTML to PDF) to public Documents/Downloads directories.
- Use SAF directory tree selection to save batch outputs (Split, PDF to Images) into a user-selected folder.
- Sync every successful PDF saving operation to the local Room database (`DocumentDao`) with the correct filename, URI, and timestamp to update recent history.

### R3. Memory Safety & Lifecycle-Bound PDF Rendering
- Solve file descriptor and memory leaks in `PdfRendererPool`. Ensure `PdfRenderer` lifecycles are strictly managed. Any open renderers must be closed and recycled when navigating away from the grid or screen (e.g., in Jetpack Compose's `onDispose` block or inside the ViewModel's `onCleared`).
- Optimize PDF memory management: prevent Out-Of-Memory (OOM) exceptions when processing large PDFs. Explicitly close all InputStreams, OutputStreams, and recycle Bitmaps immediately after rendering or compression.

### R4. Flicker-Free UI & Gesture Selection in Grids
- Ensure zero lag and high-framerate scroll rendering in PDF grids. Eliminate visual flashing, recompositions, or image reload artifacts on state changes (such as selecting/unselecting thumbnails). Draw the in-memory Bitmap directly using Compose `Image(bitmap.asImageBitmap())` or optimize Coil's cache settings to prevent content re-loading.
- Implement a conflict-free gesture system in Compose grids: support drag-to-select multiple items, hold-to-preview a single page, and standard tapping to select. Ensure gestures do not collide or swallow touch events.

## Verification Plan

### Automated Tests
- Build and run the app module using gradle command `./gradlew assembleDebug` to verify that there are no compilation or DI errors.

### Manual Verification
- **Free vs. Paid Check**: Verify that Merge, Split, Page Deletion, and Page Extraction can be performed even with 0 credits and do not deduct credits.
- **AI Gating Check**: Verify that Chat, Summarize, and OCR check for credits, deduct credits correctly on success, and show the Ad/Paywall UI when credits are insufficient.
- **Storage & History Check**: Verify that saved files appear in the public Documents or Downloads directory and are listed in the recent files history on the home screen.
- **Resource Cleanup Check**: Audit the app processes during rendering to verify that memory usage remains stable and file descriptors do not accumulate when repeatedly opening/closing PDFs.

## Acceptance Criteria

### Monetization & Flow
- [ ] Offline tools (Merge, Split, Extract, Compress, Annotate, Delete, Reorder, Image/PDF conversions, Scan) do not check or deduct credits.
- [ ] Paid features check credits (Chat/OCR: 1 credit, Summarize: 5 credits), halt and show Paywall/Ad dialog if insufficient, and deduct credits only upon successful operation.

### Storage & History
- [ ] Single files save to public Documents/Downloads via MediaStore or SAF, and batch outputs save to SAF-selected directory.
- [ ] All successful creations sync to `DocumentDao` and display in recent history.

### Performance & Gestures
- [ ] PDF rendering and pooling recycle resources, close file descriptors on screen exit/navigate away, and do not leak memory.
- [ ] Grids render thumbnails at high frame rate without visual flicker or reload loops.
- [ ] Gestures allow drag-to-select, long-press hold-to-preview, and single-tap to select without collisions.
