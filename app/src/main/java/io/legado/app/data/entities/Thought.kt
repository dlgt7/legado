package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "thoughts",
    indices = [(Index(value = ["bookName", "bookAuthor"], unique = false))]
)
data class Thought(
    @PrimaryKey
    var time: Long = System.currentTimeMillis(),
    var bookName: String = "",
    var bookAuthor: String = "",
    var chapterIndex: Int = 0,
    var chapterName: String = "",
    var selectedText: String = "",
    var content: String = "",
    var pageNum: Int = 0
) : Parcelable