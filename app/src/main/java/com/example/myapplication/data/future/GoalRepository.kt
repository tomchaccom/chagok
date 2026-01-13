package com.example.myapplication.data.future

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID
// 🌟 Alias 적용: feature 패키지의 Goal과 혼동되지 않도록 data 패키지의 Goal을 명시합니다.
import com.example.myapplication.data.future.Goal as DataGoal

@RequiresApi(Build.VERSION_CODES.O)
object GoalRepository {
    // 🌟 이제 items는 반드시 id와 isAchieved가 포함된 DataGoal 리스트입니다.
    private val items = mutableListOf<DataGoal>()
    private var storageFile: File? = null

    fun getAll(): List<DataGoal> = items.toList()

    /**
     * 앱 시작 시 초기화 및 로드
     */
    fun initialize(context: Context) {
        if (storageFile != null) return
        storageFile = File(context.filesDir, "goals.json")

        val loadedFromStorage = loadFromStorage()
        if (loadedFromStorage.isNotEmpty()) {
            items.clear()
            items.addAll(loadedFromStorage)
            return
        }

        // 초기 파일 시도 (assets)
        try {
            val assetStream = context.assets.open("goals.json")
            val text = assetStream.bufferedReader().use { it.readText() }
            val parsed = parseJsonToList(text)
            if (parsed.isNotEmpty()) {
                items.clear()
                items.addAll(parsed)
                saveToStorage()
                return
            }
        } catch (_: Exception) {}

        // 기본 더미 데이터 로드
        items.clear()
        items.addAll(createDummyGoals())
        saveToStorage()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createDummyGoals(): List<DataGoal> = listOf(
        DataGoal(id = UUID.randomUUID().toString(), title = "스페인어 배우기", date = LocalDate.of(2026, 12, 31)),
        DataGoal(id = UUID.randomUUID().toString(), title = "마라톤 완주하기", date = LocalDate.of(2026, 5, 4)),
        DataGoal(id = UUID.randomUUID().toString(), title = "일본 여행하기", date = LocalDate.of(2026, 3, 2))
    )

    fun add(goal: DataGoal) {
        items.add(0, goal)
        try { if (storageFile != null) saveToStorage() } catch (_: Exception) {}
    }

    /**
     * 🌟 실천 버튼 클릭 시 뷰모델에서 호출하는 함수
     */
    fun updateGoalAchieved(goalId: String, isAchieved: Boolean) {
        val index = items.indexOfFirst { it.id == goalId }
        if (index != -1) {
            // copy를 사용하여 불변 객체의 상태를 업데이트합니다.
            items[index] = items[index].copy(isAchieved = isAchieved)
            saveToStorage()
        }
    }

    // --- Persistence Helpers ---
    private fun loadFromStorage(): List<DataGoal> {
        val file = storageFile ?: return emptyList()
        try {
            if (!file.exists()) return emptyList()
            val text = file.readText()
            return if (text.isBlank()) emptyList() else parseJsonToList(text)
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun parseJsonToList(text: String): List<DataGoal> {
        val arr = JSONArray(text)
        val list = mutableListOf<DataGoal>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)

            // 🌟 JSON에서 id와 isAchieved를 읽어옵니다. 없으면 기본값을 부여합니다.
            val id = obj.optString("id", UUID.randomUUID().toString())
            val title = obj.optString("title", "")
            val isAchieved = obj.optBoolean("isAchieved", false)

            val dateStr = if (obj.has("targetDate")) obj.optString("targetDate", "") else obj.optString("date", "")
            try {
                val date = if (dateStr.isNotBlank()) LocalDate.parse(dateStr) else LocalDate.now()
                list.add(DataGoal(id = id, title = title, date = date, isAchieved = isAchieved))
            } catch (_: Exception) {}
        }
        return list
    }

    private fun saveToStorage() {
        val file = storageFile ?: return
        try {
            val arr = JSONArray()
            for (g in items) {
                val obj = JSONObject()
                // 🌟 JSON 저장 시 모든 필드를 포함합니다.
                obj.put("id", g.id)
                obj.put("title", g.title)
                obj.put("date", g.date.toString())
                obj.put("isAchieved", g.isAchieved)
                arr.put(obj)
            }
            file.writeText(arr.toString())
        } catch (_: Exception) {}
    }


}