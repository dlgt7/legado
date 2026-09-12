package io.legado.app.ui.assistant

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityAiChatBinding
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

class AiChatActivity : VMBaseActivity<ActivityAiChatBinding, AiChatViewModel>() {

    override val binding by viewBinding(ActivityAiChatBinding::inflate)
    override val viewModel by viewModels<AiChatViewModel>()

    private val adapter by lazy { AiChatAdapter() }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rvMsgList.layoutManager = LinearLayoutManager(this)
        binding.rvMsgList.adapter = adapter

        viewModel.messagesLiveData.observe(this) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvMsgList.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.errorLiveData.observe(this) { error ->
            toastOnUi(error)
        }

        viewModel.loadingLiveData.observe(this) { loading ->
            binding.btnSend.isEnabled = !loading
            binding.etInput.isEnabled = !loading
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.etInput.text.clear()
                viewModel.send(text)
            }
        }

        viewModel.initChat()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.ai_chat, menu)
        return true
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> viewModel.clearHistory()
        }
        return true
    }

}
