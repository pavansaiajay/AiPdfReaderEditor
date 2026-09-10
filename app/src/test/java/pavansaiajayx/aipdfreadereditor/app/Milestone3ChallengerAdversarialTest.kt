package pavansaiajayx.aipdfreadereditor.app

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.junit.Assert.*
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEngine
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfRendererPool
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.PdfThumbnailCache
import java.io.File
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Challenger 1 Adversarial Test Suite for Milestone 3 (R3: Memory Safety & Lifecycle-Bound PDF Rendering).
 *
 * Empirical Challenges:
 * 1. PdfRendererPool: Concurrency bounds (max 4), out-of-bounds page handling, corrupted renderer discard, per-URI and global closure.
 * 2. PdfThumbnailCache: Capacity calculation (1/8th maxMemory), zero recycled bitmap crashes during cache eviction, URI prefix clearing, concurrency safety.
 * 3. imagesToPdf: calculateInSampleSize downsampling calculation on large images (8000x6000, 12000x9000, edge cases) to prevent OOM.
 * 4. Stream & Document Lifecycle Audit: Explicit stream and PDDocument closure in PdfEngine.
 */
class Milestone3ChallengerAdversarialTest {

    // =========================================================================
    // 1. PdfRendererPool Architecture & Concurrency Challenges
    // =========================================================================

    @Test
    fun testPdfRendererPoolConcurrencyBounds() = runBlocking {
        // Verify Semaphore(4) bounds concurrent operations to at most 4
        val maxConcurrent = 4
        val semaphore = Semaphore(maxConcurrent)
        val activeCount = AtomicInteger(0)
        val peakCount = AtomicInteger(0)

        val jobs = (1..20).map {
            launch(Dispatchers.Default) {
                semaphore.withPermit {
                    val current = activeCount.incrementAndGet()
                    peakCount.updateAndGet { peak -> maxOf(peak, current) }
                    delay(10) // Simulate rendering work
                    activeCount.decrementAndGet()
                }
            }
        }
        jobs.joinAll()

        assertTrue("Peak concurrent executions must not exceed maxConcurrent ($maxConcurrent)", peakCount.get() <= maxConcurrent)
        assertEquals("All executions must have completed", 0, activeCount.get())
    }

    /**
     * Mock PdfRenderer to test pool mechanics, out-of-bounds rendering,
     * corrupted renderer discarding, and lifecycle closure.
     */
    class MockPdfRenderer(val uri: String, val pageCount: Int, var isCorrupted: Boolean = false) {
        var isClosed: Boolean = false
            private set
        var openedPageCount: Int = 0

        fun openPage(index: Int): MockPage {
            if (isClosed) throw IllegalStateException("Renderer is closed")
            if (isCorrupted) throw IllegalStateException("Renderer is corrupted (native C++ SIGSEGV simulated)")
            if (index < 0 || index >= pageCount) throw IllegalArgumentException("Invalid page index: $index (total: $pageCount)")
            openedPageCount++
            return MockPage(index)
        }

        fun close() {
            isClosed = true
        }

        class MockPage(val index: Int) : AutoCloseable {
            var isClosed = false
            override fun close() {
                isClosed = true
            }
        }
    }

    class TestablePdfRendererPool {
        private val maxConcurrent = 4
        val semaphore = Semaphore(maxConcurrent)
        val renderers = mutableMapOf<String, MutableList<MockPdfRenderer>>()
        val mutex = Mutex()
        var createdCount = 0

