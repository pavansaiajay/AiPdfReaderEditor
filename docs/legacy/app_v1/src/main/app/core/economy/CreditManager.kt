package pavansaiajayx.aipdfreadereditor.app.core.economy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CreditManager(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val CREDITS_KEY = intPreferencesKey("user_credits")
        private const val DEFAULT_CREDITS = 0
        private val LAST_OPENED_DATE_KEY = androidx.datastore.preferences.core.longPreferencesKey("last_opened_date")
        private val HAS_SEEN_ONBOARDING_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("has_seen_onboarding")
    }

    val creditsFlow: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[CREDITS_KEY] ?: DEFAULT_CREDITS
        }

    val hasSeenOnboardingFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
        }

    suspend fun setHasSeenOnboarding() {
        dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = true
        }
    }

    suspend fun checkDailyWelcomeCredits() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
            dataStore.edit { preferences ->
                val lastOpenedDay = preferences[LAST_OPENED_DATE_KEY] ?: 0L
                if (currentDay != lastOpenedDay) {
                    val currentCredits = preferences[CREDITS_KEY] ?: DEFAULT_CREDITS
                    if (currentCredits < 5) {
                        preferences[CREDITS_KEY] = 5
                    }
                    preferences[LAST_OPENED_DATE_KEY] = currentDay
                }
            }
        }
    }

    suspend fun addCredits(amount: Int) {
        dataStore.edit { preferences ->
            val current = preferences[CREDITS_KEY] ?: DEFAULT_CREDITS
            preferences[CREDITS_KEY] = current + amount
        }
    }

    suspend fun deductCredits(amount: Int): Boolean {
        var success = false
        dataStore.edit { preferences ->
            val current = preferences[CREDITS_KEY] ?: DEFAULT_CREDITS
            if (current >= amount) {
                preferences[CREDITS_KEY] = current - amount
                success = true
            }
        }
        return success
    }
}
