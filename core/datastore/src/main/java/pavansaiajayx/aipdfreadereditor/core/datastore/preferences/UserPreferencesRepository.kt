package pavansaiajayx.aipdfreadereditor.core.datastore.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        const val DEFAULT_THEME_MODE = "system"
    }

    val hasSeenOnboardingFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    val themeModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: DEFAULT_THEME_MODE
    }

    suspend fun setHasSeenOnboarding(hasSeen: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = hasSeen
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }
}
