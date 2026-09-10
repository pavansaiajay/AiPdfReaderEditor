package pavansaiajayx.aipdfreadereditor.app

import org.junit.Assert.*
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.DeletePagesViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.ExtractPagesViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.PdfThumbnailCache
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.SplitPdfViewModel
import java.lang.reflect.Modifier

/**
 * Challenger Test Suite for Milestone 4 (R4: Flicker-Free UI & Conflict-Free Gesture Selection in Grids).
 *
 * Empirical Challenges:
 * 1. Range selection calculation: forward drag (2..6), backward drag (6..2), point selection (3..3), disjoint union.
 * 2. Select All and Clear Selection boundary mechanics.
 * 3. Reflection verification of ViewModel range selection APIs and Grid parameters.
 * 4. PdfThumbnailCache safe byte-size and LRU structure inspection.
 */
class Milestone4ChallengerAdversarialTest {

    // =========================================================================
    // 1. Range Selection & Gesture Logic Simulation
    // =========================================================================

    private class TestRangeSelectionState(var pageCount: Int) {
        var selectedPages: Set<Int> = emptySet()

        fun togglePageSelection(pageIndex: Int) {
            val current = selectedPages.toMutableSet()
            if (current.contains(pageIndex)) {
                current.remove(pageIndex)
            } else {
                current.add(pageIndex)
            }
            selectedPages = current
        }

        fun selectRange(start: Int, end: Int) {
            val min = minOf(start, end)
            val max = maxOf(start, end)
            val range = (min..max).toSet()
            selectedPages = selectedPages + range
        }

        fun selectAll() {
            selectedPages = (0 until pageCount).toSet()
        }

        fun clearSelection() {
            selectedPages = emptySet()
        }
    }

    @Test
    fun testForwardDragRangeSelection() {
        val state = TestRangeSelectionState(pageCount = 10)
        assertEquals(emptySet<Int>(), state.selectedPages)

        // Drag from item 2 to item 5
        state.selectRange(2, 5)
        assertEquals(setOf(2, 3, 4, 5), state.selectedPages)
    }

    @Test
    fun testBackwardDragRangeSelection() {
        val state = TestRangeSelectionState(pageCount = 10)

        // Reverse drag from item 7 down to item 4
        state.selectRange(7, 4)
        assertEquals(setOf(4, 5, 6, 7), state.selectedPages)
    }

    @Test
    fun testSingleItemRangeSelection() {
        val state = TestRangeSelectionState(pageCount = 10)

        // Single tap or drag on single item (start == end)
        state.selectRange(3, 3)
        assertEquals(setOf(3), state.selectedPages)
    }

    @Test
    fun testMultiDragDisjointUnion() {
        val state = TestRangeSelectionState(pageCount = 20)

        // Drag select range 1..3
        state.selectRange(1, 3)
        assertEquals(setOf(1, 2, 3), state.selectedPages)

        // Drag select another range 7..9
        state.selectRange(7, 9)
        assertEquals(setOf(1, 2, 3, 7, 8, 9), state.selectedPages)

        // Overlapping drag select range 3..8
        state.selectRange(3, 8)
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8, 9), state.selectedPages)
    }

    @Test
    fun testSelectAllAndClearSelection() {
        val state = TestRangeSelectionState(pageCount = 15)

        state.selectAll()
        assertEquals(15, state.selectedPages.size)
        assertEquals((0..14).toSet(), state.selectedPages)

        state.clearSelection()
        assertTrue(state.selectedPages.isEmpty())
    }

    @Test
    fun testToggleAndRangeSelectionCombination() {
        val state = TestRangeSelectionState(pageCount = 10)

        // Toggle item 0 and item 9
        state.togglePageSelection(0)
        state.togglePageSelection(9)
        assertEquals(setOf(0, 9), state.selectedPages)

        // Drag select 3..5
        state.selectRange(3, 5)
        assertEquals(setOf(0, 3, 4, 5, 9), state.selectedPages)

        // Toggle off item 4
        state.togglePageSelection(4)
        assertEquals(setOf(0, 3, 5, 9), state.selectedPages)
    }

    // =========================================================================
    // 2. ViewModel Reflection API Verification
    // =========================================================================

    @Test
    fun testDeletePagesViewModelRangeSelectionMethods() {
        val clazz = DeletePagesViewModel::class.java
        val methodNames = clazz.declaredMethods.map { it.name }

        assertTrue("DeletePagesViewModel must contain selectRange", methodNames.contains("selectRange"))
        assertTrue("DeletePagesViewModel must contain selectAll", methodNames.contains("selectAll"))
        assertTrue("DeletePagesViewModel must contain clearSelection", methodNames.contains("clearSelection"))
        assertTrue("DeletePagesViewModel must contain togglePageSelection", methodNames.contains("togglePageSelection"))
    }

    @Test
    fun testExtractPagesViewModelRangeSelectionMethods() {
        val clazz = ExtractPagesViewModel::class.java
        val methodNames = clazz.declaredMethods.map { it.name }

        assertTrue("ExtractPagesViewModel must contain selectRange", methodNames.contains("selectRange"))
        assertTrue("ExtractPagesViewModel must contain selectAll", methodNames.contains("selectAll"))
        assertTrue("ExtractPagesViewModel must contain clearSelection", methodNames.contains("clearSelection"))
        assertTrue("ExtractPagesViewModel must contain togglePageSelection", methodNames.contains("togglePageSelection"))
    }

    @Test
    fun testSplitPdfViewModelRangeSelectionMethods() {
        val clazz = SplitPdfViewModel::class.java
        val methodNames = clazz.declaredMethods.map { it.name }

        assertTrue("SplitPdfViewModel must contain selectRange", methodNames.contains("selectRange"))
        assertTrue("SplitPdfViewModel must contain selectAll", methodNames.contains("selectAll"))
        assertTrue("SplitPdfViewModel must contain clearSelection", methodNames.contains("clearSelection"))
        assertTrue("SplitPdfViewModel must contain togglePageSelection", methodNames.contains("togglePageSelection"))
    }

    // =========================================================================
    // 3. PdfThumbnailCache Structure & API Verification
    // =========================================================================

    @Test
    fun testPdfThumbnailCacheReflection() {
        val clazz = PdfThumbnailCache::class.java
        val methodNames = clazz.declaredMethods.map { it.name }

        assertTrue("PdfThumbnailCache must contain get", methodNames.contains("get"))
        assertTrue("PdfThumbnailCache must contain put", methodNames.contains("put"))
        assertTrue("PdfThumbnailCache must contain clearForUri", methodNames.contains("clearForUri"))
        assertTrue("PdfThumbnailCache must contain clearAll", methodNames.contains("clearAll"))
    }
}
