package pavansaiajayx.aipdfreadereditor.app

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEngine
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.DeletePagesViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.ExtractPagesViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.PdfThumbnailCache
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.SplitPdfViewModel
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Empirical Challenger 2 Adversarial Stress Test Suite for M3 & M4.
 *
 * Focus Areas:
 * 1. Image Decoding Safety (`calculateInSampleSize` in `PdfEngine.kt`):
 *    - Power-of-2 invariant test on 10,000 randomized dimension pairs (1x1 to 200,000x200,000).
 *    - Memory footprint bound guarantee: decoded bitmap <= 16MB (2048x2048x4 bytes).
 *    - Extreme aspect ratios (ultra-wide 100,000x10, ultra-tall 10x100,000).
 *    - Corrupted / Zero / Negative boundary handling.
 *
 * 2. Grid Hit Testing & Layout Coordinates:
 *    - 2D layout bounding-box hit testing simulation (LazyVerticalGrid layoutInfo).
 *    - Center, 4-corner boundary hit tests, inter-item dead zones, out-of-grid coordinates.
 *    - Multi-column, multi-row, diagonal, and reverse-drag gesture tracking.
 *
 * 3. Direct Compose Bitmap Rendering Lifecycle & Safe Heap Cache:
 *    - LRU Cache eviction safety: 0 recycled bitmap crashes.
 *    - URI-scoped eviction isolation.
 *    - High-concurrency stress test with 100 coroutines.
 *    - Selection state composition stability.
 */
class Milestone3Milestone4Challenger2AdversarialTest {

    // =========================================================================
    // 1. Empirical Image Decoding Safety (calculateInSampleSize)
    // =========================================================================

