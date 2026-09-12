package io.legado.app.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAiProvider(private val config: AiConfig) : AiProvider {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    override suspend fun chat(
        messages: List<AiMessage>,
        tools: List<McpTool>?
    ): AiMessage = withContext(Dispatchers.IO) {
        val body = buildRequestBody(messages, tools)
        val request = Request.Builder()
            .url("${config.baseUrl}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(jsonMedia))
            .build()

        val response = okHttpClient.newCall(request).execute()
        val respBody = response.body.string()


        if (!response.isSuccessful) {
            throw RuntimeException("API 错误 ${response.code}: $respBody")
        }

        parseResponse(respBody)
    }

    private fun buildRequestBody(messages: List<AiMessage>, tools: List<McpTool>?): String {
        val root = JsonObject()
        root.addProperty("model", config.model)

        val msgArray = JsonArray()
        messages.forEach { msg ->
            val msgObj = JsonObject()
            msgObj.addProperty("role", msg.role)
            msg.content?.let { msgObj.addProperty("content", it) }
            msg.name?.let { msgObj.addProperty("name", it) }
            msg.toolCallId?.let { msgObj.addProperty("tool_call_id", it) }
            msg.toolCalls?.let { calls ->
                val callsArray = JsonArray()
                calls.forEach { call ->
                    val callObj = JsonObject()
                    callObj.addProperty("id", call.id)
                    callObj.addProperty("type", "function")
                    val funcObj = JsonObject()
                    funcObj.addProperty("name", call.function.name)
                    funcObj.addProperty("arguments", call.function.arguments)
                    callObj.add("function", funcObj)
                    callsArray.add(callObj)
                }
                msgObj.add("tool_calls", callsArray)
            }
            msgArray.add(msgObj)
        }
        root.add("messages", msgArray)

        if (!tools.isNullOrEmpty()) {
            val toolsArray = JsonArray()
            tools.forEach { tool ->
                val toolObj = JsonObject()
                toolObj.addProperty("type", "function")
                val funcObj = JsonObject()
                funcObj.addProperty("name", tool.name)
                funcObj.addProperty("description", tool.description)
                funcObj.add("parameters", tool.inputSchema)
                toolObj.add("function", funcObj)
                toolsArray.add(toolObj)
            }
            root.add("tools", toolsArray)
        }

        return root.toString()
    }

    private fun parseResponse(respBody: String): AiMessage {
        val json = JsonParser.parseString(respBody).asJsonObject
        val choices = json.getAsJsonArray("choices")
        if (choices == null || choices.size() == 0) {
            throw RuntimeException("无 choices")
        }
        val message = choices[0].asJsonObject.getAsJsonObject("message")
        val role = message.get("role")?.asString ?: "assistant"
        val content = message.get("content")?.let {
            if (it.isJsonNull) null else it.asString
        }

        val toolCalls = message.getAsJsonArray("tool_calls")?.let { callsArray ->
            callsArray.map { callElem ->
                val callObj = callElem.asJsonObject
                val funcObj = callObj.getAsJsonObject("function")
                ToolCall(
                    id = callObj.get("id").asString,
                    function = ToolFunction(
                        name = funcObj.get("name").asString,
                        arguments = funcObj.get("arguments").asString
                    )
                )
            }
        }

        return AiMessage(role, content, toolCalls)
    }

}