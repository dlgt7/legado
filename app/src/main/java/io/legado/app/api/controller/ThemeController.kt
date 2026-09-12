package io.legado.app.api.controller

import io.legado.app.api.ReturnData
import io.legado.app.help.config.ThemeManager

object ThemeController {

    fun exportTheme(): ReturnData {
        val returnData = ReturnData()
        val json = ThemeManager.exportTheme()
        return returnData.setData(json)
    }

    fun importTheme(postData: String?): ReturnData {
        val returnData = ReturnData()
        val json = postData
            ?: return returnData.setErrorMsg("需要主题 JSON 数据")
        val success = ThemeManager.importTheme(json)
        return if (success) {
            returnData.setData("导入成功")
        } else {
            returnData.setErrorMsg("导入失败，JSON 格式错误")
        }
    }

}