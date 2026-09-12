package io.legado.app.model

import com.script.buildScriptBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.source.getShareScope
import org.mozilla.javascript.Scriptable
import kotlin.coroutines.CoroutineContext

object CallBackExecutor {

    fun execute(
        source: BookSource,
        event: CallBackEvent,
        coroutineContext: CoroutineContext? = null,
        data: Map<String, Any?> = emptyMap()
    ): Any? {
        val sharedScope = source.getShareScope(coroutineContext) ?: return null
        return executeWithScope(source, event, sharedScope, coroutineContext, data)
    }

    fun execute(
        source: BookSource,
        event: CallBackEvent,
        book: Book? = null,
        chapter: BookChapter? = null,
        coroutineContext: CoroutineContext? = null,
        extra: Map<String, Any?> = emptyMap()
    ): Any? {
        val data = mutableMapOf<String, Any?>()
        book?.let { data["book"] = it }
        chapter?.let { data["chapter"] = it }
        data.putAll(extra)
        return execute(source, event, coroutineContext, data)
    }

    private fun executeWithScope(
        source: BookSource,
        event: CallBackEvent,
        sharedScope: Scriptable,
        coroutineContext: CoroutineContext?,
        data: Map<String, Any?>
    ): Any? {
        val bindings = buildScriptBindings { bindings ->
            bindings["source"] = source
            data.forEach { (key, value) ->
                if (value != null) {
                    bindings[key] = value
                }
            }
        }
        bindings.prototype = sharedScope

        val typeStr = try {
            RhinoScriptEngine.eval(
                "typeof ${event.functionName}",
                bindings,
                coroutineContext
            ) as? String
        } catch (e: Exception) {
            AppLog.putDebug("callBackJs 检查失败: ${event.functionName}", e)
            return null
        }

        if (typeStr != "function") return null

        return try {
            RhinoScriptEngine.eval(
                "${event.functionName}()",
                bindings,
                coroutineContext
            )
        } catch (e: Exception) {
            AppLog.putDebug("callBackJs 执行失败: ${event.functionName}", e)
            null
        }
    }

}