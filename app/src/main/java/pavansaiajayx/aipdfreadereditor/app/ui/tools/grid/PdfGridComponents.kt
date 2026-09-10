package pavansaiajayx.aipdfreadereditor.app.ui.tools.grid

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/**
 * Safe Heap-based LRU Cache: byte-counted, GC-managed reclamation (no premature .recycle() crashes).
 * Sized to 1/8th of Runtime maxMemory.
 */
object PdfThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 16) // in KB

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

    fun clearAll() {
        cache.evictAll()
    }
}

val globalPdfRendererPool = PdfRendererPool()

@Composable
fun PdfThumbnailGrid(
    uri: Uri,
    pageCount: Int,
    selectedPages: Set<Int>,
    onPageSelected: (Int) -> Unit,
    onDragSelectRange: (startIndex: Int, endIndex: Int) -> Unit = { _, _ -> },
    onDragSelectStart: (Int) -> Unit = {},
    onDragSelectUpdate: (Int) -> Unit = {},
    pdfRendererPool: PdfRendererPool = globalPdfRendererPool,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var previewPageIndex by remember { mutableStateOf<Int?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }

    // Lifecycle cleanup: cleanly close renderers and evict thumbnails when navigating away or switching URIs
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
                                onDragSelectStart(it.index)
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
                                onDragSelectUpdate(it.index)
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
                    onClick = { onPageSelected(pageIndex) },
                    onLongClick = { previewPageIndex = pageIndex },
                    pdfRendererPool = pdfRendererPool
                )
            }
        }

        // Full-screen Hold-to-Preview Dialog
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
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onPreviewHold: (Boolean) -> Unit = {},
    pdfRendererPool: PdfRendererPool = globalPdfRendererPool
) {
    val context = LocalContext.current
    var bitmap by remember(uri, pageIndex) {
        mutableStateOf(PdfThumbnailCache.get("${uri}_${pageIndex}_thumb"))
    }

    LaunchedEffect(uri, pageIndex) {
        val cacheKey = "${uri}_${pageIndex}_thumb"
        if (bitmap == null) {
            bitmap = PdfThumbnailCache.get(cacheKey)
                ?: pdfRendererPool.renderPage(context, uri, pageIndex, 400)?.also {
                    PdfThumbnailCache.put(cacheKey, it)
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
                    onLongPress = { 
                        onPreviewHold(true)
                        onLongClick() 
                    }
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
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