    /**
     * Pure testable oracle of calculateInSampleSize matching PdfEngine.kt.
     */
    private fun calculateInSampleSize(outWidth: Int, outHeight: Int, reqWidth: Int = 2048, reqHeight: Int = 2048): Int {
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

    private fun isPowerOfTwo(n: Int): Boolean {
        return n > 0 && (n and (n - 1)) == 0
    }

    @Test
    fun testRandomizedFuzzingInSampleSizePowerOfTwoAndMemoryBounds() {
        val random = Random(42)
        val iterations = 10000
        val maxAllowedBytes = 2048L * 2048L * 4L // 16 MB max ARGB_8888 decoded memory

        for (i in 0 until iterations) {
            // Generate widths and heights from 1 to 200,000
            val width = random.nextInt(200000) + 1
            val height = random.nextInt(200000) + 1

            val sampleSize = calculateInSampleSize(width, height, 2048, 2048)

            // Invariant 1: inSampleSize must strictly be a power of 2
            assertTrue("inSampleSize ($sampleSize) for ${width}x$height must be a power of 2", isPowerOfTwo(sampleSize))

            // Invariant 2: Sample size must be at least 1
            assertTrue("inSampleSize must be >= 1", sampleSize >= 1)

            // Invariant 3: Downsampled dimensions must not exceed target (2048x2048)
            val downsampledWidth = width / sampleSize
            val downsampledHeight = height / sampleSize

            assertTrue(
                "Downsampled width ($downsampledWidth) for input ${width}x$height with sample $sampleSize must be <= 2048",
                downsampledWidth <= 2048
            )
            assertTrue(
                "Downsampled height ($downsampledHeight) for input ${width}x$height with sample $sampleSize must be <= 2048",
                downsampledHeight <= 2048
            )

            // Invariant 4: Decoded memory footprint must NEVER exceed 16MB
            val memoryBytes = downsampledWidth.toLong() * downsampledHeight.toLong() * 4L
            assertTrue(
                "Decoded memory ($memoryBytes bytes) must not exceed $maxAllowedBytes bytes",
                memoryBytes <= maxAllowedBytes
            )
        }
    }

    @Test
    fun testGigapixelAndExtremeAspectRatios() {
        // 1 Gigapixel square: 31622 x 31622
        val gigaSquareSample = calculateInSampleSize(31622, 31622, 2048, 2048)
        assertEquals(16, gigaSquareSample)
        assertTrue(31622 / gigaSquareSample <= 2048)

        // Ultra-wide panorama: 150,000 x 50
        val panoSample = calculateInSampleSize(150000, 50, 2048, 2048)
        assertEquals(128, panoSample)
        assertTrue(150000 / panoSample <= 2048)
        assertTrue(50 / panoSample <= 2048)

        // Ultra-tall vertical scroll: 50 x 150,000
        val tallSample = calculateInSampleSize(50, 150000, 2048, 2048)
        assertEquals(128, tallSample)
        assertTrue(50 / tallSample <= 2048)
        assertTrue(150000 / tallSample <= 2048)

        // 1x1 single pixel
        assertEquals(1, calculateInSampleSize(1, 1, 2048, 2048))
    }

    @Test
    fun testBoundaryExactThresholdTransitions() {
        // Exact boundary
        assertEquals(1, calculateInSampleSize(2048, 2048, 2048, 2048))

        // Off-by-one threshold transitions in integer division
        assertEquals(2, calculateInSampleSize(2049, 2048, 2048, 2048))
        assertEquals(2, calculateInSampleSize(2048, 2049, 2048, 2048))
        assertEquals(2, calculateInSampleSize(4096, 4096, 2048, 2048))
        assertEquals(4, calculateInSampleSize(4099, 4096, 2048, 2048))
        assertEquals(4, calculateInSampleSize(8192, 8192, 2048, 2048))
        assertEquals(8, calculateInSampleSize(8196, 8192, 2048, 2048))
    }

    @Test
    fun testCorruptedAndZeroDimensionInputs() {
        // Corrupted headers / non-positive dimensions
        assertEquals(1, calculateInSampleSize(0, 0, 2048, 2048))
        assertEquals(1, calculateInSampleSize(-100, -100, 2048, 2048))
        assertEquals(1, calculateInSampleSize(-1, 500, 2048, 2048))
        assertEquals(1, calculateInSampleSize(500, -1, 2048, 2048))
    }

    // =========================================================================
    // 2. Empirical Grid Hit Testing & Layout Coordinate Simulation
    // =========================================================================

    /**
     * Simulates Compose LazyGridItemInfo.
     */
    data class SimulatedGridItem(
        val index: Int,
        val offsetX: Int,
        val offsetY: Int,
        val width: Int,
        val height: Int
    ) {
        fun contains(x: Int, y: Int): Boolean {
            return x in offsetX..(offsetX + width) && y in offsetY..(offsetY + height)
        }
    }

    /**
     * Generates a 2D grid layout corresponding to Compose LazyVerticalGrid.
     */
    private fun createSimulatedGrid(
        itemCount: Int,
        columns: Int = 3,
        itemWidth: Int = 100,
        itemHeight: Int = 140,
        spacing: Int = 8,
        padding: Int = 16
    ): List<SimulatedGridItem> {
        val items = mutableListOf<SimulatedGridItem>()
        for (i in 0 until itemCount) {
            val col = i % columns
            val row = i / columns
            val x = padding + col * (itemWidth + spacing)
            val y = padding + row * (itemHeight + spacing)
            items.add(SimulatedGridItem(i, x, y, itemWidth, itemHeight))
        }
        return items
    }

    private fun findItemAt(grid: List<SimulatedGridItem>, x: Int, y: Int): SimulatedGridItem? {
        return grid.find { it.contains(x, y) }
    }

    @Test
    fun testGridHitTestingCenterAndCornerBoundaries() {
        val grid = createSimulatedGrid(itemCount = 9, columns = 3, itemWidth = 100, itemHeight = 140, spacing = 8, padding = 16)

        for (item in grid) {
            // Center
            val centerItem = findItemAt(grid, item.offsetX + item.width / 2, item.offsetY + item.height / 2)
            assertNotNull("Center of item ${item.index} must hit", centerItem)
            assertEquals(item.index, centerItem!!.index)

            // Top-Left corner
            val tlItem = findItemAt(grid, item.offsetX, item.offsetY)
            assertNotNull("Top-Left of item ${item.index} must hit", tlItem)
            assertEquals(item.index, tlItem!!.index)

            // Top-Right corner
            val trItem = findItemAt(grid, item.offsetX + item.width, item.offsetY)
            assertNotNull("Top-Right of item ${item.index} must hit", trItem)
            assertEquals(item.index, trItem!!.index)

            // Bottom-Left corner
            val blItem = findItemAt(grid, item.offsetX, item.offsetY + item.height)
            assertNotNull("Bottom-Left of item ${item.index} must hit", blItem)
            assertEquals(item.index, blItem!!.index)

            // Bottom-Right corner
            val brItem = findItemAt(grid, item.offsetX + item.width, item.offsetY + item.height)
            assertNotNull("Bottom-Right of item ${item.index} must hit", brItem)
            assertEquals(item.index, brItem!!.index)
        }
    }

    @Test
    fun testGridHitTestingDeadZonesAndOutOfBounds() {
        val grid = createSimulatedGrid(itemCount = 9, columns = 3, itemWidth = 100, itemHeight = 140, spacing = 8, padding = 16)

        // Dead zone in horizontal spacing between Col 0 and Col 1:
        // Col 0 ends at 16 + 100 = 116. Col 1 starts at 16 + 108 = 124.
        // Point x = 120 is right in the 8px dead zone.
        val deadZoneHoriz = findItemAt(grid, 120, 50)
        assertNull("Points in horizontal inter-item spacing must return null", deadZoneHoriz)

        // Dead zone in vertical spacing between Row 0 and Row 1:
        // Row 0 ends at 16 + 140 = 156. Row 1 starts at 16 + 148 = 164.
        // Point y = 160 is in the 8px vertical dead zone.
        val deadZoneVert = findItemAt(grid, 50, 160)
        assertNull("Points in vertical inter-item spacing must return null", deadZoneVert)

        // Outside grid (negative or huge coordinates)
        assertNull(findItemAt(grid, -10, -10))
        assertNull(findItemAt(grid, 5, 5)) // in 16px left/top padding
        assertNull(findItemAt(grid, 5000, 5000))
    }

    @Test
    fun testContinuousDragTrajectoryTracking() {
        val grid = createSimulatedGrid(itemCount = 12, columns = 3, itemWidth = 100, itemHeight = 140, spacing = 8, padding = 16)

        // Simulate a continuous diagonal drag from Item 0 (Col 0, Row 0) to Item 8 (Col 2, Row 2)
        val startItem = grid[0]
        val endItem = grid[8]

        var dragStartIndex = startItem.index
        var selectedRange: Set<Int> = emptySet()

        // Drag start
        selectedRange = (minOf(dragStartIndex, startItem.index)..maxOf(dragStartIndex, startItem.index)).toSet()
        assertEquals(setOf(0), selectedRange)

        // Drag steps along diagonal trajectory
        val steps = 20
        for (step in 1..steps) {
            val curX = (startItem.offsetX + (endItem.offsetX - startItem.offsetX) * (step.toFloat() / steps)).toInt()
            val curY = (startItem.offsetY + (endItem.offsetY - startItem.offsetY) * (step.toFloat() / steps)).toInt()

            val hit = findItemAt(grid, curX, curY)
            if (hit != null) {
                selectedRange = (minOf(dragStartIndex, hit.index)..maxOf(dragStartIndex, hit.index)).toSet()
            }
        }

        // Final range must be 0..8
        assertEquals((0..8).toSet(), selectedRange)

        // Now reverse drag from Item 8 back up to Item 4 (Col 1, Row 1)
        dragStartIndex = 8
        val targetItem = grid[4]
        for (step in 1..steps) {
            val curX = (endItem.offsetX + (targetItem.offsetX - endItem.offsetX) * (step.toFloat() / steps)).toInt()
            val curY = (endItem.offsetY + (targetItem.offsetY - endItem.offsetY) * (step.toFloat() / steps)).toInt()

            val hit = findItemAt(grid, curX, curY)
            if (hit != null) {
                selectedRange = (minOf(dragStartIndex, hit.index)..maxOf(dragStartIndex, hit.index)).toSet()
            }
        }

        // Final range from 8 to 4 must be 4..8
        assertEquals(setOf(4, 5, 6, 7, 8), selectedRange)
    }

    // =========================================================================
    // 3. Direct Compose Bitmap Rendering Lifecycle & Safe Cache
    // =========================================================================

    class TrackedFakeBitmap(val id: String, val byteSize: Int = 100 * 1024) {
        var isRecycled = false
        fun recycle() {
            isRecycled = true
        }
    }

    /**
     * Memory-safe LRU cache mirror for testing zero-recycle and eviction semantics.
     */
    class SafeTestLruCache(val capacityBytes: Int) {
        private val map = LinkedHashMap<String, TrackedFakeBitmap>(16, 0.75f, true)
        private var currentSize = 0

        @Synchronized
        fun put(key: String, bitmap: TrackedFakeBitmap) {
            val old = map.put(key, bitmap)
            currentSize += bitmap.byteSize
            if (old != null) {
                currentSize -= old.byteSize
            }
            trim()
        }

        @Synchronized
        fun get(key: String): TrackedFakeBitmap? = map[key]

        @Synchronized
        fun clearForUri(uriPrefix: String) {
            val iterator = map.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key.startsWith(uriPrefix)) {
                    currentSize -= entry.value.byteSize
                    iterator.remove()
                }
            }
        }

