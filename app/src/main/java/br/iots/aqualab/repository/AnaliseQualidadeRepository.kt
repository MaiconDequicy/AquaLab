package br.iots.aqualab.repository

import br.iots.aqualab.BuildConfig
import br.iots.aqualab.model.ChatGptRequest
import br.iots.aqualab.model.ChatMessage
import br.iots.aqualab.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnaliseQualidadeRepository {
    suspend fun obterAnalise(prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val chatRequest = ChatGptRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(
                        ChatMessage("system", "Você é um assistente especializado em monitoramento ambiental..."),
                        ChatMessage("user", prompt)
                    )
                )
                val response = RetrofitInstance.chatGptApiService.getChatCompletion(
                    apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}",
                    request = chatRequest
                )
                val content = response.choices.firstOrNull()?.message?.content ?: ""
                if (content.isEmpty()) {
                    Result.failure(Exception("Resposta da IA está vazia."))
                } else {
                    Result.success(content)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
     