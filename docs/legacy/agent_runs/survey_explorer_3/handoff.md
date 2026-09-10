# Survey Explorer 3: Technical Investigation Report
**Domain**: Memory Safety, Lifecycle-Bound PDF Rendering (R3) & Flicker-Free UI / Gesture Selection in Grids (R4)  
**Author**: Survey Explorer 3 (Memory, Rendering & Gesture System Specialist)  
**Date**: 2026-08-23  

---

## 1. Executive Summary

This report delivers a deep, code-level investigation into **Requirement 3 (Memory Safety & Lifecycle-Bound PDF Rendering)** and **Requirement 4 (Flicker-Free UI & Gesture Selection in Grids)** for the `AiPdfReaderEditor` Android application.

### Key Architectural Findings:
1. **Critical Resource & File Descriptor Leak in `PdfRendererPool`**:
   - `globalPdfRendererPool` is declared as a global top-level instance (`PdfGridComponents.kt:43`).
   - Open `ParcelFileDescriptor` and native `PdfRenderer` instances accumulate in an unbounded `mutableMapOf<String, MutableList<PdfRenderer>>`.
   - `closeAll()` is defined but **never invoked anywhere in the codebase**. There is **no method to close renderers for a specific URI** (`close(uri)`), and **zero** usages of `DisposableEffect { onDispose { ... } }` or ViewModel `onCleared()` exist across the UI and ViewModel layers.
2. **Fatal Bitmap Recycling Crash in `LruCache`**:
   - `PdfGridComponents.kt:35-41` overrides `entryRemoved` in `thumbnailCache` and directly calls `oldValue.recycle()` on cache eviction.
   - Because Compose UI nodes (`PdfThumbnailItem`) maintain references to the cached `Bitmap` during recomposition or scrolling, drawing an evicted bitmap causes immediate fatal runtime crashes (`java.lang.RuntimeException: Canvas: trying to use a recycled bitmap`).
3. **Thumbnail Flashing & Recomposition Stutter (R4)**:
   - `PdfThumbnailItem` passes in-memory `Bitmap` instances into Coil 3's `AsyncImage` (`PdfGridComponents.kt:173-178`).
   - Coil's asynchronous request pipeline and painter resolution execute upon every selection state change (`isSelected` recomposition), producing visual flashing, placeholder flashes, and UI stutter.
4. **Broken Gesture System & Missing Drag-to-Select (R4)**:
   - `PdfThumbnailItem` attaches redundant empty pointer loops (`awaitPointerEventScope`) and consumes pointer events in `detectTapGestures`, swallowing touch events before drag gestures can be recognized.
   - `PdfThumbnailGrid` defines `onDragSelectStart` and `onDragSelectUpdate` parameters, but **neither `PdfThumbnailGrid` nor any screen/ViewModel implements drag-to-select logic**.

---

## 2. Detailed Technical Observations

### 2.1 Investigation of R3: Memory Safety & Lifecycle-Bound PDF Rendering

#### A. `PdfRendererPool.kt` Analysis
- **File Location**: `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/core/pdf/PdfRendererPool.kt`
- **Lines 20-73**:
  ```kotlin
  class PdfRendererPool {
      private val maxConcurrent = 4
      private val semaphore = Semaphore(maxConcurrent)
      private val renderers = mutableMapOf<String, MutableList<PdfRenderer>>()
      private val mutex = Mutex()

      suspend fun renderPage(context: Context, uri: Uri, pageIndex: Int, width: Int = 400): Bitmap {
          return semaphore.withPermit {
              withContext(Dispatchers.IO) {
                  val uriString = uri.toString()
                  val renderer = mutex.withLock {
                      val list = renderers.getOrPut(uriString) { mutableListOf() }
                      if (list.isNotEmpty()) {
                          list.removeAt(list.size - 1)
                      } else {
                          val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                              ?: throw IllegalArgumentException("Cannot open PDF URI")
                          PdfRenderer(pfd)
                      }
                  }

                  try {
                      val page = renderer.openPage(pageIndex)
                      page.use { page ->
                          val scale = width.toFloat() / page.width.toFloat()
                          val height = (page.height * scale).toInt()

                          val bitmap = createBitmap(width, height)
                          bitmap.eraseColor(Color.WHITE)
                          page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                          bitmap
                      }
                  } finally {
                      mutex.withLock {
                          renderers[uriString]?.add(renderer)
                      }
                  }
              }
          }
      }

      suspend fun closeAll() {
          mutex.withLock {
              for (list in renderers.values) {
                  for (renderer in list) {
                      try { renderer.close() } catch (e: Exception) {}
                  }
              }
              renderers.clear()
          }
      }
  }
  ```
