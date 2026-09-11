package pavansaiajayx.aipdfreadereditor.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.core.datastore.preferences.UserPreferencesRepository
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 3,
    val isCompleting: Boolean = false
)

sealed interface OnboardingAction {
    data class PageChanged(val page: Int) : OnboardingAction
    data object NextClicked : OnboardingAction
    data object SkipClicked : OnboardingAction
    data object CompleteOnboarding : OnboardingAction
}

sealed interface OnboardingEvent {
    data object NavigateToHome : OnboardingEvent
    data class ScrollToPage(val page: Int) : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val creditManager: CreditManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.PageChanged -> {
                _uiState.update { it.copy(currentPage = action.page) }
            }
            is OnboardingAction.NextClicked -> {
                val current = _uiState.value.currentPage
                if (current < _uiState.value.totalPages - 1) {
                    viewModelScope.launch {
                        _events.send(OnboardingEvent.ScrollToPage(current + 1))
                    }
                } else {
                    complete()
                }
            }
            is OnboardingAction.SkipClicked -> {
                complete()
            }
            is OnboardingAction.CompleteOnboarding -> {
                complete()
            }
        }
    }

    private fun complete() {
        if (_uiState.value.isCompleting) return
        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            userPreferencesRepository.setHasSeenOnboarding(true)
            creditManager.checkDailyWelcomeCredits()
            _events.send(OnboardingEvent.NavigateToHome)
        }
    }
}
