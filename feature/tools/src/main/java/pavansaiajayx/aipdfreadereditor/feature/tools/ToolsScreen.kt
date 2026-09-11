package pavansaiajayx.aipdfreadereditor.feature.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pavansaiajayx.aipdfreadereditor.core.designsystem.theme.Spacing
import pavansaiajayx.aipdfreadereditor.core.ui.components.AppButton

@Composable
fun ToolsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ToolsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onAction(ToolsAction.FilesSelected(listOf(it.toString()))) }
    }

    val multiplePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onAction(ToolsAction.FilesSelected(uris.map { it.toString() }))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ToolsEvent.NavigateBack -> onNavigateBack()
                is ToolsEvent.LaunchFilePicker -> {
                    if (event.isMultiple) {
                        multiplePickerLauncher.launch(arrayOf(event.mimeType))
                    } else {
                        singlePickerLauncher.launch(arrayOf(event.mimeType))
                    }
                }
                is ToolsEvent.NavigateToViewer -> onNavigateToViewer(event.uri)
            }
        }
    }

    ToolsScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenViewer = onNavigateToViewer,
        modifier = modifier
    )
}

@Composable
fun ToolsScreen(
    state: ToolsUiState,
    onAction: (ToolsAction) -> Unit,
    onOpenViewer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onAction(ToolsAction.DismissError)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ToolsTopBar(onBackClick = { onAction(ToolsAction.BackClicked) })

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xl)
            ) {
                item(key = "organize_section") {
                    ToolsCategorySection(
                        title = stringResource(R.string.tools_category_organize),
                        tools = listOf(
                            ToolGridItem(ToolType.Merge, stringResource(R.string.tool_merge_title), stringResource(R.string.tool_merge_desc), Icons.Rounded.CallMerge),
                            ToolGridItem(ToolType.Split, stringResource(R.string.tool_split_title), stringResource(R.string.tool_split_desc), Icons.Rounded.CallSplit),
                            ToolGridItem(ToolType.Rotate, stringResource(R.string.tool_rotate_title), stringResource(R.string.tool_rotate_desc), Icons.Rounded.RotateRight)
                        ),
                        onToolClick = { onAction(ToolsAction.SelectTool(it)) }
                    )
                }

                item(key = "convert_section") {
                    ToolsCategorySection(
                        title = stringResource(R.string.tools_category_convert),
                        tools = listOf(
                            ToolGridItem(ToolType.Compress, stringResource(R.string.tool_compress_title), stringResource(R.string.tool_compress_desc), Icons.Rounded.Compress),
                            ToolGridItem(ToolType.ImagesToPdf, stringResource(R.string.tool_images_to_pdf_title), stringResource(R.string.tool_images_to_pdf_desc), Icons.Rounded.AddPhotoAlternate),
                            ToolGridItem(ToolType.PdfToImages, stringResource(R.string.tool_pdf_to_images_title), stringResource(R.string.tool_pdf_to_images_desc), Icons.Rounded.Collections),
                            ToolGridItem(ToolType.ExtractText, stringResource(R.string.tool_extract_text_title), stringResource(R.string.tool_extract_text_desc), Icons.Rounded.TextFields)
                        ),
                        onToolClick = { onAction(ToolsAction.SelectTool(it)) }
                    )
                }

                item(key = "security_section") {
                    ToolsCategorySection(
                        title = stringResource(R.string.tools_category_security),
                        tools = listOf(
                            ToolGridItem(ToolType.Encrypt, stringResource(R.string.tool_encrypt_title), stringResource(R.string.tool_encrypt_desc), Icons.Rounded.Lock),
                            ToolGridItem(ToolType.Decrypt, stringResource(R.string.tool_decrypt_title), stringResource(R.string.tool_decrypt_desc), Icons.Rounded.LockOpen),
                            ToolGridItem(ToolType.Watermark, stringResource(R.string.tool_watermark_title), stringResource(R.string.tool_watermark_desc), Icons.Rounded.BrandingWatermark)
                        ),
                        onToolClick = { onAction(ToolsAction.SelectTool(it)) }
                    )
                }
            }
        }

        if (state.isProcessing) {
            ToolsProcessingOverlay(progress = state.progress)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.md)
        )
    }

    if (state.showPasswordDialog) {
        PasswordInputDialog(
            onConfirm = { onAction(ToolsAction.PasswordSubmitted(it)) },
            onDismiss = { onAction(ToolsAction.DismissDialogs) }
        )
    }

    if (state.showWatermarkDialog) {
        WatermarkInputDialog(
            onConfirm = { onAction(ToolsAction.WatermarkSubmitted(it)) },
            onDismiss = { onAction(ToolsAction.DismissDialogs) }
        )
    }

    if (state.showRotationDialog) {
        RotationSelectDialog(
            onConfirm = { onAction(ToolsAction.RotationSubmitted(it)) },
            onDismiss = { onAction(ToolsAction.DismissDialogs) }
        )
    }

    state.resultMessage?.let { msg ->
        OperationSuccessDialog(
            message = msg,
            resultUri = state.resultUri,
            onOpenPdf = { state.resultUri?.let(onOpenViewer) },
            onDismiss = { onAction(ToolsAction.DismissResult) }
        )
    }
}

