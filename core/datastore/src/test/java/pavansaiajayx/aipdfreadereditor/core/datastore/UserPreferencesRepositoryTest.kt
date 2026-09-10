package pavansaiajayx.aipdfreadereditor.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.datastore.preferences.UserPreferencesRepository

class UserPreferencesRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setup() {
        dataStore = InMemoryPreferencesDataStore()
        repository = UserPreferencesRepository(dataStore)
    }

    @Test
    fun defaultOnboardingHasNotBeenSeen() = runTest {
        assertFalse(repository.hasSeenOnboardingFlow.first())
    }

    @Test
    fun setHasSeenOnboardingUpdatesFlow() = runTest {
        repository.setHasSeenOnboarding(true)
        assertTrue(repository.hasSeenOnboardingFlow.first())

        repository.setHasSeenOnboarding(false)
        assertFalse(repository.hasSeenOnboardingFlow.first())
    }

    @Test
    fun defaultThemeModeIsSystem() = runTest {
        assertEquals("system", repository.themeModeFlow.first())
    }

    @Test
    fun setThemeModeUpdatesFlow() = runTest {
        repository.setThemeMode("dark")
        assertEquals("dark", repository.themeModeFlow.first())

        repository.setThemeMode("light")
        assertEquals("light", repository.themeModeFlow.first())
    }
}
