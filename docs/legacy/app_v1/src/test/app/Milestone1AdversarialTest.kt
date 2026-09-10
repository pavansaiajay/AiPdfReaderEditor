package pavansaiajayx.aipdfreadereditor.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.app.core.economy.CreditManager

/**
 * Thread-safe In-Memory DataStore for testing CreditManager logic without host FS/OS dependencies.
 */
class InMemoryPreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val data: Flow<Preferences> = state.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return mutex.withLock {
            val updated = transform(state.value)
            state.value = updated
            updated
        }
    }
}

class Milestone1AdversarialTest {

    private fun createTestCreditManager(initialCredits: Int = 0): CreditManager {
        val store = InMemoryPreferencesDataStore()
        val manager = CreditManager(store)
        if (initialCredits > 0) {
            runBlocking {
                manager.addCredits(initialCredits)
            }
        }
        return manager
    }

    @Test
    fun testInitialCreditsZero() = runBlocking {
        val creditManager = createTestCreditManager()
        val credits = creditManager.creditsFlow.first()
        assertEquals("Initial credits must be 0", 0, credits)
    }

    @Test
    fun testDeductCreditsWhenZeroFails() = runBlocking {
        val creditManager = createTestCreditManager(0)
        val result = creditManager.deductCredits(1)
        assertFalse("Deducting 1 credit when balance is 0 must return false", result)
        assertEquals("Credits balance must remain 0 after failed deduction", 0, creditManager.creditsFlow.first())
    }

    @Test
    fun testDeductCreditsInsufficientForSummarize() = runBlocking {
        val creditManager = createTestCreditManager(4)
        val result = creditManager.deductCredits(5)
        assertFalse("Deducting 5 credits when balance is 4 must return false", result)
        assertEquals("Credits balance must remain 4 after failed deduction", 4, creditManager.creditsFlow.first())
    }

    @Test
    fun testAddAndDeductCreditsSucceeds() = runBlocking {
        val creditManager = createTestCreditManager(0)
        creditManager.addCredits(5)
        assertEquals("Credits balance should be 5 after ad reward", 5, creditManager.creditsFlow.first())

        val deduct1 = creditManager.deductCredits(1)
        assertTrue("Deducting 1 credit from 5 should succeed", deduct1)
        assertEquals("Credits balance should be 4", 4, creditManager.creditsFlow.first())

        val deduct5 = creditManager.deductCredits(5)
        assertFalse("Deducting 5 credits from 4 should fail", deduct5)
        assertEquals("Credits balance should still be 4", 4, creditManager.creditsFlow.first())

        val deduct4 = creditManager.deductCredits(4)
        assertTrue("Deducting 4 credits from 4 should succeed", deduct4)
        assertEquals("Credits balance should be 0", 0, creditManager.creditsFlow.first())
    }

    @Test
    fun testConcurrentCreditDeductionSafety() = runBlocking(Dispatchers.IO) {
        val creditManager = createTestCreditManager(5)

        // Launch 20 concurrent deduction attempts of 1 credit each
        val jobs = (1..20).map {
            async {
                creditManager.deductCredits(1)
            }
        }
        val results = jobs.awaitAll()

        val successCount = results.count { it }
        val failureCount = results.count { !it }

        assertEquals("Exactly 5 deductions must succeed from initial 5 credits", 5, successCount)
        assertEquals("Exactly 15 deductions must fail", 15, failureCount)
        assertEquals("Final credit balance must be exactly 0 (no negative balance or race leak)", 0, creditManager.creditsFlow.first())
    }

    @Test
    fun testDailyWelcomeCreditsCap() = runBlocking {
        val creditManager = createTestCreditManager(0)
        // Balance = 0 -> should top up to 5
        creditManager.checkDailyWelcomeCredits()
        assertEquals("Welcome credits should top up to 5 on first day open", 5, creditManager.creditsFlow.first())

        // If user already has 10 credits -> should not decrease to 5
        creditManager.addCredits(5)
        assertEquals(10, creditManager.creditsFlow.first())
        creditManager.checkDailyWelcomeCredits()
        assertEquals("Welcome credits should not reduce existing credits balance > 5", 10, creditManager.creditsFlow.first())
    }

    @Test
    fun verifyOfflineViewModelsDoNotInjectOrDependOnCreditManager() {
        val offlineClasses = listOf(
            pavansaiajayx.aipdfreadereditor.app.ui.tools.MergePdfViewModel::class.java,
            pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.DeletePagesViewModel::class.java,
            pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.ExtractPagesViewModel::class.java,
            pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.SplitPdfViewModel::class.java
        )

        for (clazz in offlineClasses) {
            val constructors = clazz.declaredConstructors
            for (constructor in constructors) {
                for (paramType in constructor.parameterTypes) {
                    assertNotEquals(
                        "Class ${clazz.simpleName} must not have CreditManager in constructor parameters",
                        CreditManager::class.java,
                        paramType
                    )
                }
            }

            for (field in clazz.declaredFields) {
                assertNotEquals(
                    "Class ${clazz.simpleName} must not contain CreditManager fields",
                    CreditManager::class.java,
                    field.type
                )
            }
        }
    }

    @Test
    fun verifyMviStateContractsForMonetization() {
        // Verify PdfToolsState.InsufficientCredits exists and is a singleton object
        val insufficientCreditsState: pavansaiajayx.aipdfreadereditor.app.ui.tools.PdfToolsState =
            pavansaiajayx.aipdfreadereditor.app.ui.tools.PdfToolsState.InsufficientCredits
        assertNotNull(insufficientCreditsState)

        // Verify PdfChatState defaults showInsufficientCreditsDialog to false
        val chatState = pavansaiajayx.aipdfreadereditor.app.ui.chat.PdfChatState()
        assertFalse(chatState.showInsufficientCreditsDialog)

        // Verify PdfViewerState defaults showInsufficientCreditsDialog to false
        val viewerState = pavansaiajayx.aipdfreadereditor.app.ui.viewer.PdfViewerState()
        assertFalse(viewerState.showInsufficientCreditsDialog)
    }

    @Test
    fun verifyScanDocumentCompletedIntent() {
        val scanIntent = pavansaiajayx.aipdfreadereditor.app.ui.tools.PdfToolsIntent.ScanDocumentCompleted("content://pdf/123")
        assertEquals("content://pdf/123", scanIntent.pdfUri)
    }
}
