package pavansaiajayx.aipdfreadereditor.app.ui.viewer

import android.net.Uri
import android.widget.Toast
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rajat.pdfviewer.compose.PdfRendererViewCompose
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.rajat.pdfviewer.util.PdfSource
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEdit
import pavansaiajayx.aipdfreadereditor.app.ui.theme.AccentVibrant
import pavansaiajayx.aipdfreadereditor.app.ui.theme.DeepBlack
import pavansaiajayx.aipdfreadereditor.app.ui.theme.OnSurfaceLight
import pavansaiajayx.aipdfreadereditor.app.ui.theme.SurfaceDark
import pavansaiajayx.aipdfreadereditor.app.ui.theme.SurfaceVariant
import java.io.File
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    fileUri: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentPath by remember { mutableStateOf<android.graphics.Path?>(null) }
    var currentRect by remember { mutableStateOf<android.graphics.RectF?>(null) }
    
    var textDialogVisible by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var textLocation by remember { mutableStateOf(Offset.Zero) }
    
    var signatureDialogVisible by remember { mutableStateOf(false) }
    var signaturePath by remember { mutableStateOf<android.graphics.Path?>(null) }
    var savedSignature by remember { mutableStateOf<android.graphics.Path?>(null) }
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showAiSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(state.error, state.saveSuccess) {
        if (state.error != null) {
            snackbarHostState.showSnackbar(state.error!!)
            viewModel.clearError()
        }
        if (state.saveSuccess) {
            Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetSaveSuccess()
        }
    }

    if (state.showInsufficientCreditsDialog) {
        val currentContext = LocalContext.current
        val activity = currentContext as? Activity

        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text("Out of Credits") },
            text = { Text("You need more credits to use this feature. Watch a short ad to earn 5 free credits!") },
            confirmButton = {
                Button(onClick = { 
                    activity?.let { viewModel.watchAdForCredits(it) } 
                }) {
                    Text("Watch Ad (+5)")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (textDialogVisible) {
        AlertDialog(
            onDismissRequest = { textDialogVisible = false },
            title = { Text("Add Text", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.addEdit(
                            PdfEdit.Text(
                                text = textInput,
                                x = textLocation.x,
                                y = textLocation.y,
                                color = state.currentColor.toArgb(),
                                size = 16f
                            )
                        )
                    }
                    textDialogVisible = false
                    textInput = ""
                }) { Text("Add", color = AccentVibrant) }
            },
            containerColor = SurfaceDark
        )
    }

    if (signatureDialogVisible) {
        Dialog(onDismissRequest = { 
            signatureDialogVisible = false
            signaturePath = null
        }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text("Draw Signature", color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newPath = android.graphics.Path()
                                    newPath.moveTo(offset.x, offset.y)
                                    signaturePath = newPath
                                },
                                onDrag = { change, _ ->
                                    signaturePath?.lineTo(change.position.x, change.position.y)
                                }
                            )
                        }
                ) {
                    signaturePath?.let {
                        drawPath(it.asComposePath(), Color.Black, style = Stroke(width = 5f))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        signaturePath = null
                    }) { Text("Clear", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        savedSignature = signaturePath
                        signatureDialogVisible = false
                        signaturePath = null
                    }) { Text("Done", color = AccentVibrant) }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...", color = OnSurfaceLight) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                            viewModel.clearSearchResults()
                        }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Close Search", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.searchInPdf(Uri.parse(fileUri), searchQuery) }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Submit Search", tint = AccentVibrant)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
                )
            } else {
                TopAppBar(
                    title = { Text("PDF Viewer", color = MaterialTheme.colorScheme.onSurface) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            showAiSheet = true
                            viewModel.summarizePdf(Uri.parse(fileUri))
                        }) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Summary", tint = AccentVibrant)
                        }
                        IconButton(onClick = { onNavigateToChat(fileUri) }) {
                            Icon(Icons.Rounded.Send, contentDescription = "Chat with PDF", tint = AccentVibrant)
                        }
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = OnSurfaceLight)
                        }
                        IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(fileUri))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                    }) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = OnSurfaceLight
                        )
                    }
                    IconButton(onClick = {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        val printAdapter = object : PrintDocumentAdapter() {
                            override fun onWrite(pages: Array<out android.print.PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: WriteResultCallback?) {
                                try {
                                    context.contentResolver.openInputStream(Uri.parse(fileUri))?.use { input ->
                                        java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                } catch (e: Exception) {
                                    callback?.onWriteFailed(e.message)
                                }
                            }
                            override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback?, extras: android.os.Bundle?) {
                                if (cancellationSignal?.isCanceled == true) {
                                    callback?.onLayoutCancelled()
                                    return
                                }
                                val builder = PrintDocumentInfo.Builder("document.pdf")
                                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                                callback?.onLayoutFinished(builder.build(), newAttributes != oldAttributes)
                            }
                        }
                        printManager.print("Document", printAdapter, null)
                    }) {
                        Icon(
                            Icons.Rounded.Print,
                            contentDescription = "Print",
                            tint = OnSurfaceLight
                        )
                    }
                    IconButton(onClick = { viewModel.saveEdits(fileUri, 0) }) {
                        Icon(
                            Icons.Rounded.Save,
                            contentDescription = "Save",
                            tint = AccentVibrant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepBlack)
        ) {

            val source = try {
                if (fileUri.startsWith("content://") || fileUri.startsWith("file://")) {
                    PdfSource.LocalUri(Uri.parse(fileUri))
                } else {
                    PdfSource.LocalFile(File(fileUri))
                }
            } catch (e: Exception) {
                null
            }

            if (source != null) {
                PdfRendererViewCompose(
                    source = source,
                    lifecycleOwner = LocalLifecycleOwner.current,
                    statusCallBack = object : com.rajat.pdfviewer.PdfRendererView.StatusCallBack {
                        override fun onError(error: Throwable) {
                            Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )

                // The Canvas Overlay
                if (state.currentTool != EditTool.None) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(state.currentTool) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (state.currentTool == EditTool.Pen) {
                                            val newPath = android.graphics.Path()
                                            newPath.moveTo(offset.x, offset.y)
                                            currentPath = newPath
                                        } else if (state.currentTool == EditTool.Highlight) {
                                            currentRect = android.graphics.RectF(offset.x, offset.y, offset.x, offset.y)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        if (state.currentTool == EditTool.Pen) {
                                            currentPath?.lineTo(change.position.x, change.position.y)
                                        } else if (state.currentTool == EditTool.Highlight) {
                                            currentRect?.apply {
                                                right = change.position.x
                                                bottom = change.position.y
                                            }
                                        } else if (state.currentTool == EditTool.Eraser) {
                                            val touchRect = android.graphics.RectF(change.position.x - 20f, change.position.y - 20f, change.position.x + 20f, change.position.y + 20f)
                                            val toRemove = state.edits.filter { edit ->
                                                when (edit) {
                                                    is PdfEdit.Highlight -> android.graphics.RectF.intersects(edit.rect, touchRect)
                                                    else -> false
                                                }
                                            }
                                            toRemove.forEach { viewModel.removeEdit(it) }
                                        }
                                    },
                                    onDragEnd = {
                                        if (state.currentTool == EditTool.Pen && currentPath != null) {
                                            viewModel.addEdit(PdfEdit.Draw(currentPath!!, state.currentColor.toArgb(), 5f))
                                            currentPath = null
                                        } else if (state.currentTool == EditTool.Highlight && currentRect != null) {
                                            viewModel.addEdit(PdfEdit.Highlight(currentRect!!, state.currentColor.copy(alpha = 0.5f).toArgb()))
                                            currentRect = null
                                        }
                                    }
                                )
                            }
                            .pointerInput(state.currentTool) {
                                detectTapGestures { offset ->
                                    if (state.currentTool == EditTool.Text) {
                                        textLocation = offset
                                        textDialogVisible = true
                                    } else if (state.currentTool == EditTool.Signature && savedSignature != null) {
                                        val bounds = android.graphics.RectF()
                                        savedSignature!!.computeBounds(bounds, true)
                                        val matrix = android.graphics.Matrix()
                                        matrix.setTranslate(offset.x - bounds.centerX(), offset.y - bounds.centerY())
                                        val translatedPath = android.graphics.Path(savedSignature)
                                        translatedPath.transform(matrix)
                                        viewModel.addEdit(PdfEdit.Draw(translatedPath, state.currentColor.toArgb(), 5f))
                                        viewModel.setTool(EditTool.None)
                                    }
                                }
                            }
                    ) {
                        state.edits.forEach { edit ->
                            when (edit) {
                                is PdfEdit.Draw -> {
                                    drawPath(
                                        path = edit.path.asComposePath(),
                                        color = Color(edit.color),
                                        style = Stroke(width = edit.strokeWidth)
                                    )
                                }
                                is PdfEdit.Highlight -> {
                                    val left = minOf(edit.rect.left, edit.rect.right)
                                    val top = minOf(edit.rect.top, edit.rect.bottom)
                                    val w = kotlin.math.abs(edit.rect.width())
                                    val h = kotlin.math.abs(edit.rect.height())
                                    drawRect(
                                        color = Color(edit.color),
                                        topLeft = Offset(left, top),
                                        size = Size(w, h)
                                    )
                                }
                                is PdfEdit.Text -> {
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = edit.text,
                                        topLeft = Offset(edit.x, edit.y),
                                        style = TextStyle(color = Color(edit.color), fontSize = edit.size.sp)
                                    )
                                }
                            }
                        }

                        currentPath?.let {
                            drawPath(it.asComposePath(), state.currentColor, style = Stroke(width = 5f))
                        }
                        currentRect?.let {
                            val left = minOf(it.left, it.right)
                            val top = minOf(it.top, it.bottom)
                            val w = kotlin.math.abs(it.width())
                            val h = kotlin.math.abs(it.height())
                            drawRect(state.currentColor.copy(alpha = 0.5f), Offset(left, top), Size(w, h))
                        }
                    }
                }
            } else {
                Text(
                    text = "Invalid PDF Uri",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Toolbar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolIconButton(
                    icon = Icons.Rounded.TouchApp,
                    isSelected = state.currentTool == EditTool.None,
                    onClick = { viewModel.setTool(EditTool.None) }
                )
                ToolIconButton(
                    icon = Icons.Rounded.Create,
                    isSelected = state.currentTool == EditTool.Pen,
                    onClick = { viewModel.setTool(EditTool.Pen) }
                )
                ToolIconButton(
                    icon = Icons.Rounded.Highlight,
                    isSelected = state.currentTool == EditTool.Highlight,
                    onClick = { viewModel.setTool(EditTool.Highlight) }
                )
                ToolIconButton(
                    icon = Icons.Rounded.Title,
                    isSelected = state.currentTool == EditTool.Text,
                    onClick = { viewModel.setTool(EditTool.Text) }
                )
                ToolIconButton(
                    icon = Icons.Rounded.Clear,
                    isSelected = state.currentTool == EditTool.Eraser,
                    onClick = { viewModel.setTool(EditTool.Eraser) }
                )
                ToolIconButton(
                    icon = Icons.Rounded.BorderColor,
                    isSelected = state.currentTool == EditTool.Signature,
                    onClick = { 
                        viewModel.setTool(EditTool.Signature)
                        signatureDialogVisible = true
                    }
                )
            }
            
            // Color Picker
            if (state.currentTool == EditTool.Pen || state.currentTool == EditTool.Highlight || state.currentTool == EditTool.Text) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Black, Color.White)
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { viewModel.setColor(color) }
                        ) {
                            if (state.currentColor == color) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = if (color == Color.White) Color.Black else Color.White, modifier = Modifier.align(Alignment.Center).size(16.dp))
                            }
                        }
                    }
                }
            }

            // Search Results
            state.searchResults?.let { results ->
                if (results.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("Found on pages: ", color = OnSurfaceLight, modifier = Modifier.padding(start = 8.dp, end = 4.dp))
                        }
                        items(results.size) { index ->
                            val page = results[index]
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceVariant)
                                    .clickable {
                                        Toast.makeText(context, "Scroll to page $page not natively supported by wrapped view", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(page.toString(), color = AccentVibrant, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                } else if (isSearchActive && searchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .padding(16.dp)
                    ) {
                        Text("No results found.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (state.isSaving) {
                Box(
                    modifier = Modifier.fillMaxSize().background(DeepBlack.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentVibrant)
                }
            }

            if (showAiSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showAiSheet = false
                        viewModel.clearAiSummary()
                    },
                    sheetState = sheetState,
                    containerColor = SurfaceDark
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(pavansaiajayx.aipdfreadereditor.app.R.string.ai_summary_title), color = AccentVibrant, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.isAiProcessing) {
                            CircularProgressIndicator(color = AccentVibrant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(pavansaiajayx.aipdfreadereditor.app.R.string.ai_analyzing), color = OnSurfaceLight)
                        } else if (state.aiSummary != null) {
                            Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
                                Text(state.aiSummary!!, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AI Summary", state.aiSummary)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(pavansaiajayx.aipdfreadereditor.app.R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                            }) {
                                Text(stringResource(pavansaiajayx.aipdfreadereditor.app.R.string.copy_action), color = AccentVibrant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) SurfaceVariant else Color.Transparent)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) AccentVibrant else OnSurfaceLight
        )
    }
}
