package pavansaiajayx.aipdfreadereditor.app.ui.tools.grid

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractPagesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExtractPagesViewModel = hiltViewModel()
) {
    val selectedPdfUri by viewModel.selectedPdfUri.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val selectedPages by viewModel.selectedPages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val error by viewModel.error.collectAsState()
    val successUri by viewModel.successUri.collectAsState()
    
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.loadPdf(uri)
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(error ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }

    if (successUri != null) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.dismissSuccess()
                onNavigateBack()
            },
            title = { Text("Success") },
            text = { Text("File has been saved to your Documents/AiPdfReaderEditor folder.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.dismissSuccess()
                    onNavigateBack()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extract Pages") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.extractPages() },
                        enabled = selectedPages.isNotEmpty() && !isProcessing
                    ) {
                        Text("Extract (${selectedPages.size})")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedPdfUri != null) {
                PdfThumbnailGrid(
                    uri = selectedPdfUri!!,
                    pageCount = pageCount,
                    selectedPages = selectedPages,
                    onPageSelected = { viewModel.togglePageSelection(it) },
                    onDragSelectRange = { start, end -> viewModel.selectRange(start, end) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { pdfPicker.launch("application/pdf") }) {
                        Text("Select PDF")
                    }
                }
            }
            
            if (isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
