package pavansaiajayx.aipdfreadereditor.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.Shapes
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.Spacing
import pavansaiajayx.aipdfreadereditor.core.ui.components.AppIconButton

@Composable
fun ViewerRoute(
    documentUri: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToTools: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(documentUri) {
        if (documentUri.isNotBlank()) {
            val fileName = documentUri.substringAfterLast('/')
            viewModel.onAction(ViewerAction.LoadDocument(uri = documentUri, fileName = fileName, pageCount = 1))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ViewerEvent.NavigateBack -> onNavigateBack()
                is ViewerEvent.NavigateToChat -> onNavigateToChat(event.documentUri)
                is ViewerEvent.NavigateToTools -> onNavigateToTools()
                is ViewerEvent.ScrollToPage -> viewModel.onAction(ViewerAction.PageChanged(event.page))
            }
        }
    }

    ViewerScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@Composable
fun ViewerScreen(
    state: ViewerUiState,
    onAction: (ViewerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        ViewerTopBar(
            fileName = state.fileName,
            onBackClick = { onAction(ViewerAction.BackClicked) },
            onChatClick = { onAction(ViewerAction.ChatClicked) },
            onToolsClick = { onAction(ViewerAction.ToolsClicked) }
        )

        ViewerCanvas(
            state = state,
            onZoomChange = { onAction(ViewerAction.ZoomChanged(state.zoomLevel * it)) },
            onResetZoom = { onAction(ViewerAction.ResetZoom) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        ViewerBottomBar(
            currentPage = state.currentPage,
            pageCount = state.pageCount,
            zoomLevel = state.zoomLevel,
            onPageChange = { onAction(ViewerAction.PageChanged(it)) },
            onResetZoom = { onAction(ViewerAction.ResetZoom) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ViewerTopBar(
    fileName: String,
    onBackClick: () -> Unit,
    onChatClick: () -> Unit,
    onToolsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.viewer_action_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = fileName.ifBlank { stringResource(R.string.viewer_title) },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.sm)
        )

        Row {
            AppIconButton(onClick = onChatClick) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = stringResource(R.string.viewer_action_chat),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AppIconButton(onClick = onToolsClick) {
                Icon(
                    imageVector = Icons.Rounded.Build,
                    contentDescription = stringResource(R.string.viewer_action_tools),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun ViewerCanvas(
    state: ViewerUiState,
    onZoomChange: (Float) -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    onZoomChange(zoom)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onResetZoom() })
            },
        contentAlignment = Alignment.Center
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.xxxl)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg)
                    .graphicsLayer(
                        scaleX = state.zoomLevel,
                        scaleY = state.zoomLevel
                    )
                    .clip(Shapes.md)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.xxxl * 2)
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    Text(
                        text = stringResource(
                            R.string.viewer_page_format,
                            state.currentPage + 1,
                            state.pageCount.coerceAtLeast(1)
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerBottomBar(
    currentPage: Int,
    pageCount: Int,
    zoomLevel: Float,
    onPageChange: (Int) -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    ) {
        if (pageCount > 1) {
            Slider(
                value = currentPage.toFloat(),
                onValueChange = { onPageChange(it.toInt()) },
                valueRange = 0f..(pageCount - 1).toFloat(),
                steps = if (pageCount > 2) pageCount - 2 else 0,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageIndicatorPill(
                text = stringResource(
                    R.string.viewer_page_format,
                    currentPage + 1,
                    pageCount.coerceAtLeast(1)
                )
            )

            PageIndicatorPill(
                text = stringResource(R.string.viewer_zoom_format, (zoomLevel * 100).toInt()),
                onClick = onResetZoom
            )
        }
    }
}

@Composable
private fun PageIndicatorPill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = Shapes.full,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
        )
    }
}
