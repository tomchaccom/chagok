package com.example.myapplication.data.future

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.myapplication.feature.future.Goal
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
object GoalRepository {
    private val items = mutableListOf<Goal>()

    // storage file: 초기화 시 설정됨
    private var storageFile: File? = null

    fun getAll(): List<Goal> = items.toList()

    // 객체 생성 시에는 아무 데이터도 로드하지 않음. initialize(context)에서 로드.

    /**
     * 앱 시작 시 호출: 내부 storage 파일을 초기화하고(없으면 생성) 저장된 goals를 로드합니다.
     * 예: GoalRepository.initialize(context)
     */
    fun initialize(context: Context) {
        if (storageFile != null) return // 이미 초기화됨
        storageFile = File(context.filesDir, "goals.json")

        // 1) 내부 저장소에 파일이 있으면 로드
        val loadedFromStorage = loadFromStorage()
        if (loadedFromStorage.isNotEmpty()) {
            items.clear()
            items.addAll(loadedFromStorage)
            return
        }

        // 2) 내부 파일이 없거나 비어 있으면 assets에 있는 초기 파일을 시도(assets/past_entries.json은 과거용; goals.json이 assets에 있으면 사용)
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
        } catch (_: Exception) {
            // assets에 없거나 읽기 실패하면 무시
        }

        // 3) 어느 곳에도 없으면 더미 데이터를 로드하고 저장
        val dummy = createDummyGoals()
        items.clear()
        items.addAll(dummy)
        saveToStorage()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createDummyGoals(): List<Goal> = listOf(
        Goal("스페인어 배우기", LocalDate.of(2026, 12, 31)),
        Goal("마라톤 완주하기", LocalDate.of(2026, 5, 4)),
        Goal("일본 여행하기", LocalDate.of(2026, 3, 2)),
        /*Goal("토익 990점 달성", LocalDate.of(2026, 2, 2)),
        Goal("책 50권 읽기", LocalDate.of(2026, 8, 31)),
        Goal("저축 1천만 원", LocalDate.of(2026, 10, 26)),
        Goal("사이드 프로젝트 완성", LocalDate.of(2026, 1, 14)),
        Goal("운동 루틴 정착", LocalDate.of(2026, 1, 30)),
        Goal("사진 전시회 가기", LocalDate.of(2026, 7, 7))*/
    )

    fun add(goal: Goal) {
        // 가장 위에 추가되게 0번 인덱스에 삽입
        items.add(0, goal)
        // storageFile이 초기화되어 있으면 자동 저장
        try {
            if (storageFile != null) saveToStorage()
        } catch (_: Exception) {
            // 저장 실패는 무시(원하면 로깅 추가)
        }
    }

    // --- persistence helpers ---
    private fun loadFromStorage(): List<Goal> {
        val file = storageFile ?: return emptyList()
        try {
            if (!file.exists()) return emptyList()
            val text = file.readText()
            if (text.isBlank()) return emptyList()
            return parseJsonToList(text)
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun parseJsonToList(text: String): List<Goal> {
        val arr = JSONArray(text)
        val list = mutableListOf<Goal>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val title = obj.optString("title", "")
            // support both 'targetDate' (legacy) and 'date' keys
            val dateStr = if (obj.has("targetDate")) obj.optString("targetDate", "") else obj.optString("date", "")
            try {
                val date = if (dateStr.isNotBlank()) LocalDate.parse(dateStr) else LocalDate.now()
                list.add(Goal(title, date))
            } catch (_: Exception) {
                // parsing 실패면 건너뜀
            }
        }
        return list
    }

    private fun saveToStorage() {
        val file = storageFile ?: return
        try {
            val arr = JSONArray()
            for (g in items) {
                val obj = JSONObject()
                obj.put("title", g.title)
                // serialize using 'date' to match Goal.date
                obj.put("date", g.date.toString())
                arr.put(obj)
            }
            file.writeText(arr.toString())
        } catch (_: Exception) {
            // ignore write errors for now
        }
    }

}