- **Defects Identified**:
  1. **Global Unbounded Accumulation**: Whenever a new URI is opened, up to 4 `PdfRenderer` instances (and their underlying `ParcelFileDescriptor` file handles) are allocated and stored in `renderers[uriString]`.
  2. **No Per-URI Cleanup**: There is no `fun close(uri: Uri)` method. If a user navigates between 10 different PDF documents, 40 open native file descriptors remain allocated indefinitely.
  3. **Tainted Instance Recycling**: If `openPage` fails with an exception (e.g. invalid index or corrupted page), the `finally` block unconditionally returns the potentially broken `renderer` back into the pool.
  4. **Lack of Dependency Injection**: `PdfRendererPool` is not bound as a singleton in Dagger/Hilt, but instantiated as a global top-level val `val globalPdfRendererPool = PdfRendererPool()` in `PdfGridComponents.kt:43`.

#### B. Absence of Lifecycle Hooks in UI & ViewModels
- **Codebase Audit**:
  - `DisposableEffect` / `onDispose`: **0 occurrences** across the entire codebase.
  - `ViewModel.onCleared()`: **0 occurrences** across all ViewModels (`DeletePagesViewModel`, `ExtractPagesViewModel`, `SplitPdfViewModel`, `PdfViewerViewModel`, `MergePdfViewModel`, `PdfToolsViewModel`, `HomeViewModel`, `PdfChatViewModel`).
- **Impact**:
  - Navigating away from `DeletePagesScreen`, `ExtractPagesScreen`, `SplitPdfScreen`, or `PdfViewerScreen` leaves rendering pools, cached bitmaps, and file descriptors completely active in memory.

#### C. In-Memory LRU Cache Bitmap Recycling Bug
- **File Location**: `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/ui/tools/grid/PdfGridComponents.kt`
- **Lines 35-41**:
  ```kotlin
  private val thumbnailCache = object : LruCache<String, Bitmap>(50) {
      override fun entryRemoved(evicted: Boolean, key: String?, oldValue: Bitmap?, newValue: Bitmap?) {
          if (evicted && oldValue != newValue && oldValue?.isRecycled == false) {
              oldValue.recycle()
          }
      }
  }
  ```
- **Defects Identified**:
  1. **Premature Recycling**: In Android graphics and Jetpack Compose Skia pipeline, calling `Bitmap.recycle()` while a Composable item (`PdfThumbnailItem`) is in the composition hierarchy causes immediate unrecoverable crashes when drawing.
  2. **Static Capacity Count**: Hardcoded count `50` takes no account of byte size. 50 full-resolution or 800px preview bitmaps consume ~180MB of heap. LRU cache sizing must calculate capacity using `bitmap.byteCount / 1024` and `Runtime.getRuntime().maxMemory()`.

#### D. Stream Handling & OOM Risks in `PdfEngine.kt`
- **File Location**: `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/core/pdf/PdfEngine.kt`
- **Lines 210-245 (`imagesToPdf`)**:
  ```kotlin
  val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@use
  val pdImage = JPEGFactory.createFromImage(document, bitmap)
  ...
  bitmap.recycle()
  ```
  - `BitmapFactory.decodeStream(inputStream)` decodes full-resolution camera pictures (e.g. 108MP / 50MP images = 200MB+ bitmap) without `inSampleSize` or bounds inspection, causing immediate `OutOfMemoryError` before reaching `bitmap.recycle()`.