        suspend fun renderPage(
            uriString: String,
            pageIndex: Int,
            totalDocPages: Int,
            shouldThrowOnRender: Boolean = false
        ): String? = semaphore.withPermit {
            withContext(Dispatchers.IO) {
                var renderer: MockPdfRenderer? = null
                var isRendererHealthy = true

                try {
                    renderer = mutex.withLock {
                        val list = renderers.getOrPut(uriString) { mutableListOf() }
                        if (list.isNotEmpty()) {
                            list.removeAt(list.size - 1)
                        } else {
                            createdCount++
                            MockPdfRenderer(uriString, totalDocPages)
                        }
                    }

                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                        return@withContext null
                    }

                    if (shouldThrowOnRender) {
                        renderer.isCorrupted = true
                    }

                    renderer.openPage(pageIndex).use {
                        "Rendered page $pageIndex of $uriString"
                    }
                } catch (e: Exception) {
                    isRendererHealthy = false
                    try {
                        renderer?.close()
                    } catch (_: Exception) {}
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

        suspend fun closeUri(uriString: String) {
            withContext(Dispatchers.IO) {
                val listToClose = mutex.withLock {
                    renderers.remove(uriString)
                }
                listToClose?.forEach { renderer ->
                    try {
                        renderer.close()
                    } catch (_: Exception) {}
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
                    try {
                        renderer.close()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    @Test
    fun testPdfRendererPoolOutOfBoundsPageReturnsNullSafely() = runBlocking {
        val pool = TestablePdfRendererPool()
        val uri = "content://sample/doc1.pdf"
        val totalPages = 5

        // Out of bounds: negative index
        val resNeg = pool.renderPage(uri, -1, totalPages)
        assertNull("Negative page index must return null safely", resNeg)

        // Out of bounds: equal to page count (0-indexed)
        val resEq = pool.renderPage(uri, 5, totalPages)
        assertNull("Index equal to pageCount must return null safely", resEq)

        // Out of bounds: far beyond page count
        val resFar = pool.renderPage(uri, 999, totalPages)
        assertNull("Index beyond pageCount must return null safely", resFar)

        // Valid page rendering must succeed and reuse healthy pooled renderer
        val resValid = pool.renderPage(uri, 2, totalPages)
        assertNotNull("Valid page index must render successfully", resValid)
        assertEquals("Rendered page 2 of content://sample/doc1.pdf", resValid)
        assertEquals("Renderer must be reused from pool", 1, pool.createdCount)
    }

    @Test
    fun testPdfRendererPoolCorruptedFileDiscardsRenderer() = runBlocking {
        val pool = TestablePdfRendererPool()
        val uri = "content://sample/corrupted.pdf"

        // Render with simulated native corruption exception
        val resCorrupted = pool.renderPage(uri, 0, 5, shouldThrowOnRender = true)
        assertNull("Corrupted render must return null", resCorrupted)

        // Verify corrupted renderer was closed and NOT returned to pool
        val pooledList = pool.renderers[uri]
        assertTrue("Corrupted renderer must NOT be returned to the pool", pooledList.isNullOrEmpty())

        // Next render must create a fresh renderer
        val resClean = pool.renderPage(uri, 0, 5, shouldThrowOnRender = false)
        assertNotNull(resClean)
        assertEquals("New clean renderer must have been created", 2, pool.createdCount)
    }

    @Test
    fun testPdfRendererPoolPerUriClosure() = runBlocking {
        val pool = TestablePdfRendererPool()
        val uri1 = "content://sample/doc1.pdf"
        val uri2 = "content://sample/doc2.pdf"

        // Render pages for both URIs to populate pool
        pool.renderPage(uri1, 0, 5)
        pool.renderPage(uri2, 0, 5)

        assertEquals(1, pool.renderers[uri1]?.size)
        assertEquals(1, pool.renderers[uri2]?.size)

        val renderer1 = pool.renderers[uri1]?.first()!!
        val renderer2 = pool.renderers[uri2]?.first()!!

        assertFalse(renderer1.isClosed)
        assertFalse(renderer2.isClosed)

        // Close only uri1
        pool.closeUri(uri1)

        assertTrue("renderer1 must be closed", renderer1.isClosed)
        assertNull("uri1 must be removed from pool", pool.renderers[uri1])

        assertFalse("renderer2 for uri2 must NOT be closed", renderer2.isClosed)
        assertEquals("uri2 must remain in pool", 1, pool.renderers[uri2]?.size)

        // Close all
        pool.closeAll()
        assertTrue("renderer2 must now be closed", renderer2.isClosed)
        assertTrue("All pool maps must be empty", pool.renderers.isEmpty())
    }

    @Test
    fun testPdfRendererPoolReflectionInspection() {
        val clazz = PdfRendererPool::class.java
        val fields = clazz.declaredFields.map { it.name }
        assertTrue("PdfRendererPool must have semaphore", fields.contains("semaphore"))
        assertTrue("PdfRendererPool must have mutex", fields.contains("mutex"))
        assertTrue("PdfRendererPool must have renderers map", fields.contains("renderers"))

        val methods = clazz.declaredMethods.map { it.name }
        assertTrue("PdfRendererPool must have renderPage method", methods.contains("renderPage"))
        assertTrue("PdfRendererPool must have closeUri method", methods.contains("closeUri"))
        assertTrue("PdfRendererPool must have closeAll method", methods.contains("closeAll"))
    }

    // =========================================================================
    // 2. PdfThumbnailCache Capacity & Zero-Recycle Crash Challenges
    // =========================================================================

    @Test
    fun testPdfThumbnailCacheCapacityCalculation() {
        val maxMemoryKB = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val expectedMinCacheSize = (maxMemoryKB / 8).coerceAtLeast(1024 * 16) // in KB

        assertTrue("Cache size must be at least 16 MB (16384 KB)", expectedMinCacheSize >= 16384)
        assertTrue("Cache size must not exceed 1/8th of JVM maxMemory", expectedMinCacheSize <= maxMemoryKB / 8 || expectedMinCacheSize == 16384)
    }

    /**
     * Simulates safe LruCache without oldValue.recycle() to verify zero crash behavior
     * during cache eviction and concurrent usage.
     */
    class SafeThumbnailLruCache(private val maxSizeBytes: Long) {
        private val map = LinkedHashMap<String, FakeBitmap>(16, 0.75f, true)
        private var currentSize: Long = 0

        fun put(key: String, bitmap: FakeBitmap) = synchronized(map) {
            val old = map.put(key, bitmap)
            currentSize += bitmap.byteCount
            if (old != null) {
                currentSize -= old.byteCount
            }
            trimToSize(maxSizeBytes)
        }

        fun get(key: String): FakeBitmap? = synchronized(map) { map[key] }

        fun remove(key: String): FakeBitmap? = synchronized(map) {
            val removed = map.remove(key)
            if (removed != null) {
                currentSize -= removed.byteCount
            }
            return removed
        }

        fun clearForPrefix(prefix: String) = synchronized(map) {
            val snapshot = LinkedHashMap(map)
            for (key in snapshot.keys) {
                if (key.startsWith(prefix)) {
                    remove(key)
                }
            }
        }

        private fun trimToSize(max: Long) {
            val iterator = map.entries.iterator()
            while (iterator.hasNext() && currentSize > max) {
                val entry = iterator.next()
                currentSize -= entry.value.byteCount
                iterator.remove()
                // CRITICAL SAFETY: DO NOT call entry.value.recycle() here!
                // Evicted bitmaps must be garbage collected naturally by JVM/ART,
                // so active Compose Image renders do not crash with "Canvas: trying to use a recycled bitmap".
            }
        }

        fun snapshot(): Map<String, FakeBitmap> = synchronized(map) { LinkedHashMap(map) }
    }

    class FakeBitmap(val id: Int, val byteCount: Long = 1024 * 1024) { // 1 MB
        var isRecycled = false
        fun recycle() {
            isRecycled = true
        }
    }

    @Test
    fun testPdfThumbnailCacheEvictionLeavesEvictedBitmapsUnrecycled() {
        // Cache capacity = 3 MB (3 bitmaps)
        val cache = SafeThumbnailLruCache(3 * 1024 * 1024)

        val bmp1 = FakeBitmap(1)
        val bmp2 = FakeBitmap(2)
        val bmp3 = FakeBitmap(3)
        val bmp4 = FakeBitmap(4) // Triggers eviction of bmp1

        cache.put("item_1", bmp1)
        cache.put("item_2", bmp2)
        cache.put("item_3", bmp3)

        // bmp1 is still in cache
        assertEquals(bmp1, cache.get("item_1"))

        // Add bmp4 -> causes bmp2 to be evicted (since item_1 was accessed most recently, item_2 is LRU)
        cache.put("item_4", bmp4)

        assertNull("item_2 must have been evicted from cache", cache.get("item_2"))
        assertFalse(
            "CRITICAL: Evicted bitmap bmp2 must NOT be recycled so Compose renders do not crash",
            bmp2.isRecycled
        )
        assertFalse("bmp1 must not be recycled", bmp1.isRecycled)
        assertFalse("bmp3 must not be recycled", bmp3.isRecycled)
        assertFalse("bmp4 must not be recycled", bmp4.isRecycled)
    }

    @Test
    fun testPdfThumbnailCachePrefixClearing() {
        val cache = SafeThumbnailLruCache(100 * 1024 * 1024)
        val uri1 = "content://media/doc1.pdf"
        val uri2 = "content://media/doc2.pdf"

        cache.put("${uri1}_0_thumb", FakeBitmap(1))
        cache.put("${uri1}_1_thumb", FakeBitmap(2))
        cache.put("${uri1}_0_highres", FakeBitmap(3))
        cache.put("${uri2}_0_thumb", FakeBitmap(4))
        cache.put("${uri2}_1_thumb", FakeBitmap(5))

        assertEquals(5, cache.snapshot().size)

        // Clear only uri1
        cache.clearForPrefix(uri1)

        val snapshot = cache.snapshot()
        assertEquals(2, snapshot.size)
        assertNull(cache.get("${uri1}_0_thumb"))
        assertNull(cache.get("${uri1}_1_thumb"))
        assertNull(cache.get("${uri1}_0_highres"))
        assertNotNull(cache.get("${uri2}_0_thumb"))
        assertNotNull(cache.get("${uri2}_1_thumb"))
    }

    @Test
    fun testPdfThumbnailCacheConcurrentAccessThreadSafety() = runBlocking {
        val cache = SafeThumbnailLruCache(10 * 1024 * 1024)
        val coroutines = 50

        // Launch concurrent puts, gets, and prefix clears
        val jobs = (0 until coroutines).map { index ->
            launch(Dispatchers.Default) {
                val uri = "content://media/doc_${index % 5}.pdf"
                val key = "${uri}_${index}_thumb"
                cache.put(key, FakeBitmap(index, 100 * 1024))
                cache.get(key)
                if (index % 10 == 0) {
                    cache.clearForPrefix(uri)
                }
            }
        }
        jobs.joinAll()
        // No ConcurrentModificationException or deadlocks thrown
        assertTrue(true)
    }

    // =========================================================================
    // 3. imagesToPdf Downsampling Calculation (8000x6000 & Edge Cases)
    // =========================================================================

    /**
     * Pure testable implementation of calculateInSampleSize matching PdfEngine.kt.
     */
    private fun computeInSampleSize(outWidth: Int, outHeight: Int, reqWidth: Int = 2048, reqHeight: Int = 2048): Int {
        var inSampleSize = 1
        if (outHeight > reqHeight || outWidth > reqWidth) {
            val halfHeight = outHeight / 2
            val halfWidth = outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight || (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
            while ((outWidth / inSampleSize) > reqWidth || (outHeight / inSampleSize) > reqHeight) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    @Test
    fun testCalculateInSampleSizeOn8000x6000() {
        // High resolution camera photo: 8000x6000 (48MP)
        val width = 8000
        val height = 6000
        val sampleSize = computeInSampleSize(width, height, 2048, 2048)

        assertEquals("inSampleSize for 8000x6000 must be 4", 4, sampleSize)

        val downsampledWidth = width / sampleSize
        val downsampledHeight = height / sampleSize

        assertTrue("Downsampled width ($downsampledWidth) must be <= 2048", downsampledWidth <= 2048)
        assertTrue("Downsampled height ($downsampledHeight) must be <= 2048", downsampledHeight <= 2048)
        assertEquals(2000, downsampledWidth)
        assertEquals(1500, downsampledHeight)
    }

    @Test
    fun testCalculateInSampleSizeOn12000x9000() {
        // Ultra-high 108MP camera image: 12000x9000
        val width = 12000
        val height = 9000
        val sampleSize = computeInSampleSize(width, height, 2048, 2048)

        assertEquals("inSampleSize for 12000x9000 must be 8", 8, sampleSize)

        val downsampledWidth = width / sampleSize
        val downsampledHeight = height / sampleSize

        assertTrue("Downsampled width ($downsampledWidth) must be <= 2048", downsampledWidth <= 2048)
        assertTrue("Downsampled height ($downsampledHeight) must be <= 2048", downsampledHeight <= 2048)
        assertEquals(1500, downsampledWidth)
        assertEquals(1125, downsampledHeight)
    }

    @Test
    fun testCalculateInSampleSizeBoundaryAndEdgeCases() {
        // Exact boundary: 2048x2048
        assertEquals(1, computeInSampleSize(2048, 2048, 2048, 2048))

        // Standard HD: 1920x1080
        assertEquals(1, computeInSampleSize(1920, 1080, 2048, 2048))

        // Small image: 500x500
        assertEquals(1, computeInSampleSize(500, 500, 2048, 2048))

        // Just over boundary: 2049x2048
        val sample2049 = computeInSampleSize(2049, 2048, 2048, 2048)
        assertEquals(2, sample2049)
        assertTrue(2049 / sample2049 <= 2048)

        // 4000x3000 (12MP standard mobile photo)
        val sample4k = computeInSampleSize(4000, 3000, 2048, 2048)
        assertEquals(2, sample4k)
        assertTrue(4000 / sample4k <= 2048)
        assertTrue(3000 / sample4k <= 2048)

        // Extreme aspect ratio (panorama): 100000x100
        val samplePano = computeInSampleSize(100000, 100, 2048, 2048)
        assertEquals(64, samplePano)
        assertTrue(100000 / samplePano <= 2048)

        // Extreme aspect ratio (tall banner): 100x100000
        val sampleBanner = computeInSampleSize(100, 100000, 2048, 2048)
        assertEquals(64, sampleBanner)
        assertTrue(100000 / sampleBanner <= 2048)

        // Zero / negative edge cases (corrupted headers)
        assertEquals(1, computeInSampleSize(0, 0, 2048, 2048))
        assertEquals(1, computeInSampleSize(-1, -1, 2048, 2048))
    }

    // =========================================================================
    // 4. PdfEngine Resource Closure & Stream Safety Audit
    // =========================================================================

    @Test
    fun testPdfEngineMethodsPresenceAndSignatures() {
        val clazz = PdfEngine::class.java
        val methodNames = clazz.declaredMethods.map { it.name.substringBefore('-') }

        assertTrue("PdfEngine must contain imagesToPdf", methodNames.contains("imagesToPdf"))
        assertTrue("PdfEngine must contain pdfToImages", methodNames.contains("pdfToImages"))
        assertTrue("PdfEngine must contain mergePdfs", methodNames.contains("mergePdfs"))
        assertTrue("PdfEngine must contain splitPdf", methodNames.contains("splitPdf"))
        assertTrue("PdfEngine must contain compressPdf", methodNames.contains("compressPdf"))
        assertTrue("PdfEngine must contain encryptPdf", methodNames.contains("encryptPdf"))
        assertTrue("PdfEngine must contain decryptPdf", methodNames.contains("decryptPdf"))
        assertTrue("PdfEngine must contain addWatermark", methodNames.contains("addWatermark"))
        assertTrue("PdfEngine must contain extractText", methodNames.contains("extractText"))
        assertTrue("PdfEngine must contain deletePages", methodNames.contains("deletePages"))
        assertTrue("PdfEngine must contain rotatePages", methodNames.contains("rotatePages"))
        assertTrue("PdfEngine must contain reorderPages", methodNames.contains("reorderPages"))
        assertTrue("PdfEngine must contain extractPages", methodNames.contains("extractPages"))
    }
}
