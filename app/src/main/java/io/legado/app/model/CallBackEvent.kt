package io.legado.app.model

enum class CallBackEvent(val functionName: String, val description: String) {
    ON_APP_START("onAppStart", "App 启动"),
    ON_BOOK_OPEN("onBookOpen", "打开书籍详情"),
    ON_BOOKSHELF_ADD("onBookshelfAdd", "加入书架"),
    ON_READ_START("onReadStart", "开始阅读"),
    ON_READ_END("onReadEnd", "结束阅读"),
    ON_PAGE_CHANGE("onPageChange", "翻页"),
    ON_CHAPTER_LOAD("onChapterLoad", "章节加载"),
    ON_CONTENT_LOAD("onContentLoad", "正文加载"),
    ON_CHAPTER_DOWNLOADED("onChapterDownloaded", "章节下载完成"),
    ON_SEARCH_START("onSearchStart", "搜索开始"),
    ON_SEARCH_END("onSearchEnd", "搜索结束"),
    ON_BOOK_UPDATE("onBookUpdate", "书籍更新"),
    ON_LOGIN_SUCCESS("onLoginSuccess", "登录成功"),
    ON_BOOKMARK_ADD("onBookmarkAdd", "添加书签"),
    ON_HIGHLIGHT_ADD("onHighlightAdd", "添加高亮"),
    ON_THEME_CHANGE("onThemeChange", "主题切换"),
    ON_SOURCE_EDIT("onSourceEdit", "书源编辑")
}