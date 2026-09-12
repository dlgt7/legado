package io.legado.app.ui.assistant

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.ai.AiChatManager
import io.legado.app.ai.AiConfig
import io.legado.app.ai.OpenAiProvider
import io.legado.app.base.BaseViewModel
import io.legado.app.help.config.AppConfig

class AiChatViewModel(application: Application) : BaseViewModel(application) {

    val messagesLiveData = MutableLiveData<List<ChatMessage>>(emptyList())
    val errorLiveData = MutableLiveData<String>()
    val loadingLiveData = MutableLiveData(false)

    private var chatManager: AiChatManager? = null

    private val messages = mutableListOf<ChatMessage>()

    fun initChat() {
        val apiKey = AppConfig.aiApiKey
        if (apiKey.isBlank()) {
            errorLiveData.postValue("请先在设置中配置 API Key")
            return
        }
        val config = AiConfig(
            apiKey = apiKey,
            baseUrl = AppConfig.aiBaseUrl,
            model = AppConfig.aiModel,
            systemPrompt = AppConfig.aiSystemPrompt
        )
        val provider = OpenAiProvider(config)
        chatManager = AiChatManager(provider).apply {
            setSystemPrompt(config.systemPrompt ?: "")
        }
    }

    fun send(userMessage: String) {
        if (userMessage.isBlank()) return
        val manager = chatManager
        if (manager == null) {
            errorLiveData.postValue("请先配置 API Key")
            return
        }

        messages.add(ChatMessage("user", userMessage))
        messagesLiveData.postValue(messages.toList())
        loadingLiveData.postValue(true)

        execute {
            try {
                val response = manager.chat(userMessage)
                messages.add(ChatMessage("assistant", response))
                messagesLiveData.postValue(messages.toList())
            } catch (e: Exception) {
                errorLiveData.postValue(e.message ?: "请求失败")
            } finally {
                loadingLiveData.postValue(false)
            }
        }
    }

    fun clearHistory() {
        messages.clear()
        messagesLiveData.postValue(emptyList())
        chatManager?.clearHistory()
    }

}