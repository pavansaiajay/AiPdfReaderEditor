package pavansaiajayx.aipdfreadereditor.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import pavansaiajayx.aipdfreadereditor.core.common.network.ApplicationScope
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.core.datastore.preferences.UserPreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val USER_PREFERENCES = "user_preferences"

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { context.preferencesDataStoreFile(USER_PREFERENCES) }
        )
    }

    @Provides
    @Singleton
    fun provideCreditManager(
        dataStore: DataStore<Preferences>
    ): CreditManager {
        return CreditManager(dataStore)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>
    ): UserPreferencesRepository {
        return UserPreferencesRepository(dataStore)
    }
}
