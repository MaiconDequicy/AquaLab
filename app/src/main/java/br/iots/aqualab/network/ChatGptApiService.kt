package br.iots.aqualab.network

import br.iots.aqualab.model.ChatGptRequest
import br.iots.aqualab.model.ChatGptResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatGptApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatGptRequest
    ): ChatGptResponse
}
    