package io.legado.app.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.Bookmark
import io.legado.app.data.entities.Thought
import io.legado.app.help.book.BookHelp
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.GSON
import kotlinx.coroutines.flow.first

object McpToolRegistry {

    private val tools = mutableMapOf<String, Pair<McpTool, McpToolHandler>>()

    fun register(tool: McpTool, handler: McpToolHandler) {
        tools[tool.name] = tool to handler
    }

    fun listTools(): List<McpTool> = tools.values.map { it.first }

    fun getTool(name: String): McpTool? = tools[name]?.first

    fun getHandler(name: String): McpToolHandler? = tools[name]?.second

    fun toolNames(): Set<String> = tools.keys

    fun init() {
        registerSearchBooks()
        registerGetBookshelf()
        registerGetBookSources()
        registerGetBookInfo()
        registerGetChapterList()
        registerGetReadingProgress()
        registerSearchWebBooks()
        registerAddToBookshelf()
        registerRemoveFromBookshelf()
        registerEnableDisableSource()
        registerGetChapterContent()
        registerGetBookmarks()
        registerAddBookmark()
        registerGetThoughts()
        registerAddThought()
    }

    private fun objSchema(properties: JsonObject, required: List<String> = emptyList()): JsonObject {
        return JsonObject().apply {
            addProperty("type", "object")
            add("properties", properties)
            if (required.isNotEmpty()) {
                add("required", JsonArray().apply { required.forEach { add(it) } })
            }
        }
    }

    private fun stringProp(desc: String) = JsonObject().apply {
        addProperty("type", "string")
        addProperty("description", desc)
    }

    private fun intProp(desc: String) = JsonObject().apply {
        addProperty("type", "integer")
        addProperty("description", desc)
    }

    private fun boolProp(desc: String) = JsonObject().apply {
        addProperty("type", "boolean")
        addProperty("description", desc)
    }

