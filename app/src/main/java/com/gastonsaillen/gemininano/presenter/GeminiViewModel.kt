import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GeminiViewModel(private val repository: GeminiRepository = GeminiRepository()) : ViewModel() {

    var uiState by mutableStateOf<ChatUiState>(ChatUiState.Initializing)
        private set

    // Lista interna para mantener el historial
    private val _messages = mutableStateListOf<ChatMessage>()

    init {
        prepareIA()
    }

    private fun prepareIA() {
        viewModelScope.launch {
            repository.prepareModelFlow().collectLatest { state ->
                uiState = when (state) {
                    is ModelState.Downloading -> ChatUiState.Loading(state.progress)
                    is ModelState.Ready -> {
                        val welcomeMsg = "¡Hola! Soy Gemini Nano. ¿En qué puedo ayudarte?"
                        _messages.add(ChatMessage(welcomeMsg, isUser = false))
                        ChatUiState.Idle(_messages)
                    }
                    is ModelState.Error -> ChatUiState.Error(state.message)
                    else -> ChatUiState.Initializing
                }
            }
        }
    }

    fun sendPrompt(text: String) {
        // 1. Verificación rápida (Guard Clause)
        // Solo enviamos si hay texto y si la IA no está ocupada
        if (text.isBlank() || uiState !is ChatUiState.Idle) return

        viewModelScope.launch {
            // 2. Agregamos el mensaje del usuario a nuestra "fuente de verdad"
            _messages.add(ChatMessage(text, isUser = true))

            // Cambiamos a estado de carga para mostrar el "Pensando..."
            uiState = ChatUiState.Generating

            // 3. Construimos el "Contexto" (Esto reemplaza tu idea de previousMessage)
            // Convertimos toda la lista en un solo String para que Gemini Nano sepa de qué hablamos
            val fullPrompt = _messages.joinToString("\n") { msg ->
                if (msg.isUser) "User: ${msg.text}" else "Model: ${msg.text}"
            }

            val response = repository.generateContent(fullPrompt)

            // 4. Actualizamos la lista con la respuesta y volvemos a Idle
            _messages.add(ChatMessage(response, isUser = false))
            uiState = ChatUiState.Idle(_messages)
        }
    }
}

// Modelo para los mensajes
data class ChatMessage(val text: String, val isUser: Boolean)

sealed class ChatUiState {
    object Initializing : ChatUiState()
    data class Loading(val progress: Float) : ChatUiState()
    data class Idle(val messages: List<ChatMessage>) : ChatUiState()
    object Generating : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}