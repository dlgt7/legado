package io.legado.app.ai

import io.legado.app.constant.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiChatManager(
    private val provider: AiProvider,
    private val maxToolRounds: Int = 5
) {

    private val messages = mutableListOf<AiMessage>()
    private val tools = McpToolRegistry.listTools()

    fun setSystemPrompt(prompt: String) {
        messages.removeAll { it.role == "system" }
        messages.add(0, AiMessage("system", prompt))
    }

    fun getMessages(): List<AiMessage> = messages.toList()

    suspend fun chat(userMessage: String): String = withContext(Dispatchers.IO) {
        messages.add(AiMessage("user", userMessage))

        var round = 0
        while (round < maxToolRounds) {
            round++
            val response = provider.chat(messages, tools)

            if (response.toolCalls.isNullOrEmpty()) {
                messages.add(response)
                return@withContext response.content ?: ""
            }

            messages.add(response)

            for (toolCall in response.toolCalls) {
                val result = executeToolCall(toolCall)
                messages.add(AiMessage(
                    role = "tool",
                    content = result,
                    toolCallId = toolCall.id,
                    name = toolCall.function.name
                ))
            }
        }

        "达到工具调用轮次上限 ($maxToolRounds)，请简化请求"
    }

    private suspend fun executeToolCall(toolCall: ToolCall): String {
        val funcName = toolCall.function.name
        val handler = McpToolRegistry.getHandler(funcName)
        if (handler == null) {
            AppLog.put("MCP 工具不存在: $funcName")
            return "{\"error\": \"工具不存在: $funcName\"}"
        }

        val params = try {
            parseArguments(toolCall.function.arguments)
        } catch (e: Exception) {
            AppLog.put("MCP 参数解析失败: $funcName", e)
            return "{\"error\": \"参数解析失败\"}"
        }

        return try {
            val result = handler.execute(params)
            if (result.success) {
                result.content
            } else {
                "{\"error\": \"${result.error}\"}"
            }
        } catch (e: Exception) {
            AppLog.put("MCP 工具执行失败: $funcName", e)
            "{\"error\": \"${e.message}\"}"
        }
    }

    private fun parseArguments(args: String): Map<String, Any?> {
        if (args.isBlank()) return emptyMap()
        val json = com.google.gson.JsonParser.parseString(args).asJsonObject
        val map = mutableMapOf<String, Any?>()
        for (entry in json.entrySet()) {
            val value = entry.value
            map[entry.key] = when {
                value.isJsonNull -> null
                value.isJsonPrimitive -> {
                    val prim = value.asJsonPrimitive
                    when {
                        prim.isBoolean -> prim.asBoolean
                        prim.isNumber -> prim.asNumber
                        prim.isString -> prim.asString
                        else -> prim.toString()
                    }
                }
                else -> value.toString()
            }
        }
        return map
    }

    fun clearHistory() {
        val systemMsg = messages.find { it.role == "system" }
        messages.clear()
        systemMsg?.let { messages.add(it) }
    }

}