private data class ToolGridItem(
    val type: ToolType,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
private fun ToolsTopBar(
    onBackClick: () -> Unit,
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
                    contentDescription = stringResource(R.string.tools_back_description),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(Spacing.xs))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tools_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.tools_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(end = Spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.tools_badge_free_offline),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                )
            }
        }
    }
}

@Composable
private fun ToolsCategorySection(
    title: String,
    tools: List<ToolGridItem>,
    onToolClick: (ToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .height(((tools.size + 1) / 2 * 120).dp),
            userScrollEnabled = false
        ) {
            items(tools, key = { it.type.name }) { tool ->
                ToolCard(
                    item = tool,
                    onClick = { onToolClick(tool.type) }
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    item: ToolGridItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ToolsProcessingOverlay(
    progress: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.padding(Spacing.xl)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = stringResource(R.string.tools_processing),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordInputDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tool_encrypt_title)) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.tools_password_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.tools_action_process),
                onClick = { if (password.isNotBlank()) onConfirm(password) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tools_action_cancel))
            }
        }
    )
}

@Composable
private fun WatermarkInputDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var watermarkText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tool_watermark_title)) },
        text = {
            OutlinedTextField(
                value = watermarkText,
                onValueChange = { watermarkText = it },
                label = { Text(stringResource(R.string.tools_watermark_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.tools_action_process),
                onClick = { if (watermarkText.isNotBlank()) onConfirm(watermarkText) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tools_action_cancel))
            }
        }
    )
}

@Composable
private fun RotationSelectDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDegrees by remember { mutableIntStateOf(90) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tool_rotate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                listOf(
                    90 to stringResource(R.string.tools_rotation_90),
                    180 to stringResource(R.string.tools_rotation_180),
                    270 to stringResource(R.string.tools_rotation_270)
                ).forEach { (deg, label) ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (selectedDegrees == deg) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDegrees = deg }
                            .padding(vertical = Spacing.xs)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedDegrees == deg) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(Spacing.sm)
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.tools_action_process),
                onClick = { onConfirm(selectedDegrees) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tools_action_cancel))
            }
        }
    )
}

@Composable
private fun OperationSuccessDialog(
    message: String,
    resultUri: String?,
    onOpenPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = { Text(stringResource(R.string.tools_success_title)) },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            if (resultUri != null && resultUri.endsWith(".pdf", ignoreCase = true)) {
                AppButton(
                    text = stringResource(R.string.tools_action_open),
                    onClick = {
                        onDismiss()
                        onOpenPdf()
                    }
                )
            } else {
                AppButton(
                    text = stringResource(R.string.tools_action_dismiss),
                    onClick = onDismiss
                )
            }
        },
        dismissButton = {
            if (resultUri != null && resultUri.endsWith(".pdf", ignoreCase = true)) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.tools_action_dismiss))
                }
            }
        }
    )
}
