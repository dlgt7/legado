package io.legado.app.ui.assistant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.Thought
import io.legado.app.databinding.ItemThoughtBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThoughtsAdapter : ListAdapter<Thought, ThoughtsAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    companion object DiffCallback : DiffUtil.ItemCallback<Thought>() {
        override fun areItemsTheSame(oldItem: Thought, newItem: Thought) =
            oldItem.time == newItem.time

        override fun areContentsTheSame(oldItem: Thought, newItem: Thought) =
            oldItem == newItem
    }

    class ViewHolder(val binding: ItemThoughtBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemThoughtBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvBookName.text = item.bookName
            tvChapterName.text = item.chapterName
            tvSelectedText.text = item.selectedText
            tvContent.text = item.content
            tvTime.text = dateFormat.format(Date(item.time))
        }
    }

}
