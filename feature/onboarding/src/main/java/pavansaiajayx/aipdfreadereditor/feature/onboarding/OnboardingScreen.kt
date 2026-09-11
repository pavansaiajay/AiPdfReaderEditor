package pavansaiajayx.aipdfreadereditor.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.Shapes
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.Spacing
import pavansaiajayx.aipdfreadereditor.core.ui.components.AppButton

@Composable
fun OnboardingRoute(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { state.totalPages })

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingEvent.NavigateToHome -> onNavigateToHome()
                is OnboardingEvent.ScrollToPage -> pagerState.animateScrollToPage(event.page)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != state.currentPage) {
                viewModel.onAction(OnboardingAction.PageChanged(page))
            }
        }
    }

    OnboardingScreen(
        state = state,
        pagerState = pagerState,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    pagerState: PagerState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        OnboardingTopBar(
            canSkip = state.currentPage < state.totalPages - 1,
            onSkip = { onAction(OnboardingAction.SkipClicked) }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            OnboardingPageContent(page = page)
        }

        OnboardingPageIndicator(
            pageCount = state.totalPages,
            currentPage = state.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md)
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        val buttonText = if (state.currentPage == state.totalPages - 1) {
            stringResource(R.string.onboarding_action_get_started)
        } else {
            stringResource(R.string.onboarding_action_next)
        }

        AppButton(
            text = buttonText,
            onClick = { onAction(OnboardingAction.NextClicked) },
            isLoading = state.isCompleting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingTopBar(
    canSkip: Boolean,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.xxxl),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = canSkip,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TextButton(onClick = onSkip) {
                Text(
                    text = stringResource(R.string.onboarding_action_skip),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: Int,
    modifier: Modifier = Modifier
) {
    val (icon, titleRes, descRes) = when (page) {
        0 -> Triple(
            Icons.Rounded.PictureAsPdf,
            R.string.onboarding_title_offline,
            R.string.onboarding_desc_offline
        )
        1 -> Triple(
            Icons.Rounded.AutoAwesome,
            R.string.onboarding_title_ai,
            R.string.onboarding_desc_ai
        )
        else -> Triple(
            Icons.Rounded.MonetizationOn,
            R.string.onboarding_title_credits,
            R.string.onboarding_desc_credits
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.xxxl * 2)
                .clip(Shapes.xl)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Spacing.xxxl),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = stringResource(descRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width = if (isSelected) Spacing.xl else Spacing.sm
            val color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.xs)
                    .height(Spacing.sm)
                    .width(width)
                    .clip(Shapes.full)
                    .background(color)
            )
        }
    }
}
