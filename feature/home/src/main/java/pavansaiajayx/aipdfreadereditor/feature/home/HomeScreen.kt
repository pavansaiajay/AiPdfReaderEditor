package pavansaiajayx.aipdfreadereditor.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.Spacing
import pavansaiajayx.aipdfreadereditor.core.ui.components.AppButton
import pavansaiajayx.aipdfreadereditor.core.ui.components.AppOutlinedButton
import pavansaiajayx.aipdfreadereditor.core.ui.components.AppSearchField
import pavansaiajayx.aipdfreadereditor.core.ui.components.BlendedAdCard
import pavansaiajayx.aipdfreadereditor.core.ui.components.CreditBadge
import pavansaiajayx.aipdfreadereditor.core.ui.components.DocumentCard
import pavansaiajayx.aipdfreadereditor.core.ui.components.EmptyState
import pavansaiajayx.aipdfreadereditor.core.ui.components.InsufficientCreditsDialog

@Composable
fun HomeRoute(
    onNavigateToViewer: (String) -> Unit,
    onNavigateToTools: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            viewModel.onAction(HomeAction.OpenPdfUri(it.toString(), fileName))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToViewer -> onNavigateToViewer(event.uri)
                is HomeEvent.NavigateToTools -> onNavigateToTools()
                is HomeEvent.LaunchFilePicker -> openDocumentLauncher.launch(arrayOf("application/pdf"))
            }
        }
    }

    HomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenDocumentClick = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onOpenDocumentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = Spacing.lg)
    ) {
        HomeTopBar(
            credits = state.credits,
            onEarnCreditsClick = { onAction(HomeAction.EarnCreditsClicked) },
            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.md)
        )

        AppSearchField(
            query = state.searchQuery,
            onQueryChange = { onAction(HomeAction.SearchQueryChanged(it)) },
            placeholder = stringResource(R.string.home_search_placeholder),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        HomeQuickActionsRow(
            onOpenPdfClick = onOpenDocumentClick,
            onToolsClick = { onAction(HomeAction.OpenToolsClicked) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(R.string.home_section_recent),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        if (state.recentDocuments.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_desc),
                actionText = stringResource(R.string.home_empty_action),
                onActionClick = onOpenDocumentClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            RecentDocumentsFeed(
                documents = state.recentDocuments,
                onDocumentClick = { onAction(HomeAction.DocumentClicked(it.uri)) },
                onDeleteClick = { onAction(HomeAction.DeleteDocument(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    if (state.showInsufficientCreditsDialog) {
        InsufficientCreditsDialog(
            currentBalance = state.credits,
            requiredCredits = 1,
            onEarnCreditsClick = {
                onAction(HomeAction.DismissCreditsDialog)
            },
            onDismissRequest = { onAction(HomeAction.DismissCreditsDialog) }
        )
    }
}

@Composable
private fun HomeTopBar(
    credits: Int,
    onEarnCreditsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        CreditBadge(
            credits = credits,
            onClick = onEarnCreditsClick
        )
    }
}

@Composable
private fun HomeQuickActionsRow(
    onOpenPdfClick: () -> Unit,
    onToolsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        AppButton(
            text = stringResource(R.string.home_quick_action_open),
            onClick = onOpenPdfClick,
            modifier = Modifier.weight(1f)
        )

        AppOutlinedButton(
            text = stringResource(R.string.home_quick_action_tools),
            onClick = onToolsClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RecentDocumentsFeed(
    documents: List<DocumentEntity>,
    onDocumentClick: (DocumentEntity) -> Unit,
    onDeleteClick: (DocumentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(vertical = Spacing.sm)
    ) {
        items(
            items = documents,
            key = { it.id }
        ) { document ->
            val sizeKb = if (document.fileSizeBytes > 0) "${document.fileSizeBytes / 1024} KB" else ""
            DocumentCard(
                fileName = document.fileName,
                pageCount = document.pageCount,
                fileSizeText = sizeKb,
                lastAccessedText = "",
                onClick = { onDocumentClick(document) },
                onMenuClick = { onDeleteClick(document) }
            )
        }

        item(key = "native_ad_card") {
            BlendedAdCard(
                headline = stringResource(R.string.home_ad_headline),
                body = stringResource(R.string.home_ad_body),
                onClick = { /* Intentionally non-blocking blended promo click */ }
            )
        }
    }
}
