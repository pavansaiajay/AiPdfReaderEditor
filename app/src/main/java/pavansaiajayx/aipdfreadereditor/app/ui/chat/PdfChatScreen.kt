package pavansaiajayx.aipdfreadereditor.app.ui.chat

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.app.R
import pavansaiajayx.aipdfreadereditor.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfChatScreen(
    fileUri: String,
    onNavigateBack: () -> Unit,
    viewModel: PdfChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    
    LaunchedEffect(fileUri) {
        viewModel.initializeChat(Uri.parse(fileUri))
    }

    LaunchedEffect(state.chatHistory.size) {
        if (state.chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(state.chatHistory.size - 1)
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    if (state.showInsufficientCreditsDialog) {
        val currentContext = LocalContext.current
        val activity = currentContext as? Activity

        AlertDialog(
            onDismissRequest = { viewModel.dismissInsufficientCreditsDialog() },
            title = { Text("Out of Credits") },
            text = { Text("You need more credits to chat with this PDF. Watch a short ad to earn 5 free credits!") },
            confirmButton = {
                Button(onClick = { 
                    activity?.let { viewModel.watchAdForCredits(it) } 
                }) {
                    Text("Watch Ad (+5)")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissInsufficientCreditsDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_with_pdf), color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.type_a_message), color = OnSurfaceLight) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariant,
                        unfocusedContainerColor = SurfaceVariant,
                        focusedBorderColor = AccentVibrant,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !state.isExtractingText && !state.isAiTyping
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !state.isExtractingText && !state.isAiTyping,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (inputText.isNotBlank() && !state.isExtractingText && !state.isAiTyping) AccentVibrant else SurfaceVariant)
                ) {
                    if (state.isExtractingText || state.isAiTyping) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentVibrant, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(DeepBlack)) {
            if (state.isExtractingText && state.chatHistory.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentVibrant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.analyzing_context), color = OnSurfaceLight)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.chatHistory) { message ->
                        val isUser = message.sender == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    ))
                                    .background(if (isUser) AccentVibrant else SurfaceVariant)
                                    .padding(12.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = message.text,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    if (state.isAiTyping) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                        .background(SurfaceVariant)
                                        .padding(12.dp)
                                ) {
                                    Text(stringResource(R.string.ai_is_typing), color = OnSurfaceLight)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
