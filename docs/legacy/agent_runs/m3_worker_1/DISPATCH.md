## 2026-08-23T05:15:26Z
Worker 1 for Milestone 3 (R3: Memory Safety & Lifecycle-Bound PDF Rendering).
Working Directory: c:/Users/AJAYJKN/AndroidStudioProjects/AiPdfReaderEditor/.agents/m3_worker_1
Parent: Project Orchestrator (Conversation ID: 9ae7d212-5a8f-4910-b2f9-a2343c3d9e0d)

Exclusive Write Ownership:
- app/src/main/java/pavansaiajayx/aipdfreadereditor/app/core/pdf/PdfRendererPool.kt
- app/src/main/java/pavansaiajayx/aipdfreadereditor/app/ui/tools/grid/PdfGridComponents.kt
- app/src/main/java/pavansaiajayx/aipdfreadereditor/app/core/pdf/PdfEngine.kt

Requirements:
1. PdfRendererPool.kt:
   - Annotate with @Singleton @Inject constructor().
   - Implement suspend fun renderPage(context: Context, uri: Uri, pageIndex: Int, targetWidth: Int = 400): Bitmap? with concurrency semaphore (max 4), mutex-locked URI renderer map, bounds checks (pageIndex in 0 until renderer.pageCount), and healthy renderer recovery (close tainted renderers on exception without recycling broken instances).
   - Implement suspend fun closeUri(uri: Uri) that removes and cleanly closes all open PdfRenderer and ParcelFileDescriptor instances for that specific URI.
   - Implement suspend fun closeAll() that cleanly closes all pooled renderers.
2. PdfGridComponents.kt:
   - Replace crashing static thumbnailCache with PdfThumbnailCache: byte-counted LruCache<String, Bitmap> sized to 1/8th of Runtime.getRuntime().maxMemory(), using GC-managed recycling and clearForUri(uri: Uri).
3. PdfEngine.kt:
   - In imagesToPdf: Implement memory-safe image decoding using BitmapFactory.Options with inJustDecodeBounds = true, calculate inSampleSize (targeting max 2048x2048 resolution) to prevent OOM crashes on high-res camera photos, and explicitly recycle decoded bitmaps.
   - Ensure all InputStreams, OutputStreams, and PDDocument instances in PdfEngine are closed with .use { } blocks.
4. Verification:
   - Run ./gradlew testDebugUnitTest and ./gradlew assembleDebug.