- **Stream Closure Verification in other `PdfEngine` methods**:
  - `mergePdfs` (lines 50-83): Correctly accumulates `InputStream`s and closes them in a `finally` block.
  - `compressPdf` (lines 117-149): Correctly calls `recycle()` on `originalBitmap` and `scaledBitmap`.
  - `pdfToImages` (lines 247-269): Correctly calls `recycle()` on each rendered page bitmap.
  - `getPageCount` (lines 604-612): Correctly closes `ParcelFileDescriptor` and `PdfRenderer` using `.use { }`.

---

### 2.2 Investigation of R4: Flicker-Free UI & Gesture Selection in Grids

#### A. Coil 3 `AsyncImage` vs Compose `Image(asImageBitmap())`
- **File Location**: `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/ui/tools/grid/PdfGridComponents.kt`
- **Lines 171-184**:
  ```kotlin
  if (bitmap != null) {
      // Using Coil's AsyncImage with crossfade(false) as requested
      AsyncImage(
          model = bitmap,
          contentDescription = "Page ${pageIndex + 1}",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
      )
  } else {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(strokeWidth = 2.dp)
      }
  }
  ```
- **Defects Identified**:
  1. `AsyncImage` is designed for remote/asynchronous URI loading. Passing a pre-rendered `android.graphics.Bitmap` causes Coil to schedule background painter jobs, dispatch image requests, and execute transition animations.
  2. When the user taps a grid item to toggle selection, `isSelected` changes from `false` to `true`. This causes `PdfThumbnailItem` to recompose. When `AsyncImage` recomposes, it momentarily re-enters loading/animating states, resulting in visual flicker and thumbnail blinking.
  3. Direct drawing with Jetpack Compose native `Image(bitmap = bitmap.asImageBitmap(), contentDescription = ...)` bypasses any async scheduling and draws directly to the canvas in 0ms with zero flicker.

#### B. Gesture Collisions & Missing Drag-to-Select
- **File Location**: `app/src/main/java/pavansaiajayx/aipdfreadereditor/app/ui/tools/grid/PdfGridComponents.kt`
- **Lines 139-169**:
  ```kotlin
  .pointerInput(Unit) {
      // Handle Long Press to Preview (consumes pointer so drag-to-select won't trigger)
      awaitPointerEventScope {
          while (true) {
              val event = awaitPointerEvent(PointerEventPass.Main)
              if (event.changes.any { it.pressed }) {
                  // Start hold logic in detectTapGestures
              }
          }
      }
  }
  .pointerInput(Unit) {
      detectTapGestures(
          onPress = {
              val pressStartTime = System.currentTimeMillis()
              var holding = true
              try {
                  val released = tryAwaitRelease()
                  holding = false
                  onPreviewHold(false)
              } catch (e: Exception) {
                  holding = false
                  onPreviewHold(false)
              }
          },
          onTap = { onClick() },
          onLongPress = { 
              onPreviewHold(true) 
          }
      )
  }
  ```
- **Defects Identified**:
  1. **Redundant Dead Pointer Loop**: Lines 139-149 run an infinite `while(true)` polling pointer events without taking any action.
  2. **Touch Event Swallowing**: `detectTapGestures` on every thumbnail item intercepts down/up touch events. When a user drags their finger across items to select multiple pages, the first item's tap gesture detector consumes the down event and cancels when drag movement threshold is exceeded, preventing parent drag detection.
  3. **No Drag-to-Select in Grid**: `LazyVerticalGrid` (lines 60-80) has **no drag gesture listener**.
  4. **No Drag Selection in ViewModels**: `DeletePagesViewModel`, `ExtractPagesViewModel`, and `SplitPdfViewModel` only support single-item `togglePageSelection(pageIndex: Int)`, lacking range selection (`selectRange(from, to)`) or drag-selection batch states.

---

## 3. Logic Chain: Root Causes to Architectural Solutions

