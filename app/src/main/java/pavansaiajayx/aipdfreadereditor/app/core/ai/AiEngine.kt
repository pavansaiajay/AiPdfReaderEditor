package pavansaiajayx.aipdfreadereditor.app.core.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiEngine @Inject constructor() {
    private val primaryModel = Firebase.ai.generativeModel("gemini-3.7-flash")
    private val fallbackModel = Firebase.ai.generativeModel("gemini-3.5-flash-lite")

    suspend fun generateSummary(text: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            android.util.Log.d("AiEngine_Debug", "Starting summary request. Text length: ${text.length} chars")
            val prompt = "You are an expert document analyzer. Summarize the following text clearly and concisely using markdown bullet points. Here is the text: \n\n $text"
            
            var attempt = 1
            while (attempt <= 2) {
                try {
                    android.util.Log.d("AiEngine_Debug", "Calling Firebase AI generateContent (Attempt $attempt)...")
                    val response = primaryModel.generateContent(prompt)
                    android.util.Log.d("AiEngine_Debug", "AI Response received: ${response.text?.take(100)}...")
                    return@withContext response.text ?: "Empty response from AI"
                } catch (e: Exception) {
                    android.util.Log.e("AiEngine_Debug", "Attempt $attempt failed: ${e.javaClass.simpleName} - ${e.message}")
                    if (attempt == 1) {
                        delay(2000)
                    } else {
                        android.util.Log.d("AiEngine_Debug", "Falling back to lite model")
                    }
                }
                attempt++
            }

            try {
                val response = fallbackModel.generateContent(prompt)
                android.util.Log.d("AiEngine_Debug", "Fallback AI Response received: ${response.text?.take(100)}...")
                return@withContext response.text ?: "Empty response from AI"
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
                android.util.Log.e("AiEngine_Debug", "Fallback model also failed: ${e.javaClass.simpleName} - ${e.message}", e)
                throw Exception("AI servers are currently overloaded. Please try again later.", e)
            }
        }
    }

    fun startChatSession(pdfContext: String): com.google.firebase.ai.Chat {
        val chatModel = Firebase.ai.generativeModel("gemini-3.5-flash-lite")
        return chatModel.startChat(
            listOf(
                com.google.firebase.ai.type.content("user") { text("Here is the document context. Answer all future questions based strictly on this: \n\n $pdfContext") },
                com.google.firebase.ai.type.content("model") { text("Understood. I am ready to answer questions based on the document.") }
            )
        )
    }

    suspend fun sendMessage(chat: com.google.firebase.ai.Chat, message: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            var attempt = 1
            while (attempt <= 2) {
                try {
                    val response = chat.sendMessage(message)
                    return@withContext response.text ?: "Empty response from AI"
                } catch (e: Exception) {
                    if (attempt == 1) {
                        delay(2000)
                    } else {
                        throw Exception("AI servers are currently overloaded. Please try again later.", e)
                    }
                }
                attempt++
            }
            throw Exception("Failed to send message")
        }

    }
}