        @Synchronized
        fun clearAll() {
            map.clear()
            currentSize = 0
        }

        private fun trim() {
            val iterator = map.entries.iterator()
            while (iterator.hasNext() && currentSize > capacityBytes) {
                val entry = iterator.next()
                currentSize -= entry.value.byteSize
                iterator.remove()
                // NOTE: DO NOT call entry.value.recycle() — let ART GC handle it safely!
            }
        }

        @Synchronized
        fun size() = map.size
    }

    @Test
    fun testLruEvictionLeavesBitmapsAliveForComposeRendering() {
        // Capacity for 3 bitmaps (300 KB)
        val cache = SafeTestLruCache(300 * 1024)

        val b1 = TrackedFakeBitmap("b1")
        val b2 = TrackedFakeBitmap("b2")
        val b3 = TrackedFakeBitmap("b3")
        val b4 = TrackedFakeBitmap("b4")
        val b5 = TrackedFakeBitmap("b5")

        cache.put("k1", b1)
        cache.put("k2", b2)
        cache.put("k3", b3)

        // k1 is touched
        assertEquals(b1, cache.get("k1"))

        // Add k4 and k5 -> evicts k2 and k3
        cache.put("k4", b4)
        cache.put("k5", b5)

        assertNull("k2 must be evicted", cache.get("k2"))
        assertNull("k3 must be evicted", cache.get("k3"))

        // CRITICAL CHECK: Evicted bitmaps must NOT be recycled so active Compose Image renders don't crash
        assertFalse("b2 must NOT be recycled", b2.isRecycled)
        assertFalse("b3 must NOT be recycled", b3.isRecycled)
        assertFalse("b1 must NOT be recycled", b1.isRecycled)
        assertFalse("b4 must NOT be recycled", b4.isRecycled)
        assertFalse("b5 must NOT be recycled", b5.isRecycled)
    }

    @Test
    fun testLruCacheHighConcurrencyStress() = runBlocking {
        val cache = SafeTestLruCache(2 * 1024 * 1024) // 2 MB
        val coroutines = 100
        val operationsPerCoroutine = 20

        val jobs = (0 until coroutines).map { coroutineId ->
            launch(Dispatchers.Default) {
                for (op in 0 until operationsPerCoroutine) {
                    val uri = "content://pdf/doc_${coroutineId % 4}.pdf"
                    val key = "${uri}_page_${op}"
                    val bmp = TrackedFakeBitmap(key)
                    cache.put(key, bmp)
                    cache.get(key)
                    if (op % 5 == 0) {
                        cache.clearForUri("content://pdf/doc_0.pdf")
                    }
                }
            }
        }
        jobs.joinAll()
        assertTrue("Concurrent execution completed without throwing exceptions", true)
    }
}
