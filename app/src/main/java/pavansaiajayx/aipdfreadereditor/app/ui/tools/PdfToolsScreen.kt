package pavansaiajayx.aipdfreadereditor.app.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import android.net.Uri
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RotateRight
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.compose.material.icons.rounded.FormatListNumbered
import pavansaiajayx.aipdfreadereditor.app.R
import pavansaiajayx.aipdfreadereditor.app.ui.theme.AccentVibrant
import pavansaiajayx.aipdfreadereditor.app.ui.theme.DeepBlack
import pavansaiajayx.aipdfreadereditor.app.ui.theme.OnSurfaceDim
import pavansaiajayx.aipdfreadereditor.app.ui.theme.OnSurfaceLight
import pavansaiajayx.aipdfreadereditor.app.ui.theme.SurfaceDark
import pavansaiajayx.aipdfreadereditor.app.ui.theme.SurfaceVariant

@Composable
fun PdfToolsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEarnCredits: () -> Unit,
    onNavigateToMergePdf: () -> Unit,
    onNavigateToSplitPdf: () -> Unit,
    onNavigateToDeletePdfPage: () -> Unit,
    onNavigateToExtractPages: () -> Unit,
    viewModel: PdfToolsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val credits by viewModel.credits.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var passwordDialogVisible by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<((String) -> Unit)?>(null) }

    var watermarkDialogVisible by remember { mutableStateOf(false) }
    var watermarkInput by remember { mutableStateOf("") }
    var pendingWatermarkUri by remember { mutableStateOf<Uri?>(null) }
    
    var removePagesDialogVisible by remember { mutableStateOf(false) }
    var removePagesInput by remember { mutableStateOf("") }
    var pendingRemovePagesUri by remember { mutableStateOf<Uri?>(null) }

    var rotatePdfDialogVisible by remember { mutableStateOf(false) }
    var pendingRotatePdfUri by remember { mutableStateOf<Uri?>(null) }
    
    var reorderPagesDialogVisible by remember { mutableStateOf(false) }
    var reorderPagesInput by remember { mutableStateOf("") }
    var pendingReorderPagesUri by remember { mutableStateOf<Uri?>(null) }
    
    var extractPageDialogVisible by remember { mutableStateOf(false) }
    var extractPageInput by remember { mutableStateOf("") }
    var pendingExtractPageUri by remember { mutableStateOf<Uri?>(null) }

    var htmlToPdfDialogVisible by remember { mutableStateOf(false) }
    var htmlToPdfInput by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    // ── SAF Launchers ──

    val mergePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.size >= 2) {
            viewModel.onIntent(PdfToolsIntent.MergeClicked(uris))
        }
    }

    val splitPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onIntent(PdfToolsIntent.SplitClicked(it)) }
    }

    val compressPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onIntent(PdfToolsIntent.CompressClicked(it)) }
    }

    val encryptPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingAction = { password -> viewModel.onIntent(PdfToolsIntent.EncryptClicked(uri, password)) }
            passwordDialogVisible = true
        }
    }

    val decryptPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingAction = { password -> viewModel.onIntent(PdfToolsIntent.DecryptClicked(uri, password)) }
            passwordDialogVisible = true
        }
    }

    val imagesToPdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onIntent(PdfToolsIntent.ImagesToPdfClicked(uris))
        }
    }

    val pdfToImagesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onIntent(PdfToolsIntent.PdfToImagesClicked(it)) }
    }

    val addWatermarkPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingWatermarkUri = uri
            watermarkDialogVisible = true
        }
    }

    val removePagesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingRemovePagesUri = uri
            removePagesDialogVisible = true
        }
    }

    val rotatePdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingRotatePdfUri = uri
            rotatePdfDialogVisible = true
        }
    }

    val reorderPagesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingReorderPagesUri = uri
            reorderPagesDialogVisible = true
        }
    }

    val extractPagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingExtractPageUri = uri
            extractPageDialogVisible = true
        }
    }

    val flattenPdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(PdfToolsIntent.FlattenPdfClicked(uri))
        }
    }

    val extractTextPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onIntent(PdfToolsIntent.ExtractTextClicked(it)) }
    }

    val ocrPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onIntent(PdfToolsIntent.OcrImageClicked(it)) }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.uri?.let { pdfUri ->
                viewModel.onIntent(PdfToolsIntent.ScanDocumentCompleted(pdfUri.toString()))
            }
        }
    }

    val saveTextFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val currentState = state
        if (uri != null && currentState is PdfToolsState.Success) {
            currentState.outputFiles.firstOrNull()?.let { file ->
                viewModel.onIntent(PdfToolsIntent.SaveFile(file, uri))
            }
        }
    }

    val saveSingleFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val currentState = state
        if (uri != null && currentState is PdfToolsState.Success) {
            currentState.outputFiles.firstOrNull()?.let { file ->
                viewModel.onIntent(PdfToolsIntent.SaveFile(file, uri))
            }
        }
    }

    val saveMultipleFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val currentState = state
        if (uri != null && currentState is PdfToolsState.Success) {
            viewModel.onIntent(PdfToolsIntent.SaveFiles(currentState.outputFiles, uri))
        }
    }

    // ── String Resources ──
    val mergeSuccessMsg = stringResource(R.string.merge_success)
    val splitSuccessMsg = stringResource(R.string.split_success)
    val compressSuccessMsg = stringResource(R.string.compress_success)
    val encryptSuccessMsg = stringResource(R.string.encrypt_success)
    val decryptSuccessMsg = stringResource(R.string.decrypt_success)
    val imagesToPdfSuccessMsg = stringResource(R.string.images_to_pdf_success)
    val pdfToImagesSuccessMsg = stringResource(R.string.pdf_to_images_success)
    val watermarkSuccessMsg = stringResource(R.string.watermark_success)
    val extractTextSuccessMsg = stringResource(R.string.extract_text_success)
    val ocrSuccessMsg = stringResource(R.string.ocr_success)
    val scanSuccessMsg = stringResource(R.string.scan_success)
    val removePagesSuccessMsg = stringResource(R.string.remove_pages_success)
    val rotatePdfSuccessMsg = stringResource(R.string.rotate_pdf_success)
    val reorderPagesSuccessMsg = stringResource(R.string.reorder_pages_success)
    val extractPageSuccessMsg = stringResource(R.string.extract_page_success)
    val flattenPdfSuccessMsg = stringResource(R.string.flatten_pdf_success)
    val htmlToPdfSuccessMsg = stringResource(R.string.html_to_pdf_success)

    // ── Side Effects ──
    LaunchedEffect(state) {
        when (val s = state) {
            is PdfToolsState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.onIntent(PdfToolsIntent.DismissState)
            }
            else -> {}
        }
    }

    // ── Insufficient Credits Dialog ──
    if (state is PdfToolsState.InsufficientCredits) {
        val context = LocalContext.current
        val activity = context as? Activity

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

    if (passwordDialogVisible) {
        AlertDialog(
            onDismissRequest = { 
                passwordDialogVisible = false
                passwordInput = ""
                pendingAction = null
            },
            title = {
                Text(
                    text = stringResource(R.string.password_dialog_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text(stringResource(R.string.password_dialog_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (passwordInput.isNotEmpty()) {
                            pendingAction?.invoke(passwordInput)
                            passwordDialogVisible = false
                            passwordInput = ""
                            pendingAction = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_confirm), color = AccentVibrant)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        passwordDialogVisible = false
                        passwordInput = ""
                        pendingAction = null
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_cancel), color = OnSurfaceDim)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (watermarkDialogVisible) {
        AlertDialog(
            onDismissRequest = { 
                watermarkDialogVisible = false
                watermarkInput = ""
                pendingWatermarkUri = null
            },
            title = {
                Text(
                    text = stringResource(R.string.watermark_dialog_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = watermarkInput,
                    onValueChange = { watermarkInput = it },
                    label = { Text(stringResource(R.string.watermark_dialog_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingWatermarkUri
                        if (watermarkInput.isNotEmpty() && uri != null) {
                            viewModel.onIntent(PdfToolsIntent.AddWatermarkClicked(uri, watermarkInput))
                            watermarkDialogVisible = false
                            watermarkInput = ""
                            pendingWatermarkUri = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_confirm), color = AccentVibrant)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        watermarkDialogVisible = false
                        watermarkInput = ""
                        pendingWatermarkUri = null
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_cancel), color = OnSurfaceDim)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (removePagesDialogVisible) {
        AlertDialog(
            onDismissRequest = { 
                removePagesDialogVisible = false
                removePagesInput = ""
                pendingRemovePagesUri = null
            },
            title = {
                Text(
                    text = stringResource(R.string.remove_pages_dialog_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = removePagesInput,
                    onValueChange = { removePagesInput = it },
                    label = { Text(stringResource(R.string.remove_pages_dialog_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingRemovePagesUri
                        if (removePagesInput.isNotEmpty() && uri != null) {
                            viewModel.onIntent(PdfToolsIntent.DeletePagesClicked(uri, removePagesInput))
                            removePagesDialogVisible = false
                            removePagesInput = ""
                            pendingRemovePagesUri = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_confirm), color = AccentVibrant)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        removePagesDialogVisible = false
                        removePagesInput = ""
                        pendingRemovePagesUri = null
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_cancel), color = OnSurfaceDim)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (rotatePdfDialogVisible) {
        AlertDialog(
            onDismissRequest = { 
                rotatePdfDialogVisible = false
                pendingRotatePdfUri = null
            },
            title = {
                Text(
                    text = stringResource(R.string.rotate_pdf_dialog_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    TextButton(onClick = {
                        pendingRotatePdfUri?.let { viewModel.onIntent(PdfToolsIntent.RotatePdfClicked(it, 90)) }
                        rotatePdfDialogVisible = false
                        pendingRotatePdfUri = null
                    }) { Text("90°", color = OnSurfaceLight) }
                    TextButton(onClick = {
                        pendingRotatePdfUri?.let { viewModel.onIntent(PdfToolsIntent.RotatePdfClicked(it, 180)) }
                        rotatePdfDialogVisible = false
                        pendingRotatePdfUri = null
                    }) { Text("180°", color = OnSurfaceLight) }
                    TextButton(onClick = {
                        pendingRotatePdfUri?.let { viewModel.onIntent(PdfToolsIntent.RotatePdfClicked(it, 270)) }
                        rotatePdfDialogVisible = false
                        pendingRotatePdfUri = null
                    }) { Text("270°", color = OnSurfaceLight) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        rotatePdfDialogVisible = false
                        pendingRotatePdfUri = null
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_cancel), color = OnSurfaceDim)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (reorderPagesDialogVisible) {
        AlertDialog(
            onDismissRequest = { 
                reorderPagesDialogVisible = false
                reorderPagesInput = ""
                pendingReorderPagesUri = null
            },
            title = {
                Text(
                    text = stringResource(R.string.reorder_pages_dialog_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = reorderPagesInput,
                    onValueChange = { reorderPagesInput = it },
                    label = { Text(stringResource(R.string.reorder_pages_dialog_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingReorderPagesUri
                        if (reorderPagesInput.isNotEmpty() && uri != null) {
                            viewModel.onIntent(PdfToolsIntent.ReorderPagesClicked(uri, reorderPagesInput))
                            reorderPagesDialogVisible = false
                            reorderPagesInput = ""
                            pendingReorderPagesUri = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_confirm), color = AccentVibrant)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reorderPagesDialogVisible = false
                        reorderPagesInput = ""
                        pendingReorderPagesUri = null
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_cancel), color = OnSurfaceDim)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (extractPageDialogVisible) {
        AlertDialog(
            onDismissRequest = { 
                extractPageDialogVisible = false
                extractPageInput = ""
                pendingExtractPageUri = null
            },
            title = {
                Text(
                    text = stringResource(R.string.extract_page_dialog_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = extractPageInput,
                    onValueChange = { extractPageInput = it },
                    label = { Text(stringResource(R.string.extract_page_dialog_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingExtractPageUri
                        val pageNum = extractPageInput.toIntOrNull()
                        if (pageNum != null && uri != null) {
                            viewModel.onIntent(PdfToolsIntent.ExtractPageClicked(uri, pageNum))
                            extractPageDialogVisible = false
                            extractPageInput = ""
                            pendingExtractPageUri = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_confirm), color = AccentVibrant)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        extractPageDialogVisible = false
                        extractPageInput = ""
                        pendingExtractPageUri = null
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_cancel), color = OnSurfaceDim)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (htmlToPdfDialogVisible) {
        AlertDialog(
            onDismissRequest = { 
                htmlToPdfDialogVisible = false
                htmlToPdfInput = ""
            },
            title = {
                Text(
                    text = stringResource(R.string.html_to_pdf_dialog_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = htmlToPdfInput,
                    onValueChange = { htmlToPdfInput = it },
                    label = { Text(stringResource(R.string.html_to_pdf_dialog_hint)) },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (htmlToPdfInput.isNotEmpty()) {
                            viewModel.onIntent(PdfToolsIntent.HtmlToPdfClicked(htmlToPdfInput))
                            htmlToPdfDialogVisible = false
                            htmlToPdfInput = ""
                        }
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_confirm), color = AccentVibrant)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        htmlToPdfDialogVisible = false
                        htmlToPdfInput = ""
                    }
                ) {
                    Text(stringResource(R.string.password_dialog_cancel), color = OnSurfaceDim)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Main UI ──
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back),
                                tint = OnSurfaceLight
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.pdf_tools_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Credit pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = AccentVibrant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.credit_balance_format, credits),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                pavansaiajayx.aipdfreadereditor.app.core.ads.InlineBannerAd()

                Spacer(modifier = Modifier.height(16.dp))

                // Tool Cards
                ToolCard(
                    icon = Icons.Rounded.CallMerge,
                    title = stringResource(R.string.tool_merge_title),
                    description = stringResource(R.string.tool_merge_desc),
                    onClick = onNavigateToMergePdf
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.ContentCut,
                    title = stringResource(R.string.tool_split_title),
                    description = stringResource(R.string.tool_split_desc),
                    onClick = onNavigateToSplitPdf
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.Star,
                    title = stringResource(R.string.tool_compress_title),
                    description = stringResource(R.string.tool_compress_desc),
                    onClick = { compressPicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.tool_encrypt_title),
                    description = stringResource(R.string.tool_encrypt_desc),
                    onClick = { encryptPicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.LockOpen,
                    title = stringResource(R.string.tool_decrypt_title),
                    description = stringResource(R.string.tool_decrypt_desc),
                    onClick = { decryptPicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.Image,
                    title = stringResource(R.string.tool_images_to_pdf_title),
                    description = stringResource(R.string.tool_images_to_pdf_desc),
                    onClick = { imagesToPdfPicker.launch("image/*") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.PictureAsPdf,
                    title = stringResource(R.string.tool_pdf_to_images_title),
                    description = stringResource(R.string.tool_pdf_to_images_desc),
                    onClick = { pdfToImagesPicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.TextFields,
                    title = stringResource(R.string.tool_watermark_title),
                    description = stringResource(R.string.tool_watermark_desc),
                    onClick = { addWatermarkPicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.Delete,
                    title = stringResource(R.string.tool_remove_pages_title),
                    description = stringResource(R.string.tool_remove_pages_desc),
                    onClick = onNavigateToDeletePdfPage
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.RotateRight,
                    title = stringResource(R.string.tool_rotate_pdf_title),
                    description = stringResource(R.string.tool_rotate_pdf_desc),
                    onClick = { rotatePdfPicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.FormatListNumbered,
                    title = stringResource(R.string.tool_reorder_pages_title),
                    description = stringResource(R.string.tool_reorder_pages_desc),
                    onClick = { reorderPagesPicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.tool_extract_text_title),
                    description = stringResource(R.string.tool_extract_text_desc),
                    onClick = { extractTextPicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.DocumentScanner,
                    title = stringResource(R.string.tool_scan_document_title),
                    description = stringResource(R.string.tool_scan_document_desc),
                    onClick = {
                        val options = GmsDocumentScannerOptions.Builder()
                            .setGalleryImportAllowed(true)
                            .setResultFormats(RESULT_FORMAT_PDF)
                            .setScannerMode(SCANNER_MODE_FULL)
                            .build()
                        val scanner = GmsDocumentScanning.getClient(options)
                        (context as? Activity)?.let { activity ->
                            scanner.getStartScanIntent(activity)
                                .addOnSuccessListener { intentSender ->
                                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.ImageSearch,
                    title = stringResource(R.string.tool_ocr_image_title),
                    description = stringResource(R.string.tool_ocr_image_desc),
                    costHint = stringResource(R.string.tool_cost_hint),
                    onClick = { ocrPicker.launch("image/*") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.PictureAsPdf,
                    title = stringResource(R.string.tool_extract_page_title),
                    description = stringResource(R.string.tool_extract_page_desc),
                    onClick = onNavigateToExtractPages
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.CallMerge,
                    title = stringResource(R.string.tool_flatten_pdf_title),
                    description = stringResource(R.string.tool_flatten_pdf_desc),
                    onClick = { flattenPdfPicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ToolCard(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.tool_html_to_pdf_title),
                    description = stringResource(R.string.tool_html_to_pdf_desc),
                    onClick = { htmlToPdfDialogVisible = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Success state — save section
                val currentState = state
                if (currentState is PdfToolsState.Success) {
                    val successMsg = when (currentState.toolName) {
                        "merge" -> mergeSuccessMsg
                        "split" -> splitSuccessMsg
                        "compress" -> compressSuccessMsg
                        "encrypt" -> encryptSuccessMsg
                        "decrypt" -> decryptSuccessMsg
                        "images_to_pdf" -> imagesToPdfSuccessMsg
                        "pdf_to_images" -> pdfToImagesSuccessMsg
                        "add_watermark" -> watermarkSuccessMsg
                        "extract_text" -> extractTextSuccessMsg
                        "ocr_image" -> ocrSuccessMsg
                        "scan_document" -> scanSuccessMsg
                        "delete_pages" -> removePagesSuccessMsg
                        "rotate_pdf" -> rotatePdfSuccessMsg
                        "reorder_pages" -> reorderPagesSuccessMsg
                        "extract_page" -> extractPageSuccessMsg
                        "flatten_pdf" -> flattenPdfSuccessMsg
                        "html_to_pdf" -> htmlToPdfSuccessMsg
                        else -> ""
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceVariant)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = AccentVibrant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = successMsg,
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (currentState.toolName == "split" || currentState.toolName == "pdf_to_images") {
                                    saveMultipleFiles.launch(null)
                                } else if (currentState.toolName == "extract_text" || currentState.toolName == "ocr_image") {
                                    saveTextFile.launch("output.txt")
                                } else {
                                    saveSingleFile.launch("output.pdf")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentVibrant,
                                contentColor = DeepBlack
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.save_file_button),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Processing overlay
            if (state is PdfToolsState.Processing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepBlack.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = AccentVibrant,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.processing_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    costHint: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AccentVibrant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceLight,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
            if (costHint != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = costHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentVibrant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
