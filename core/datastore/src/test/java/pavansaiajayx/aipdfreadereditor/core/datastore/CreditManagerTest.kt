package pavansaiajayx.aipdfreadereditor.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.core.model.CreditReason

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

class CreditManagerTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var creditManager: CreditManager

    @Before
    fun setup() {
        dataStore = InMemoryPreferencesDataStore()
        creditManager = CreditManager(dataStore)
    }

    @Test
    fun initialCreditsBalanceIsZeroByDefault() = runTest {
        assertEquals(0, creditManager.creditsFlow.first())
    }

    @Test
    fun deductCreditsWhenInsufficientReturnsFalseAndLeavesBalanceUnchanged() = runTest {
        val result = creditManager.deductCredits(1)
        assertFalse("Deduction must fail when balance is 0", result)
        assertEquals(0, creditManager.creditsFlow.first())

        creditManager.addCredits(4)
        val resultOver = creditManager.deductCredits(5)
        assertFalse("Deduction must fail when balance is less than required", resultOver)
        assertEquals(4, creditManager.creditsFlow.first())
    }

    @Test
    fun deductCreditsWhenSufficientReturnsTrueAndDecrementsBalance() = runTest {
        creditManager.addCredits(10)
        val success = creditManager.deductCredits(4, CreditReason.AiChatQuery)

        assertTrue(success)
        assertEquals(6, creditManager.creditsFlow.first())
    }

    @Test
    fun addCreditsIncrementsBalanceCorrectly() = runTest {
        creditManager.addCredits(5, CreditReason.RewardedAdWatch)
        assertEquals(5, creditManager.creditsFlow.first())

        creditManager.addCredits(3)
        assertEquals(8, creditManager.creditsFlow.first())
    }

    @Test
    fun concurrentDeductionsAreThreadSafeAndNeverGoNegative() = runTest {
        creditManager.addCredits(5)

        // Launch 10 concurrent deductions of 1 credit each, only 5 should succeed
        val deferreds = (1..10).map {
            async {
                creditManager.deductCredits(1)
            }
        }

        val results = deferreds.awaitAll()
        val successCount = results.count { it }
        val failureCount = results.count { !it }

        assertEquals("Exactly 5 deductions should succeed", 5, successCount)
        assertEquals("Exactly 5 deductions should fail", 5, failureCount)
        assertEquals("Final credit balance must be exactly 0", 0, creditManager.creditsFlow.first())
    }

    @Test
    fun checkDailyWelcomeCreditsTopsUpToFiveOnFirstDay() = runTest {
        val applied = creditManager.checkDailyWelcomeCredits()
        assertTrue("Welcome bonus should apply on first launch", applied)
        assertEquals("Welcome credits should top up to 5", 5, creditManager.creditsFlow.first())
    }

    @Test
    fun checkDailyWelcomeCreditsDoesNotReduceBalanceAboveFive() = runTest {
        creditManager.addCredits(10)
        assertEquals(10, creditManager.creditsFlow.first())

        val applied = creditManager.checkDailyWelcomeCredits()
        assertFalse("Welcome bonus should not overwrite higher balance", applied)
        assertEquals(10, creditManager.creditsFlow.first())
    }
}
