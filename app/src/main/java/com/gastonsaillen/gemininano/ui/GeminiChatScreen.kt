import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GeminiChatScreen(geminiViewModel: GeminiViewModel = viewModel()) {
    val state = geminiViewModel.uiState
    var inputText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()

    // Autoscroll al último mensaje
    LaunchedEffect(state) {
        if (state is ChatUiState.Idle) {
            scrollState.animateScrollToItem(state.messages.size)
        }
    }

    Scaffold( /* ... tu TopBar se mantiene igual ... */ ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (state) {
                    is ChatUiState.Loading -> { /* ... Tu indicador de progreso ... */ }
                    is ChatUiState.Idle, is ChatUiState.Generating -> {
                        val messages = (state as? ChatUiState.Idle)?.messages
                            ?: (state as? ChatUiState.Generating)?.let {
                                // Muestra mensajes anteriores mientras genera
                                geminiViewModel.uiState.let { (it as? ChatUiState.Idle)?.messages }
                            } ?: emptyList()

                        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                            items(messages) { message ->
                                ChatBubble(message)
                            }
                            if (state is ChatUiState.Generating) {
                                item { Text("Gemini está escribiendo...", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                    is ChatUiState.Error -> { /* ... */ }
                    else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            // Input Row (Se mantiene similar, solo habilitado en Idle)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    enabled = state is ChatUiState.Idle
                )
                IconButton(onClick = {
                    geminiViewModel.sendPrompt(inputText)
                    inputText = ""
                }, enabled = inputText.isNotBlank() && state is ChatUiState.Idle) {
                    Icon(Icons.Default.Send, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = alignment) {
        Surface(shape = MaterialTheme.shapes.medium, color = color) {
            Text(text = message.text, modifier = Modifier.padding(12.dp))
        }
    }
}