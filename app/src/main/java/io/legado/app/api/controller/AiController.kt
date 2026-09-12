package io.legado.app.api.controller

import io.legado.app.ai.AiChatManager
import io.legado.app.ai.AiConfig
import io.legado.app.ai.McpToolRegistry
import io.legado.app.ai.OpenAiProvider
import io.legado.app.api.ReturnData
import io.legado.app.data.appDb
import io.legado.app.data.entities.Thought
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.GSON
import kotlinx.coroutines.runBlocking

object AiController {

    private var chatManager: AiChatManager? = null

    fun getThoughts(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val bookName = parameters["bookName"]?.firstOrNull()
        val key = parameters["key"]?.firstOrNull()
        val thoughts = when {
            !bookName.isNullOrBlank() -> appDb.thoughtDao.getByBook(bookName, "")
            !key.isNullOrBlank() -> appDb.thoughtDao.search(key)
            else -> appDb.thoughtDao.all
        }
        return returnData.setData(thoughts)
    }

    fun addThought(postData: String?): ReturnData {
        val returnData = ReturnData()
        try {
            val thought = GSON.fromJson(postData, Thought::class.java)
                ?: return returnData.setErrorMsg("数据解析失败")
            appDb.thoughtDao.insert(thought)
            return returnData.setData("添加成功")
        } catch (e: Exception) {
            return returnData.setErrorMsg(e.message ?: "添加失败")
        }
    }

    fun deleteThought(postData: String?): ReturnData {
        val returnData = ReturnData()
        try {
            val time = postData?.toLongOrNull()
                ?: return returnData.setErrorMsg("需要 time 参数")
            appDb.thoughtDao.delete(time)
            return returnData.setData("删除成功")
        } catch (e: Exception) {
            return returnData.setErrorMsg(e.message ?: "删除失败")
        }
    }

    fun getBookmarks(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val bookName = parameters["bookName"]?.firstOrNull()
            ?: return returnData.setErrorMsg("需要 bookName 参数")
        val bookAuthor = parameters["bookAuthor"]?.firstOrNull() ?: ""
        val bookmarks = appDb.bookmarkDao.getByBook(bookName, bookAuthor)
        return returnData.setData(bookmarks)
    }

    fun getReadRecord(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val key = parameters["key"]?.firstOrNull()
        val records = if (key.isNullOrBlank()) {
            appDb.readRecordDao.allShow
        } else {
            appDb.readRecordDao.search(key)
        }
        return returnData.setData(records)
    }

    fun getAiTools(): ReturnData {
        val returnData = ReturnData()
        return returnData.setData(McpToolRegistry.listTools())
    }

    fun aiChat(postData: String?): ReturnData {
        val returnData = ReturnData()
        val message = postData
            ?: return returnData.setErrorMsg("需要消息内容")
        val apiKey = AppConfig.aiApiKey
        if (apiKey.isBlank()) {
            return returnData.setErrorMsg("未配置 AI API Key")
        }
        try {
            if (chatManager == null) {
                val config = AiConfig(
                    apiKey = apiKey,
                    baseUrl = AppConfig.aiBaseUrl,
                    model = AppConfig.aiModel,
                    systemPrompt = AppConfig.aiSystemPrompt
                )
                chatManager = AiChatManager(OpenAiProvider(config)).apply {
                    setSystemPrompt(config.systemPrompt ?: "")
                }
            }
            val response = runBlocking { chatManager!!.chat(message) }
            return returnData.setData(response)
        } catch (e: Exception) {
            return returnData.setErrorMsg(e.message ?: "AI 请求失败")
        }
    }

    fun clearAiHistory(): ReturnData {
        val returnData = ReturnData()
        chatManager?.clearHistory()
        return returnData.setData("已清空")
    }

}