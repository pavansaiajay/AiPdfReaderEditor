package pavansaiajayx.aipdfreadereditor.core.datastore.di

import org.junit.Assert.assertNotNull
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.datastore.InMemoryPreferencesDataStore

class DataStoreModuleTest {

    @Test
    fun provideCreditManagerProvidesNonNullInstance() {
        val testStore = InMemoryPreferencesDataStore()
        val manager = DataStoreModule.provideCreditManager(testStore)
        assertNotNull(manager)
    }

    @Test
    fun provideUserPreferencesRepositoryProvidesNonNullInstance() {
        val testStore = InMemoryPreferencesDataStore()
        val repository = DataStoreModule.provideUserPreferencesRepository(testStore)
        assertNotNull(repository)
    }
}
