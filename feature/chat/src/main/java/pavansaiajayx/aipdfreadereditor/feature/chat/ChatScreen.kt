package pavansaiajayx.aipdfreadereditor.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.Spacing
import pavansaiajayx.aipdfreadereditor.core.model.AiMessage
import pavansaiajayx.aipdfreadereditor.core.model.AiRole
import pavansaiajayx.aipdfreadereditor.core.ui.components.CreditBadge
import pavansaiajayx.aipdfreadereditor.core.ui.components.EmptyState
import pavansaiajayx.aipdfreadereditor.core.ui.components.InsufficientCreditsDialog

@Composable
fun ChatRoute(
    documentUri: String,
    fileName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(documentUri, fileName) {
        if (documentUri.isNotBlank()) {
            viewModel.onAction(ChatAction.LoadDocument(documentUri, fileName))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.NavigateBack -> onNavigateBack()
                is ChatEvent.ScrollToBottom -> {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size - 1)
                    }
                }
                is ChatEvent.ShowToast -> { /* Handle via snackbar */ }
            }
        }
    }

    ChatScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = { viewModel.onAction(ChatAction.BackClicked) },
        modifier = modifier
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onAction: (ChatAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(message = error)
            onAction(ChatAction.DismissError)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ChatTopBar(
            fileName = state.fileName.ifBlank { stringResource(R.string.chat_title) },
            credits = state.credits,
            onBackClick = onBackClick,
            onEarnCreditsClick = { onAction(ChatAction.EarnCreditsClicked) }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.messages.isEmpty()) {
                ChatEmptyState(
                    onSuggestionClick = { onAction(ChatAction.QuickPromptSelected(it)) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ChatMessageList(
                    messages = state.messages,
                    isAiResponding = state.isAiResponding,
                    modifier = Modifier.fillMaxSize()
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.md)
            )
        }

        if (state.messages.isNotEmpty()) {
            ChatQuickSuggestionsRow(
                onSuggestionClick = { onAction(ChatAction.QuickPromptSelected(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        ChatInputBar(
            query = state.inputQuery,
            isResponding = state.isAiResponding,
            onQueryChange = { onAction(ChatAction.InputQueryChanged(it)) },
            onSendClick = { onAction(ChatAction.SendMessage) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm)
        )
    }

    if (state.showInsufficientCreditsDialog) {
        InsufficientCreditsDialog(
            currentBalance = state.credits,
            requiredCredits = 1,
            onEarnCreditsClick = { onAction(ChatAction.DismissCreditsDialog) },
            onDismissRequest = { onAction(ChatAction.DismissCreditsDialog) }
        )
    }
}

@Composable
private fun ChatTopBar(
    fileName: String,
    credits: Int,
    onBackClick: () -> Unit,
    onEarnCreditsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.chat_back_description),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(Spacing.xs))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.chat_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            CreditBadge(
                credits = credits,
                onClick = onEarnCreditsClick
            )
        }
    }
}

@Composable
private fun ChatMessageList(
    messages: List<AiMessage>,
    isAiResponding: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isAiResponding) {
        val totalCount = messages.size + if (isAiResponding) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            ChatMessageBubble(message = message)
        }

        if (isAiResponding) {
            item(key = "typing_indicator") {
                AiTypingIndicator()
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: AiMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == AiRole.User
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = Spacing.md,
            topEnd = Spacing.md,
            bottomStart = Spacing.md,
            bottomEnd = Spacing.xs
        )
    } else {
        RoundedCornerShape(
            topStart = Spacing.md,
            topEnd = Spacing.md,
            bottomStart = Spacing.xs,
            bottomEnd = Spacing.md
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            contentColor = contentColor,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = "Gemini",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (message.tokenUsage != null) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.chat_token_cost_format, message.tokenUsage!!.creditCost),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiTypingIndicator(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(
            topStart = Spacing.md,
            topEnd = Spacing.md,
            bottomStart = Spacing.xs,
            bottomEnd = Spacing.md
        ),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.chat_ai_typing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatEmptyState(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmptyState(
            title = stringResource(R.string.chat_empty_title),
            message = stringResource(R.string.chat_empty_desc),
            icon = Icons.Rounded.Description,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        ChatQuickSuggestionsRow(
            onSuggestionClick = onSuggestionClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChatQuickSuggestionsRow(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        stringResource(R.string.chat_suggestion_summary),
        stringResource(R.string.chat_suggestion_actions),
        stringResource(R.string.chat_suggestion_conclusion)
    )

    LazyRow(
        modifier = modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        items(suggestions) { suggestion ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onSuggestionClick(suggestion) }
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    query: String,
    isResponding: Boolean,
    onQueryChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.chat_input_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 4,
                enabled = !isResponding,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(Spacing.xs))

            IconButton(
                onClick = onSendClick,
                enabled = query.isNotBlank() && !isResponding
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(R.string.chat_send_description),
                    tint = if (query.isNotBlank() && !isResponding) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}
