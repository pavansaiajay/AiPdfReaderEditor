---
name: android-data-layer
description: Authoritative guide for data architecture, Room 3, DataStore, Scoped Storage (MediaStore and SAF), and Repository pattern.
---

# Android Data Layer: AiPdfReaderEditor

## 1. Storage Strategy
- **Single File Exports**:
  - Target: `MediaStore.Files.getContentUri("external")` under `Documents/AiPdfReaderEditor`.
  - Open OutputStream with `IS_PENDING = 1`, write buffered bytes, then set `IS_PENDING = 0`.
- **Batch Exports**:
  - Use SAF `OpenDocumentTree` to let the user select a folder once.
  - Create child documents via `DocumentFile.fromTreeUri()`.
- **History Synchronization**:
  - Every exported or modified document must insert a row into Room `DocumentDao`.

## 2. Room 3 & Persistence
- Use `androidx.room3` with `sqlite-bundled` driver and KSP.
- Database operations must be exposed as `Flow<List<DocumentEntity>>` for reactive UI updates.
