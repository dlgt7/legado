package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.Thought
import kotlinx.coroutines.flow.Flow

@Dao
interface ThoughtDao {

    @get:Query("select * from thoughts order by time desc")
    val all: List<Thought>

    @get:Query("select * from thoughts order by time desc")
    val flowAll: Flow<List<Thought>>

    @Query("select * from thoughts where bookName = :bookName and bookAuthor = :bookAuthor order by chapterIndex, time")
    fun getByBook(bookName: String, bookAuthor: String): List<Thought>

    @Query("select * from thoughts where bookName like '%' || :key || '%' or content like '%' || :key || '%' or selectedText like '%' || :key || '%' order by time desc")
    fun search(key: String): List<Thought>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg thought: Thought)

    @Update
    fun update(thought: Thought)

    @Delete
    fun delete(vararg thought: Thought)

    @Query("delete from thoughts where time = :time")
    fun delete(time: Long)

    @Query("delete from thoughts")
    fun clear()

    @Query("delete from thoughts where bookName = :bookName")
    fun deleteByName(bookName: String)
}