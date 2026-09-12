package io.legado.app.ui.assistant

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.appDb
import io.legado.app.databinding.ActivityThoughtsBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThoughtsActivity : BaseActivity<ActivityThoughtsBinding>() {

    override val binding by viewBinding(ActivityThoughtsBinding::inflate)

    private val adapter by lazy { ThoughtsAdapter() }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rvThoughtList.layoutManager = LinearLayoutManager(this)
        binding.rvThoughtList.adapter = adapter
        loadData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.thoughts, menu)
        return true
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export_thoughts -> exportThoughts()
        }
        return true
    }

    private fun loadData() {
        lifecycleScope.launch {
            val thoughts = withContext(Dispatchers.IO) {
                appDb.thoughtDao.all
            }
            adapter.submitList(thoughts)
        }
    }

    private fun exportThoughts() {
        lifecycleScope.launch {
            val thoughts = withContext(Dispatchers.IO) {
                appDb.thoughtDao.all
            }
            val markdown = buildString {
                thoughts.forEach { t ->
                    appendLine("## ${t.bookName} - ${t.chapterName}")
                    if (t.selectedText.isNotBlank()) {
                        appendLine("> ${t.selectedText}")
                    }
                    appendLine()
                    appendLine(t.content)
                    appendLine()
                    appendLine("---")
                    appendLine()
                }
            }
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, markdown)
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "导出想法"))
        }
    }

}