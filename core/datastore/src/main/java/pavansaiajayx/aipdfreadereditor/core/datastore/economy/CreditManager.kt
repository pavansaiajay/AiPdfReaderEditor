package pavansaiajayx.aipdfreadereditor.core.datastore.economy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pavansaiajayx.aipdfreadereditor.core.model.CreditReason
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val CREDITS_KEY = intPreferencesKey("user_credits")
        val LAST_OPENED_DATE_KEY = longPreferencesKey("last_opened_date")
        const val DEFAULT_CREDITS = 0
        const val WELCOME_CREDITS_THRESHOLD = 5
    }

    val creditsFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[CREDITS_KEY] ?: DEFAULT_CREDITS
    }

    suspend fun addCredits(amount: Int, reason: CreditReason = CreditReason.ManualAdjustment) {
        if (amount <= 0) return
        dataStore.edit { preferences ->
            val current = preferences[CREDITS_KEY] ?: DEFAULT_CREDITS
            preferences[CREDITS_KEY] = current + amount
        }
    }

    suspend fun deductCredits(
        amount: Int,
        reason: CreditReason = CreditReason.ManualAdjustment
    ): Boolean {
        if (amount <= 0) return true
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

    suspend fun checkDailyWelcomeCredits(): Boolean {
        var applied = false
        val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        dataStore.edit { preferences ->
            val lastOpenedDay = preferences[LAST_OPENED_DATE_KEY] ?: 0L
            if (currentDay != lastOpenedDay) {
                val currentCredits = preferences[CREDITS_KEY] ?: DEFAULT_CREDITS
                if (currentCredits < WELCOME_CREDITS_THRESHOLD) {
                    preferences[CREDITS_KEY] = WELCOME_CREDITS_THRESHOLD
                    applied = true
                }
                preferences[LAST_OPENED_DATE_KEY] = currentDay
            }
        }
        return applied
    }
}
