package io.legado.app.ai

import com.google.gson.JsonObject

data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

data class McpToolResult(
    val success: Boolean,
    val content: String,
    val error: String? = null
) {
    companion object {
        fun ok(content: String) = McpToolResult(true, content)
        fun error(message: String) = McpToolResult(false, "", message)
    }
}

interface McpToolHandler {
    suspend fun execute(params: Map<String, Any?>): McpToolResult
}