    private fun registerSearchBooks() {
        val tool = McpTool(
            "search_books",
            "在书架中搜索书籍（按书名或作者）",
            objSchema(
                JsonObject().apply {
                    add("query", stringProp("搜索关键词"))
                },
                listOf("query")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val query = params["query"]?.toString()
                    ?: return McpToolResult.error("缺少 query 参数")
                return try {
                    val books = appDb.bookDao.all.filter {
                        it.name.contains(query, true) || it.author.contains(query, true)
                    }
                    val result = books.map { mapOf(
                        "name" to it.name,
                        "author" to it.author,
                        "bookUrl" to it.bookUrl
                    )}
                    McpToolResult.ok(GSON.toJson(result))
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "搜索失败")
                }
            }
        })
    }

    private fun registerGetBookshelf() {
        val tool = McpTool(
            "get_bookshelf",
            "获取书架上的所有书籍",
            objSchema(JsonObject())
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                return try {
                    val books = appDb.bookDao.all
                    val result = books.map { mapOf(
                        "name" to it.name,
                        "author" to it.author,
                        "bookUrl" to it.bookUrl,
                        "type" to it.type,
                        "chapterIndex" to it.durChapterIndex,
                        "totalChapterNum" to it.totalChapterNum
                    )}
                    McpToolResult.ok(GSON.toJson(result))
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "获取书架失败")
                }
            }
        })
    }

    private fun registerGetBookSources() {
        val tool = McpTool(
            "get_book_sources",
            "获取书源列表",
            objSchema(JsonObject().apply {
                add("enabled", boolProp("是否只返回启用的书源"))
            })
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val enabledOnly = params["enabled"] as? Boolean ?: false
                return try {
                    val sources = appDb.bookSourceDao.search("")
                    val filtered = if (enabledOnly) sources.filter { it.enabled } else sources
                    val result = filtered.map { mapOf(
                        "name" to it.bookSourceName,
                        "url" to it.bookSourceUrl,
                        "group" to it.bookSourceGroup,
                        "enabled" to it.enabled
                    )}
                    McpToolResult.ok(GSON.toJson(result))
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "获取书源失败")
                }
            }
        })
    }

    private fun registerGetBookInfo() {
        val tool = McpTool(
            "get_book_info",
            "获取书籍详细信息",
            objSchema(
                JsonObject().apply {
                    add("bookUrl", stringProp("书籍URL"))
                },
                listOf("bookUrl")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val bookUrl = params["bookUrl"]?.toString()
                    ?: return McpToolResult.error("缺少 bookUrl 参数")
                return try {
                    val book = appDb.bookDao.getBook(bookUrl)
                    if (book != null) {
                        McpToolResult.ok(GSON.toJson(book))
                    } else {
                        McpToolResult.error("书籍不存在")
                    }
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "获取书籍信息失败")
                }
            }
        })
    }

    private fun registerGetChapterList() {
        val tool = McpTool(
            "get_chapter_list",
            "获取书籍目录",
            objSchema(
                JsonObject().apply {
                    add("bookUrl", stringProp("书籍URL"))
                },
                listOf("bookUrl")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val bookUrl = params["bookUrl"]?.toString()
                    ?: return McpToolResult.error("缺少 bookUrl 参数")
                return try {
                    val chapters = appDb.bookChapterDao.getChapterList(bookUrl)
                    val result = chapters.map { mapOf(
                        "index" to it.index,
                        "title" to it.title,
                        "url" to it.url
                    )}
                    McpToolResult.ok(GSON.toJson(result))
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "获取目录失败")
                }
            }
        })
    }

    private fun registerGetReadingProgress() {
        val tool = McpTool(
            "get_reading_progress",
            "获取所有书籍的阅读进度",
            objSchema(JsonObject())
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                return try {
                    val books = appDb.bookDao.all
                    val reading = books.filter { it.durChapterIndex > 0 || it.durChapterPos > 0 }
                        .map { mapOf(
                            "name" to it.name,
                            "author" to it.author,
                            "chapterIndex" to it.durChapterIndex,
                            "chapterPos" to it.durChapterPos,
                            "lastReadTime" to it.durChapterTime
                        )}
                    McpToolResult.ok(GSON.toJson(reading))
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "获取阅读进度失败")
                }
            }
        })
    }

    private fun registerSearchWebBooks() {
        val tool = McpTool(
            "search_web_books",
            "通过启用的书源在网络中搜索书籍",
            objSchema(
                JsonObject().apply {
                    add("query", stringProp("搜索关键词"))
                    add("page", intProp("页码，从1开始"))
                },
                listOf("query")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val query = params["query"]?.toString()
                    ?: return McpToolResult.error("缺少 query 参数")
                val page = (params["page"] as? Number)?.toInt() ?: 1
                return try {
                    val sources = appDb.bookSourceDao.search("").filter { it.enabled }
                    val results = mutableListOf<Map<String, Any?>>()
                    for (source in sources.take(10)) {
                        try {
                            val books = WebBook.searchBookAwait(source, query, page)
                            books.forEach { sb ->
                                results.add(mapOf(
                                    "name" to sb.name,
                                    "author" to sb.author,
                                    "bookUrl" to sb.bookUrl,
                                    "sourceName" to sb.originName,
                                    "intro" to sb.intro
                                ))
                            }
                        } catch (_: Exception) {}
                    }
                    McpToolResult.ok(GSON.toJson(results))
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "网络搜索失败")
                }
            }
        })
    }

    private fun registerAddToBookshelf() {
        val tool = McpTool(
            "add_to_bookshelf",
            "将书籍添加到书架",
            objSchema(
                JsonObject().apply {
                    add("bookUrl", stringProp("书籍URL"))
                    add("name", stringProp("书名"))
                    add("author", stringProp("作者"))
                },
                listOf("bookUrl", "name", "author")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val bookUrl = params["bookUrl"]?.toString()
                    ?: return McpToolResult.error("缺少 bookUrl 参数")
                val name = params["name"]?.toString() ?: ""
                val author = params["author"]?.toString() ?: ""
                return try {
                    if (appDb.bookDao.has(bookUrl)) {
                        return McpToolResult.ok("已在书架中")
                    }
                    val book = Book(bookUrl = bookUrl, name = name, author = author)
                    appDb.bookDao.insert(book)
                    McpToolResult.ok("添加成功")
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "添加失败")
                }
            }
        })
    }

    private fun registerRemoveFromBookshelf() {
        val tool = McpTool(
            "remove_from_bookshelf",
            "从书架删除书籍",
            objSchema(
                JsonObject().apply {
                    add("bookUrl", stringProp("书籍URL"))
                },
                listOf("bookUrl")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val bookUrl = params["bookUrl"]?.toString()
                    ?: return McpToolResult.error("缺少 bookUrl 参数")
                return try {
                    val book = appDb.bookDao.getBook(bookUrl)
                        ?: return McpToolResult.error("书籍不存在")
                    appDb.bookDao.delete(book)
                    McpToolResult.ok("删除成功")
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "删除失败")
                }
            }
        })
    }

    private fun registerEnableDisableSource() {
        val tool = McpTool(
            "enable_disable_source",
            "启用或禁用书源",
            objSchema(
                JsonObject().apply {
                    add("sourceUrl", stringProp("书源URL"))
                    add("enabled", boolProp("true启用，false禁用"))
                },
                listOf("sourceUrl", "enabled")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val sourceUrl = params["sourceUrl"]?.toString()
                    ?: return McpToolResult.error("缺少 sourceUrl 参数")
                val enabled = params["enabled"] as? Boolean
                    ?: return McpToolResult.error("缺少 enabled 参数")
                return try {
                    appDb.bookSourceDao.enable(sourceUrl, enabled)
                    McpToolResult.ok(if (enabled) "已启用" else "已禁用")
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "操作失败")
                }
            }
        })
    }

    private fun registerGetChapterContent() {
        val tool = McpTool(
            "get_chapter_content",
            "获取章节正文内容",
            objSchema(
                JsonObject().apply {
                    add("bookUrl", stringProp("书籍URL"))
                    add("chapterIndex", intProp("章节索引"))
                },
                listOf("bookUrl", "chapterIndex")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val bookUrl = params["bookUrl"]?.toString()
                    ?: return McpToolResult.error("缺少 bookUrl 参数")
                val chapterIndex = (params["chapterIndex"] as? Number)?.toInt()
                    ?: return McpToolResult.error("缺少 chapterIndex 参数")
                return try {
                    val book = appDb.bookDao.getBook(bookUrl)
                        ?: return McpToolResult.error("书籍不存在")
                    val chapter = appDb.bookChapterDao.getChapter(bookUrl, chapterIndex)
                        ?: return McpToolResult.error("章节不存在")
                    val content = BookHelp.getContent(book, chapter)
                    if (content != null) {
                        McpToolResult.ok(content)
                    } else {
                        McpToolResult.error("内容未缓存，请先下载")
                    }
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "获取内容失败")
                }
            }
        })
    }

    private fun registerGetBookmarks() {
        val tool = McpTool(
            "get_bookmarks",
            "获取书籍的书签列表",
            objSchema(
                JsonObject().apply {
                    add("bookName", stringProp("书名"))
                    add("bookAuthor", stringProp("作者"))
                },
                listOf("bookName")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val bookName = params["bookName"]?.toString()
                    ?: return McpToolResult.error("缺少 bookName 参数")
                val bookAuthor = params["bookAuthor"]?.toString() ?: ""
                return try {
                    val bookmarks = appDb.bookmarkDao.getByBook(bookName, bookAuthor)
                    val result = bookmarks.map { mapOf(
                        "chapterIndex" to it.chapterIndex,
                        "chapterPos" to it.chapterPos,
                        "chapterName" to it.chapterName,
                        "content" to it.content
                    )}
                    McpToolResult.ok(GSON.toJson(result))
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "获取书签失败")
                }
            }
        })
    }

    private fun registerAddBookmark() {
        val tool = McpTool(
            "add_bookmark",
            "添加书签",
            objSchema(
                JsonObject().apply {
                    add("bookName", stringProp("书名"))
                    add("bookAuthor", stringProp("作者"))
                    add("chapterIndex", intProp("章节索引"))
                    add("chapterPos", intProp("章节位置"))
                    add("chapterName", stringProp("章节名称"))
                    add("content", stringProp("书签内容"))
                },
                listOf("bookName", "chapterIndex")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val bookName = params["bookName"]?.toString()
                    ?: return McpToolResult.error("缺少 bookName 参数")
                val bookAuthor = params["bookAuthor"]?.toString() ?: ""
                val chapterIndex = (params["chapterIndex"] as? Number)?.toInt() ?: 0
                val chapterPos = (params["chapterPos"] as? Number)?.toInt() ?: 0
                val chapterName = params["chapterName"]?.toString() ?: ""
                val content = params["content"]?.toString() ?: ""
                return try {
                    val bookmark = Bookmark(
                        bookName = bookName,
                        bookAuthor = bookAuthor,
                        chapterIndex = chapterIndex,
                        chapterPos = chapterPos,
                        chapterName = chapterName,
                        content = content
                    )
                    appDb.bookmarkDao.insert(bookmark)
                    McpToolResult.ok("添加成功")
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "添加书签失败")
                }
            }
        })
    }

    private fun registerGetThoughts() {
        val tool = McpTool(
            "get_thoughts",
            "获取想法/笔记列表",
            objSchema(
                JsonObject().apply {
                    add("bookName", stringProp("书名（可选，不填则返回全部）"))
                    add("key", stringProp("搜索关键词（可选）"))
                }
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val bookName = params["bookName"]?.toString()
                val key = params["key"]?.toString()
                return try {
                    val thoughts = when {
                        !bookName.isNullOrBlank() -> appDb.thoughtDao.getByBook(bookName, "")
                        !key.isNullOrBlank() -> appDb.thoughtDao.search(key)
                        else -> appDb.thoughtDao.all
                    }
                    val result = thoughts.map { mapOf(
                        "time" to it.time,
                        "bookName" to it.bookName,
                        "chapterName" to it.chapterName,
                        "selectedText" to it.selectedText,
                        "content" to it.content
                    )}
                    McpToolResult.ok(GSON.toJson(result))
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "获取想法失败")
                }
            }
        })
    }

    private fun registerAddThought() {
        val tool = McpTool(
            "add_thought",
            "添加想法/笔记",
            objSchema(
                JsonObject().apply {
                    add("bookName", stringProp("书名"))
                    add("bookAuthor", stringProp("作者"))
                    add("chapterName", stringProp("章节名称"))
                    add("selectedText", stringProp("选中的原文"))
                    add("content", stringProp("想法内容"))
                },
                listOf("bookName", "content")
            )
        )
        register(tool, object : McpToolHandler {
            override suspend fun execute(params: Map<String, Any?>): McpToolResult {
                val bookName = params["bookName"]?.toString()
                    ?: return McpToolResult.error("缺少 bookName 参数")
                val content = params["content"]?.toString()
                    ?: return McpToolResult.error("缺少 content 参数")
                return try {
                    val thought = Thought(
                        bookName = bookName,
                        bookAuthor = params["bookAuthor"]?.toString() ?: "",
                        chapterName = params["chapterName"]?.toString() ?: "",
                        selectedText = params["selectedText"]?.toString() ?: "",
                        content = content
                    )
                    appDb.thoughtDao.insert(thought)
                    McpToolResult.ok("添加成功")
                } catch (e: Exception) {
                    McpToolResult.error(e.message ?: "添加想法失败")
                }
            }
        })
    }

}
