import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.prompt.GenerateContentResponse

import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GeminiRepository {
    private val generativeModel = Generation.getClient()

    /**
     * Retorna un Flow con el estado de la preparación del modelo.
     */
    fun prepareModelFlow(): Flow<ModelState> = callbackFlow {
        generativeModel.download().collect { status ->
            when (status) {
                is DownloadStatus.DownloadStarted ->
                    trySend(ModelState.Downloading(0f))
                is DownloadStatus.DownloadProgress -> {
                    val progress = status.totalBytesDownloaded.toFloat() / 300_000_000f
                    trySend(ModelState.Downloading(progress))
                }
                is DownloadStatus.DownloadCompleted -> {
                    trySend(ModelState.Ready)
                    close()
                }
                is DownloadStatus.DownloadFailed -> {
                    trySend(ModelState.Error("Fallo la descarga: ${status.e.message}"))
                    close(status.e)
                }
            }
        }
        awaitClose { /* Cancelar suscripciones si fuera necesario */ }
    }

    /**
     * Función que llama a nuestro modelo de IA para generar contenido.
     */
    suspend fun generateContent(fullPrompt: String): String {
        return try {
            // ML Kit GenAI en beta usa generateContent
            val result: GenerateContentResponse = generativeModel.generateContent(fullPrompt)
            result.candidates.firstOrNull()?.text ?: "Sin respuesta."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }
}

sealed class ModelState {
    object Checking : ModelState()
    data class Downloading(val progress: Float) : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}