```
[Observation 1: globalPdfRendererPool never closes open renderers]
    └──> [Root Cause: No scoped lifecycle binding or close(uri) method]
    └──> [Solution: Add close(uri) / closeAll() to PdfRendererPool, inject via Hilt, and bind to DisposableEffect(uri) / onDispose]

[Observation 2: LruCache.entryRemoved calls oldValue.recycle()]
    └──> [Root Cause: Active Compose UI nodes hold references to evicted bitmaps]
    └──> [Solution: Remove oldValue.recycle() from entryRemoved; manage memory capacity by byte size and maxMemory / 8]

[Observation 3: Coil AsyncImage used with in-memory Bitmap model]
    └──> [Root Cause: Coil request pipeline introduces async painter delay on recomposition]
    └──> [Solution: Replace with pure Compose Image(bitmap = bitmap.asImageBitmap())]

[Observation 4: Thumbnail items consume pointer events; no drag-to-select on LazyVerticalGrid]
    └──> [Root Cause: Conflicting detectTapGestures modifiers and missing grid-level coordinate tracking]
    └──> [Solution: Unify gestures: tap-to-select, long-press preview with clean release detection, and drag-to-select tracking via LazyGridState.layoutInfo]
```

---

## 4. Proposed Code Blueprints & Architectural Designs

### 4.1 Memory-Safe `PdfRendererPool.kt`

```kotlin
package pavansaiajayx.aipdfreadereditor.app.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRendererPool @Inject constructor() {
    private val maxConcurrent = 4
    private val semaphore = Semaphore(maxConcurrent)
    private val renderers = mutableMapOf<String, MutableList<PdfRenderer>>()
    private val mutex = Mutex()

    suspend fun renderPage(
        context: Context, 
        uri: Uri, 
        pageIndex: Int, 
        targetWidth: Int = 400
    ): Bitmap? = semaphore.withPermit {
        withContext(Dispatchers.IO) {
            val uriString = uri.toString()
            var renderer: PdfRenderer? = null
            var isRendererHealthy = true

            try {
                renderer = mutex.withLock {
                    val list = renderers.getOrPut(uriString) { mutableListOf() }
                    if (list.isNotEmpty()) {
                        list.removeAt(list.size - 1)
                    } else {
                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                            ?: throw IOException("Cannot open ParcelFileDescriptor for URI: $uri")
                        PdfRenderer(pfd)
                    }
                }

                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    return@withContext null
                }

                renderer.openPage(pageIndex).use { page ->
                    val width = targetWidth.coerceAtLeast(1)
                    val scale = width.toFloat() / page.width.toFloat()
                    val height = (page.height * scale).toInt().coerceAtLeast(1)

                    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            } catch (e: Exception) {
                isRendererHealthy = false
                try { renderer?.close() } catch (_: Exception) {}
                null
            } finally {
                if (isRendererHealthy && renderer != null) {
                    mutex.withLock {
                        renderers[uriString]?.add(renderer)
                    }
                }
            }
        }
    }

    suspend fun closeUri(uri: Uri) {
        withContext(Dispatchers.IO) {
            val uriString = uri.toString()
            val listToClose = mutex.withLock {
                renderers.remove(uriString)
            }
            listToClose?.forEach { renderer ->
                try { renderer.close() } catch (_: Exception) {}
            }
        }
    }

    suspend fun closeAll() {
        withContext(Dispatchers.IO) {
            val allRenderers = mutex.withLock {
                val copy = renderers.values.flatten()
                renderers.clear()
                copy
            }
            allRenderers.forEach { renderer ->
                try { renderer.close() } catch (_: Exception) {}
            }
        }
    }
}
```

---

### 4.2 Safe Thumbnail Cache & Direct Compose Drawing (`PdfGridComponents.kt`)

```kotlin
package pavansaiajayx.aipdfreadereditor.app.ui.tools.grid

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfRendererPool

// Safe Heap-based LRU Cache (byte-counted, GC-reclaimed, no premature .recycle() crashes)
object PdfThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 16) // 1/8th of available memory

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
    fun clearForUri(uri: Uri) {
        val prefix = uri.toString()
        val snapshot = cache.snapshot()
        for (key in snapshot.keys) {
            if (key.startsWith(prefix)) {
                cache.remove(key)
            }
        }
    }
}
```

