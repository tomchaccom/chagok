package com.example.myapplication.data.present

import android.content.Context
import org.json.JSONObject
import java.io.File

object PracticeRepository {
    private const val FILE_NAME = "practices_state.json"
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun save(states: Map<String, Boolean?>) {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, FILE_NAME)
            val obj = JSONObject()
            for ((k, v) in states) {
                if (v == null) obj.put(k, JSONObject.NULL) else obj.put(k, v)
            }
            file.writeText(obj.toString())
        } catch (_: Exception) {
            // ignore
        }
    }

    fun load(): Map<String, Boolean?> {
        val ctx = appContext ?: return emptyMap()
        try {
            val file = File(ctx.filesDir, FILE_NAME)
            if (!file.exists()) return emptyMap()
            val content = file.readText()
            val obj = JSONObject(content)
            val keys = obj.keys()
            val map = mutableMapOf<String, Boolean?>()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = when {
                    obj.isNull(k) -> null
                    else -> obj.getBoolean(k)
                }
                map[k] = v
            }
            return map
        } catch (_: Exception) {
            return emptyMap()
        }
    }
}

