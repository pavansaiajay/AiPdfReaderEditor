package pavansaiajayx.aipdfreadereditor.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OnboardingScreenTest {

    @Test
    fun onboardingUiState_defaultsAreValid() {
        val state = OnboardingUiState()
        assertEquals(0, state.currentPage)
        assertEquals(3, state.totalPages)
        assertEquals(false, state.isCompleting)
    }

    @Test
    fun onboardingUiState_copyPreservesInvariants() {
        val state = OnboardingUiState(currentPage = 2, isCompleting = true)
        assertEquals(2, state.currentPage)
        assertEquals(3, state.totalPages)
        assertEquals(true, state.isCompleting)
    }

    @Test
    fun onboardingActions_instantiateCorrectly() {
        val pageChanged = OnboardingAction.PageChanged(1)
        assertEquals(1, pageChanged.page)

        assertNotNull(OnboardingAction.NextClicked)
        assertNotNull(OnboardingAction.SkipClicked)
        assertNotNull(OnboardingAction.CompleteOnboarding)
    }

    @Test
    fun onboardingEvents_instantiateCorrectly() {
        assertNotNull(OnboardingEvent.NavigateToHome)
        val scrollToPage = OnboardingEvent.ScrollToPage(2)
        assertEquals(2, scrollToPage.page)
    }
}
