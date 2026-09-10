package pavansaiajayx.aipdfreadereditor.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.google.android.gms.ads.MobileAds
import com.rajat.pdfviewer.PdfRendererView
import com.rajat.pdfviewer.compose.PdfRendererViewCompose
import com.rajat.pdfviewer.util.PdfSource
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pavansaiajayx.aipdfreadereditor.app.PdfManipulation.extractText
import pavansaiajayx.aipdfreadereditor.app.PdfManipulation.mergePdfs
import pavansaiajayx.aipdfreadereditor.app.PdfManipulation.splitPdf
import pavansaiajayx.aipdfreadereditor.app.ui.home.HomeScreen
import pavansaiajayx.aipdfreadereditor.app.ui.theme.AiPdfReaderEditorTheme
import java.io.File
import kotlin.collections.emptyList

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(this)

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {}

        enableEdgeToEdge()
        setContent {
            AiPdfReaderEditorTheme {
                HomeScreen()
            }
        }
    }
}

private object PdfManipulation {
    suspend fun extractText(
        context: Context,
        uri: Uri
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream)?.use {
                        PDFTextStripper().getText(it)
                    }
                } ?: "Unable to open pdf"
            } catch (e: Exception) {
                "${e.message}"
            }
        }
    }

    suspend fun mergePdfs(
        context: Context,
        uriList: List<Uri>,
        outputFile: File
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfMerger = PDFMergerUtility()
                for (uri in uriList) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        pdfMerger.addSource(inputStream)
                    }
                }
                outputFile.outputStream().use {
                    pdfMerger.destinationStream = it
                    pdfMerger.mergeDocuments(null)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun splitPdf(
        context: Context,
        uri: Uri,
        outputDirectory: File
    ): List<File> {
        return withContext(Dispatchers.IO) {
            val generatedFiles = mutableListOf<File>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream)?.use { document ->
                        val splitter = Splitter()
                        val splitPdfs = splitter.split(document)
                        splitPdfs.forEachIndexed { index, document ->
                            val newFile = File(outputDirectory, "SplitPdf${index + 1}.pdf")
                            document.use {
                                it.save(newFile)
                            }
                            generatedFiles.add(newFile)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            generatedFiles
        }
    }
}

@Composable
private fun MergePdfs(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isMerging by remember {
        mutableStateOf(false)
    }
    var mergedTempFile by remember {
        mutableStateOf<File?>(null)
    }
    val pickMultiplePdfs = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) {
        if (it.size > 1) {
            scope.launch {
                isMerging = true
                val temporaryFile =
                    File(context.cacheDir, "TemporaryMergedFile${System.currentTimeMillis()}.pdf")
                val isPdfsMerged = mergePdfs(
                    context,
                    it,
                    temporaryFile
                )
                if (isPdfsMerged) {
                    mergedTempFile = temporaryFile
                    Toast.makeText(context, "Pdf's Successfully Merged!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Merge Failed!!", Toast.LENGTH_SHORT).show()
                }
                isMerging = false
            }
        } else {
            Toast.makeText(context, "Please Select At Least Two Pdf's!", Toast.LENGTH_SHORT).show()
        }
    }
    val saveMergedPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) {
        if (it != null && mergedTempFile != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        mergedTempFile!!.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Successfully saved!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                pickMultiplePdfs.launch("application/pdf")
            },
            enabled = !isMerging
        ) {
            if (isMerging) CircularProgressIndicator() else Text("Merge Pdf's")
        }
        mergedTempFile?.let {
            Button(
                onClick = {
                    saveMergedPdf.launch(it.name)
                }
            ) {
                Text("Save Merged Pdf")
            }
            PdfRendererViewCompose(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                source = PdfSource.LocalFile(it),
                statusCallBack = object : PdfRendererView.StatusCallBack {
                    override fun onError(error: Throwable) {
                        super.onError(error)
                        Toast.makeText(context, "Unable to open pdf!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun TextExtractor(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pdfUri by remember {
        mutableStateOf<Uri?>(null)
    }
    val openPdfFromLocal = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        pdfUri = it
    }
    val scope = rememberCoroutineScope()
    var stripedText by remember {
        mutableStateOf("")
    }
    var isExtracting by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                openPdfFromLocal.launch("application/pdf")
            }
        ) {
            Text("Open Pdf")
        }
        pdfUri?.let {
            PdfRendererViewCompose(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                source = PdfSource.LocalUri(it),
                statusCallBack = object : PdfRendererView.StatusCallBack {
                    override fun onError(error: Throwable) {
                        super.onError(error)
                        Toast.makeText(context, "Unable to open pdf!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Button(
                onClick = {
                    scope.launch {
                        isExtracting = true
                        stripedText = extractText(
                            context,
                            it
                        )
                        isExtracting = false
                    }
                },
                enabled = !isExtracting
            ) {
                if (isExtracting) CircularProgressIndicator() else Text("Extract Text")
            }
            Text(stripedText)
        }
    }
}

@Composable
fun SplitPdf(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var isSplitting by remember {
        mutableStateOf(false)
    }
    var splitTempFiles by remember {
        mutableStateOf<List<File>>(emptyList())
    }
    val openPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null){
            pdfUri = uri
            val temporaryCacheDirectory = File(context.cacheDir, "TemporarySplitFiles")
            temporaryCacheDirectory.mkdirs()
            scope.launch(Dispatchers.IO){
                isSplitting = true
                val resultSplitPdfs = splitPdf(
                    context,
                    uri,
                    temporaryCacheDirectory
                )
                withContext(Dispatchers.Main){
                    if (resultSplitPdfs.isNotEmpty()){
                        splitTempFiles = resultSplitPdfs
                        Toast.makeText(context, "Split Successful!", Toast.LENGTH_SHORT).show()
                    } else{
                        Toast.makeText(context, "Split Failed!", Toast.LENGTH_SHORT).show()
                    }
                }
                isSplitting = false
            }
        }
    }
    val saveSplitPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null && splitTempFiles.isNotEmpty()){
            scope.launch(Dispatchers.Main){
                try {
                    withContext(Dispatchers.IO){
                        val pickedDirectory = DocumentFile.fromTreeUri(context, uri)
                        splitTempFiles.forEach { temporaryFile ->
                            val newFile = pickedDirectory?.createFile("application/pdf", temporaryFile.name)
                            newFile?.uri?.let { destinationUri ->
                                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                                    temporaryFile.inputStream().use { inputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                            }
                        }
                    }
                    Toast.makeText(context, "Files saved successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                openPdf.launch("application/pdf")
            },
            enabled = !isSplitting && splitTempFiles.isEmpty()
        ) {
            Text("Open Pdf")
        }
        pdfUri?.let { uri ->
            Button(
                onClick = {
                    saveSplitPdf.launch(null)
                }
            ) {
                Text("Save Split Pdf's")
            }
            PdfRendererViewCompose(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                source = PdfSource.LocalUri(uri),
            )
        }
    }
}
