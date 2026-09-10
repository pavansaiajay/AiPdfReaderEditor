package pavansaiajayx.aipdfreadereditor.app.core.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.app.core.economy.CreditManager
import javax.inject.Inject

data class PreferencesState(
    val isLoading: Boolean = true,
    val isOnboardingCompleted: Boolean = false
)

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val creditManager: CreditManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreferencesState())
    val uiState: StateFlow<PreferencesState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasSeenOnboarding = creditManager.hasSeenOnboardingFlow.first()
            _uiState.value = PreferencesState(
                isLoading = false,
                isOnboardingCompleted = hasSeenOnboarding
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            creditManager.setHasSeenOnboarding()
            _uiState.value = _uiState.value.copy(isOnboardingCompleted = true)
        }
    }
}
