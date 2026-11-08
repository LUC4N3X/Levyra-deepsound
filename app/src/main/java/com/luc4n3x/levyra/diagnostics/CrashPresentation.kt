package com.luc4n3x.levyra.diagnostics

import org.json.JSONArray
import org.json.JSONObject

internal data class CrashPresentation(
    val language: String = "en",
    val font: String = "Outfit",
    val animations: Boolean = true,
    val light: Boolean = false,
    val colors: List<Int> = emptyList()
) {
    fun encode(): String = JSONObject().apply {
        put("language", language)
        put("font", font)
        put("animations", animations)
        put("light", light)
        put("colors", JSONArray(colors))
    }.toString()

    companion object {
        fun decode(value: String?): CrashPresentation? {
            if (value == null || value.length > 4_096) return null
            return try {
                val json = JSONObject(value)
                val language = json.optString("language", "en").take(16)
                val font = json.optString("font", "Outfit").take(40)
                val colors = json.optJSONArray("colors")
                CrashPresentation(language, font, json.optBoolean("animations", true), json.optBoolean("light"),
                    if (colors?.length() == 12) (0 until 12).map(colors::getInt) else emptyList())
            } catch (_: Exception) {
                null
            }
        }
    }
}
