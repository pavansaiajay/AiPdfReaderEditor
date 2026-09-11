package pavansaiajayx.aipdfreadereditor.app

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pavansaiajayx.aipdfreadereditor.core.datastore.preferences.UserPreferencesRepository
import pavansaiajayx.aipdfreadereditor.core.navigation.Screen
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(
        val startDestination: Screen,
        val initialPdfUri: String? = null
    ) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _deepLinkUri = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        userPreferencesRepository.hasSeenOnboardingFlow,
        _deepLinkUri
    ) { hasSeenOnboarding, deepLinkUri ->
        val startDestination = if (hasSeenOnboarding) {
            Screen.Home
        } else {
            Screen.Onboarding
        }
        MainUiState.Ready(
            startDestination = startDestination,
            initialPdfUri = deepLinkUri
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState.Loading
    )

    fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data?.toString()
            if (!uri.isNullOrBlank()) {
                _deepLinkUri.value = uri
            }
        }
    }

    fun setDeepLinkUri(uri: String?) {
        _deepLinkUri.value = uri
    }

    fun clearDeepLink() {
        _deepLinkUri.value = null
    }
}
