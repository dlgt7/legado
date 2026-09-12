package io.legado.app.help.config

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.legado.app.constant.PreferKey
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.ThemeStorePrefKeys
import io.legado.app.utils.GSON
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString
import splitties.init.appCtx

object ThemeManager {

    fun exportTheme(): String {
        val json = JsonObject()

        val prefs = appCtx.getSharedPreferences(
            ThemeStorePrefKeys.CONFIG_PREFS_KEY_DEFAULT, 0
        )
        val themeData = JsonObject().apply {
            for (key in prefs.all.keys) {
                val value = prefs.all[key]
                when (value) {
                    is Int -> addProperty(key, value)
                    is Boolean -> addProperty(key, value)
                    is String -> addProperty(key, value)
                    is Float -> addProperty(key, value)
                    is Long -> addProperty(key, value)
                }
            }
        }
        json.add("themeStore", themeData)

        val appPrefs = JsonObject().apply {
            for (key in themePrefKeys) {
                val value = appCtx.getPrefString(key)
                if (value != null) {
                    addProperty(key, value)
                }
            }
        }
        json.add("appPrefs", appPrefs)

        json.addProperty("version", 1)
        json.addProperty("exportTime", System.currentTimeMillis())

        return GSON.toJson(json)
    }

    fun importTheme(jsonStr: String): Boolean {
        return try {
            val json = JsonParser.parseString(jsonStr).asJsonObject

            json.getAsJsonObject("themeStore")?.let { themeData ->
                val editor = appCtx.getSharedPreferences(
                    ThemeStorePrefKeys.CONFIG_PREFS_KEY_DEFAULT, 0
                ).edit()
                for (entry in themeData.entrySet()) {
                    val value = entry.value
                    when {
                        value.isJsonPrimitive -> {
                            val prim = value.asJsonPrimitive
                            when {
                                prim.isBoolean -> editor.putBoolean(entry.key, prim.asBoolean)
                                prim.isNumber -> editor.putInt(entry.key, prim.asNumber.toInt())
                                prim.isString -> editor.putString(entry.key, prim.asString)
                                else -> null
                            }
                        }
                        else -> null
                    }?.let { }
                }
                editor.apply()
            }

            json.getAsJsonObject("appPrefs")?.let { appPrefs ->
                for (entry in appPrefs.entrySet()) {
                    if (entry.value.isJsonPrimitive) {
                        appCtx.putPrefString(entry.key, entry.value.asString)
                    }
                }
            }

            ThemeConfig.applyTheme(appCtx)
            true
        } catch (e: Exception) {
            false
        }
    }

    private val themePrefKeys = listOf(
        PreferKey.cPrimary, PreferKey.cAccent, PreferKey.cBackground, PreferKey.cBBackground,
        PreferKey.bgImage, PreferKey.bgImageBlurring,
        PreferKey.cNPrimary, PreferKey.cNAccent, PreferKey.cNBackground, PreferKey.cNBBackground,
        PreferKey.bgImageN, PreferKey.bgImageNBlurring,
        PreferKey.dynamicColors
    )

}