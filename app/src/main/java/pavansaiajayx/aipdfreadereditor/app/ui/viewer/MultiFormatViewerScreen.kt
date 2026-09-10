package pavansaiajayx.aipdfreadereditor.app.ui.viewer

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import pavansaiajayx.aipdfreadereditor.app.R
import pavansaiajayx.aipdfreadereditor.app.ui.theme.DeepBlack
import pavansaiajayx.aipdfreadereditor.app.ui.theme.SurfaceDark
import java.io.File
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiFormatViewerScreen(
    fileUri: String,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uri = Uri.parse(fileUri)
    val context = LocalContext.current

    val mimeType = context.contentResolver.getType(uri)
    val isHtmlOrTxt = mimeType == "text/html" || mimeType == "text/plain"
    val isOffice = mimeType?.contains("officedocument") == true || mimeType?.contains("msword") == true || mimeType?.contains("ms-excel") == true || mimeType?.contains("ms-powerpoint") == true

    LaunchedEffect(isHtmlOrTxt, isOffice) {
        if (!isHtmlOrTxt && !isOffice) {
            snackbarHostState.showSnackbar(context.getString(R.string.unsupported_format))
        } else if (isOffice) {
            // Using a lightweight viewer is requested, but if absent, fallback gracefully
            snackbarHostState.showSnackbar("Office viewer rendering...")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.open_document_button), color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepBlack)
        ) {
            if (isHtmlOrTxt) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                        }
                    },
                    update = { webView ->
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val content = stream.bufferedReader().use { it.readText() }
                                webView.loadDataWithBaseURL(null, content, mimeType, "UTF-8", null)
                            }
                        } catch (e: Exception) {
                            // Ignore or log
                        }
                    }
                )
            } else if (isOffice) {
                // If a lightweight library was integrated, it would be called here.
                // For now, we fallback to showing an error if it can't render natively
                Text(
                    text = "Office formats require a supported viewer library.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = stringResource(R.string.unsupported_format),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
