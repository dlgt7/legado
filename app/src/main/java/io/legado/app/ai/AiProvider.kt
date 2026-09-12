package io.legado.app.ai

import com.google.gson.JsonObject

data class AiMessage(
    val role: String,
    val content: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val name: String? = null
)

data class ToolCall(
    val id: String,
    val function: ToolFunction
)

data class ToolFunction(
    val name: String,
    val arguments: String
)

data class AiConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String? = null
)

interface AiProvider {
    suspend fun chat(
        messages: List<AiMessage>,
        tools: List<McpTool>? = null
    ): AiMessage
}