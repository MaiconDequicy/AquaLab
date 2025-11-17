package br.iots.aqualab.model

data class ChatGptRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatGptResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessage
)