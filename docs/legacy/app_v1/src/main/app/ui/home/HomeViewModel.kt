package pavansaiajayx.aipdfreadereditor.app.ui.home

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.app.core.analytics.AnalyticsManager
import pavansaiajayx.aipdfreadereditor.app.core.ads.AdManager
import pavansaiajayx.aipdfreadereditor.app.core.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentDao
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentEntity
import javax.inject.Inject

data class HomeUiState(
    val credits: Int = 0,
    val isAdLoading: Boolean = false,
    val adError: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val creditManager: CreditManager,
    private val adManager: AdManager,
    private val analyticsManager: AnalyticsManager,
    private val documentDao: DocumentDao
) : ViewModel() {

    val recentFiles: StateFlow<List<DocumentEntity>> = documentDao.getAllDocuments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _isAdLoading = MutableStateFlow(false)
    private val _adError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        creditManager.creditsFlow,
        _isAdLoading,
        _adError
    ) { credits, isAdLoading, adError ->
        HomeUiState(
            credits = credits,
            isAdLoading = isAdLoading,
            adError = adError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        preloadAd()
        analyticsManager.logScreenView("home")
    }

    private fun preloadAd() {
        _isAdLoading.value = true
        adManager.loadRewardedAd { success ->
            _isAdLoading.value = false
            if (!success) {
                _adError.value = "Failed to load ad"
            }
        }
    }

    fun onEarnCreditsClicked(activity: Activity) {
        val shown = adManager.showRewardedAd(
            activity = activity,
            onRewardEarned = {
                viewModelScope.launch {
                    creditManager.addCredits(5)
                    analyticsManager.logEvent(
                        "ad_watched_successfully",
                        mapOf("credits_earned" to "5")
                    )
                }
            }
        )
        if (!shown) {
            _adError.value = "No ads available right now. Please try again later."
        }
        preloadAd()
    }

    fun clearAdError() {
        _adError.value = null
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            documentDao.delete(document)
        }
    }
}
