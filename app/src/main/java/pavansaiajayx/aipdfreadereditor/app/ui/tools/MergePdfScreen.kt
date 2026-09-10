package pavansaiajayx.aipdfreadereditor.app.ui.tools

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pavansaiajayx.aipdfreadereditor.app.R
import pavansaiajayx.aipdfreadereditor.app.ui.theme.DeepBlack
import pavansaiajayx.aipdfreadereditor.app.ui.theme.SurfaceDark
import pavansaiajayx.aipdfreadereditor.app.ui.theme.AccentVibrant
import pavansaiajayx.aipdfreadereditor.app.ui.theme.OnSurfaceDim
import pavansaiajayx.aipdfreadereditor.app.ui.theme.OnSurfaceLight
import pavansaiajayx.aipdfreadereditor.app.ui.theme.ErrorRed
import pavansaiajayx.aipdfreadereditor.app.ui.theme.Poppins

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    onNavigateBack: () -> Unit,
    onPreviewPdf: (String) -> Unit,
    viewModel: MergePdfViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedPreviewUri by remember { mutableStateOf<Uri?>(null) }
    val localList = remember(state.selectedPdfs) {
        mutableStateListOf(*state.selectedPdfs.toTypedArray())
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onIntent(MergePdfIntent.AddPdfs(uris))
        }
    }

    if (selectedPreviewUri != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedPreviewUri = null },
            containerColor = SurfaceDark
        ) {
            val docFile = remember(selectedPreviewUri) {
                DocumentFile.fromSingleUri(
                    context,
                    selectedPreviewUri!!
                )
            }
            Column(modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()) {
                Text(
                    text = docFile?.name ?: stringResource(R.string.unknown_pdf),
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurfaceLight,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = docFile?.length()?.let { Formatter.formatFileSize(context, it) }
                        ?: stringResource(R.string.unknown_size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim
                )
                Spacer(modifier = Modifier.height(16.dp))

                val source = try {
                    com.rajat.pdfviewer.util.PdfSource.LocalUri(selectedPreviewUri!!)
                } catch (e: Exception) {
                    null
                }

                if (source != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        com.rajat.pdfviewer.compose.PdfRendererViewCompose(
                            source = source,
                            lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.tool_merge_title),
                        color = OnSurfaceLight,
                        fontFamily = Poppins
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OnSurfaceLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlack)
            )
        },
        floatingActionButton = {
            if (state.selectedPdfs.isEmpty() && !state.isMerging && state.mergeResult == null) {
                FloatingActionButton(
                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                    containerColor = AccentVibrant,
                    contentColor = DeepBlack
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_more_pdfs)
                    )
                }
            }
        },
        containerColor = DeepBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.selectedPdfs.isEmpty()) {
                Text(
                    text = stringResource(R.string.select_2_or_more_pdfs),
                    color = OnSurfaceDim,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    var draggingIndex by remember { mutableStateOf<Int?>(null) }
                    var draggingOffset by remember { mutableStateOf(0f) }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .pointerInput(localList) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val itemInfo = listState.layoutInfo.visibleItemsInfo.find {
                                            offset.y.toInt() in it.offset..(it.offset + it.size)
                                        }
                                        if (itemInfo != null && itemInfo.index < localList.size) {
                                            draggingIndex = itemInfo.index
                                            draggingOffset = 0f
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()

                                        val currentIndex =
                                            draggingIndex ?: return@detectDragGesturesAfterLongPress

                                        val absoluteY = change.position.y
                                        val targetItem =
                                            listState.layoutInfo.visibleItemsInfo.find {
                                                it.index != currentIndex &&
                                                        it.index < localList.size &&
                                                        absoluteY.toInt() in it.offset..(it.offset + it.size)
                                            }

                                        if (targetItem != null) {
                                            val targetIndex = targetItem.index
                                            val item = localList.removeAt(currentIndex)
                                            localList.add(targetIndex, item)
                                            draggingIndex = targetIndex
                                        }

                                        val viewportTop = listState.layoutInfo.viewportStartOffset
                                        val viewportBottom = listState.layoutInfo.viewportEndOffset

                                        if (absoluteY < viewportTop + 150) {
                                            coroutineScope.launch { listState.animateScrollBy(-100f) }
                                        } else if (absoluteY > viewportBottom - 150) {
                                            coroutineScope.launch { listState.animateScrollBy(100f) }
                                        }
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        draggingOffset = 0f
                                        viewModel.onIntent(MergePdfIntent.UpdateList(localList.toList()))
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        draggingOffset = 0f
                                        localList.clear()
                                        localList.addAll(state.selectedPdfs)
                                    }
                                )
                            },
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(localList, key = { _, uri -> uri.toString() }) { index, uri ->
                            val isDragging = index == draggingIndex
                            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f)
                            val elevation by animateFloatAsState(if (isDragging) 8f else 0f)

                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        shadowElevation = elevation
                                    }
                            ) {
                                PdfItemRow(
                                    uri = uri,
                                    onPreview = { selectedPreviewUri = uri },
                                    onRemove = { viewModel.onIntent(MergePdfIntent.RemovePdf(uri)) }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.add_more_pdfs))
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { viewModel.onIntent(MergePdfIntent.MergeClicked) },
                            enabled = state.selectedPdfs.size >= 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentVibrant,
                                disabledContainerColor = SurfaceDark,
                                contentColor = DeepBlack,
                                disabledContentColor = OnSurfaceDim
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.tool_merge_title),
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (state.isMerging) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepBlack.copy(alpha = 0.8f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(
                                stringResource(R.string.merging_progress_format),
                                state.mergeProgress
                            ),
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.mergeProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(50)),
                            color = AccentVibrant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }

            state.mergeResult?.let { result ->
                Dialog(onDismissRequest = { viewModel.onIntent(MergePdfIntent.ResetResult) }) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (result) {
                                is ResultState.Success -> {
                                    Text(
                                        stringResource(R.string.merge_successful_title),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = AccentVibrant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.merge_successful_desc),
                                        color = OnSurfaceLight,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            viewModel.onIntent(MergePdfIntent.ResetResult)
                                            onPreviewPdf(result.uri.toString())
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentVibrant,
                                            contentColor = DeepBlack
                                        )
                                    ) {
                                        Text(stringResource(R.string.view_merged_pdf))
                                    }
                                }

                                is ResultState.Error -> {
                                    Text(
                                        stringResource(R.string.merge_failed_title),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        result.message,
                                        color = OnSurfaceLight,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.onIntent(MergePdfIntent.ResetResult) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SurfaceDark,
                                            contentColor = OnSurfaceLight
                                        )
                                    ) {
                                        Text(stringResource(R.string.dismiss_button))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfItemRow(uri: Uri, onPreview: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val docFile = remember(uri) { DocumentFile.fromSingleUri(context, uri) }
    val name = docFile?.name ?: stringResource(R.string.unknown_pdf)
    val size = docFile?.length()?.let { Formatter.formatFileSize(context, it) }
        ?: stringResource(R.string.unknown_size)

    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemove()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorRed)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .clickable(onClick = onPreview)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = AccentVibrant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    color = OnSurfaceLight,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(size, color = OnSurfaceDim, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