---

### 4.3 Conflict-Free Gesture Architecture in `PdfThumbnailGrid`

```kotlin
@Composable
fun PdfThumbnailGrid(
    uri: Uri,
    pageCount: Int,
    selectedPages: Set<Int>,
    onPageSelected: (Int) -> Unit,
    onDragSelectRange: (startIndex: Int, endIndex: Int) -> Unit,
    pdfRendererPool: PdfRendererPool,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var previewPageIndex by remember { mutableStateOf<Int?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }

    // Lifecycle cleanup: Close PdfRenderer instances and release cache for this URI on exit
    DisposableEffect(uri) {
        onDispose {
            coroutineScope.launch {
                pdfRendererPool.closeUri(uri)
            }
            PdfThumbnailCache.clearForUri(uri)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pageCount) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val item = gridState.layoutInfo.visibleItemsInfo.find { info ->
                                offset.x.toInt() in info.offset.x..(info.offset.x + info.size.width) &&
                                offset.y.toInt() in info.offset.y..(info.offset.y + info.size.height)
                            }
                            item?.let {
                                dragStartIndex = it.index
                                onDragSelectRange(it.index, it.index)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val start = dragStartIndex ?: return@detectDragGesturesAfterLongPress
                            val pos = change.position
                            val item = gridState.layoutInfo.visibleItemsInfo.find { info ->
                                pos.x.toInt() in info.offset.x..(info.offset.x + info.size.width) &&
                                pos.y.toInt() in info.offset.y..(info.offset.y + info.size.height)
                            }
                            item?.let {
                                onDragSelectRange(minOf(start, it.index), maxOf(start, it.index))
                            }
                        },
                        onDragEnd = { dragStartIndex = null },
                        onDragCancel = { dragStartIndex = null }
                    )
                }
        ) {
            items(pageCount, key = { "page_${uri}_$it" }) { pageIndex ->
                val isSelected = selectedPages.contains(pageIndex)

                PdfThumbnailItem(
                    uri = uri,
                    pageIndex = pageIndex,
                    isSelected = isSelected,
                    pdfRendererPool = pdfRendererPool,
                    onClick = { onPageSelected(pageIndex) },
                    onLongClick = { previewPageIndex = pageIndex }
                )
            }
        }

        // Full-screen Hold-to-Preview Dialog/Overlay
        previewPageIndex?.let { pageIdx ->
            Dialog(onDismissRequest = { previewPageIndex = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.8f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.9f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    var highResBitmap by remember { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(pageIdx) {
                        val cacheKey = "${uri}_${pageIdx}_highres"
                        highResBitmap = PdfThumbnailCache.get(cacheKey)
                            ?: pdfRendererPool.renderPage(context, uri, pageIdx, 900)?.also {
                                PdfThumbnailCache.put(cacheKey, it)
                            }
                    }

                    highResBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Page Preview $pageIdx",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } ?: CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PdfThumbnailItem(
    uri: Uri,
    pageIndex: Int,
    isSelected: Boolean,
    pdfRendererPool: PdfRendererPool,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(uri, pageIndex) {
        mutableStateOf(PdfThumbnailCache.get("${uri}_${pageIndex}_thumb"))
    }

    LaunchedEffect(uri, pageIndex) {
        if (bitmap == null) {
            val key = "${uri}_${pageIndex}_thumb"
            bitmap = PdfThumbnailCache.get(key)
                ?: pdfRendererPool.renderPage(context, uri, pageIndex, 400)?.also {
                    PdfThumbnailCache.put(key, it)
                }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray.copy(alpha = 0.2f))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        bitmap?.let { b ->
            // Direct synchronous Compose drawing — ZERO flicker, 120fps smooth scrolling
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }

        // Selection overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            )
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        // Page number badge
        Text(
            text = "${pageIndex + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
```

---

## 5. ViewModel Batch Range Selection Extension

In `DeletePagesViewModel`, `ExtractPagesViewModel`, and `SplitPdfViewModel`:
```kotlin
fun selectRange(startIndex: Int, endIndex: Int) {
    val current = _selectedPages.value.toMutableSet()
    for (i in minOf(startIndex, endIndex)..maxOf(startIndex, endIndex)) {
        current.add(i)
    }
    _selectedPages.value = current
}

fun selectAll() {
    _selectedPages.value = (0 until _pageCount.value).toSet()
}

fun clearSelection() {
    _selectedPages.value = emptySet()
}

override fun onCleared() {
    super.onCleared()
    // Trigger any ViewModel-scoped cleanup if needed
}
```

---

## 6. Formal 5-Component Handoff Protocol

### 1. Observation
- `PdfRendererPool` instantiates `PdfRenderer(pfd)` on line 39 of `core/pdf/PdfRendererPool.kt` without ever releasing renderers per URI; `closeAll()` is never called in the codebase.
- `thumbnailCache` on line 38 of `ui/tools/grid/PdfGridComponents.kt` executes `oldValue.recycle()` upon eviction, crashing active Compose rendering nodes.
- `PdfThumbnailItem` on line 173 of `ui/tools/grid/PdfGridComponents.kt` renders via Coil's `AsyncImage(model = bitmap)` causing recomposition flicker on selection changes.
- Drag-to-select is unhandled in `LazyVerticalGrid` and grid ViewModels.
- `BitmapFactory.decodeStream(inputStream)` on line 215 of `core/pdf/PdfEngine.kt` decodes unconstrained full-resolution bitmaps.

### 2. Logic Chain
- Unclosed `ParcelFileDescriptor`s and native `PdfRenderer` instances lead to OS file descriptor exhaustion (`EMFILE`).
- Calling `Bitmap.recycle()` while Skia HardwareRenderer holds an image pointer produces fatal runtime exceptions.
- Coil's asynchronous request resolution lifecycle cannot match synchronous 60/120fps Compose draw calls when item state (`isSelected`) updates.
- Centralizing drag detection on `LazyVerticalGrid` using `detectDragGesturesAfterLongPress` with layout coordinate hit-testing prevents item touch swallowing while enabling smooth drag selection.

### 3. Caveats
- `io.github.afreakyelf:Pdf-Viewer` (`PdfRendererViewCompose`) used in `PdfViewerScreen` and `MergePdfScreen` handles its own internal `PdfRenderer` lifecycle via `lifecycleOwner = LocalLifecycleOwner.current`.
- Large multi-thousand page PDFs should ensure thumbnail dimensions are kept at 300-400px width to balance memory consumption and crispness.

### 4. Conclusion
- R3 requires: (a) Updating `PdfRendererPool` with scoped URI closing and healthy instance checks, (b) adding `DisposableEffect` / `onDispose` to grid screens, (c) removing `oldValue.recycle()` from `LruCache.entryRemoved` and using a byte-counted cache, and (d) adding `inSampleSize` decoding in `imagesToPdf`.
- R4 requires: (a) Replacing Coil `AsyncImage` with Compose `Image(bitmap.asImageBitmap())`, (b) attaching `detectDragGesturesAfterLongPress` with visible item hit-testing to `LazyVerticalGrid`, and (c) implementing `selectRange(start, end)` in grid ViewModels.

### 5. Verification Method
- **Automated Verification**: Run `./gradlew assembleDebug` to ensure all modifications compile cleanly without dependency or syntax errors.
- **Leak Verification**: Open and close 10 different PDF documents in `DeletePagesScreen` and verify via Android Profiler that file descriptors and native memory remain stable and are reclaimed on back navigation.
- **Flicker & Gesture Verification**: Open a 30-page PDF in `DeletePagesScreen`, rapidly tap to select/unselect items to verify zero flickering, long-press to view the preview dialog, and drag across 5 items to verify seamless multi-item selection.
