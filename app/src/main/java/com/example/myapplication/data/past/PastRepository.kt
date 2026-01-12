package com.example.myapplication.data.past

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PastRepository(private val context: Context) {
    private val storageFile = File(context.filesDir, "past_entries.json")
    private var idCounter = 1L
    // 즉시 빈 리스트로 초기화하여 loadFromStorage()가 entries를 참조하지 않도록 함
    private val entries: MutableList<DayEntry> = mutableListOf()

    init {
        val loaded = loadFromStorage()
        entries.addAll(loaded)
        // ensure idCounter is greater than any existing id
        val maxId = entries.maxOfOrNull { it.id } ?: 0L
        idCounter = maxId + 1
        // 최초 설치시 storageFile이 없다면 entries를 파일로 저장해 둠
        if (!storageFile.exists()) saveToStorage()
    }

    // 기존 API: 로컬 메모리(또는 파일)에서 불러온 리스트 반환
    fun loadPastEntries(): List<DayEntry> {
        // return a copy to avoid external mutation
        return entries.toList()
    }

    // 새 DayEntry 추가 (id는 자동 부여) — 저장 후 id 반환
    fun addDayEntry(day: DayEntry): Long {
        val newId = idCounter++
        val newEntry = day.copy(id = newId)
        entries.add(0, newEntry) // 최신 항목을 앞에 추가
        saveToStorage()
        return newId
    }

    // 기존 항목 업데이트(같은 id 사용). 반환값: 성공 여부
    fun updateDayEntry(updated: DayEntry): Boolean {
        val idx = entries.indexOfFirst { it.id == updated.id }
        return if (idx >= 0) {
            entries[idx] = updated
            saveToStorage()
            true
        } else false
    }

    // 항목 삭제
    fun deleteDayEntry(id: Long): Boolean {
        val removed = entries.removeIf { it.id == id }
        if (removed) saveToStorage()
        return removed
    }

    // 내부: 파일에서 로드(없으면 더미 데이터 생성)
    private fun loadFromStorage(): List<DayEntry> {
        try {
            if (storageFile.exists()) {
                val text = storageFile.readText()
                if (text.isNotBlank()) {
                    val arr = JSONArray(text)
                    val list = mutableListOf<DayEntry>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optLong("id", 0L)
                        val dateLabel = obj.optString("dateLabel", "")
                        val dayMemo = obj.optString("dayMemo", "")
                        val photosJson = obj.optJSONArray("photos") ?: JSONArray()
                        val photos = mutableListOf<PhotoItem>()
                        for (j in 0 until photosJson.length()) {
                            val p = photosJson.getJSONObject(j)
                            photos.add(PhotoItem(p.optString("imageUri", ""), p.optString("memo", "")))
                        }
                        list.add(DayEntry(id = id, dateLabel = dateLabel, dayMemo = dayMemo, photos = photos))
                    }
                    if (list.isNotEmpty()) return list
                }
            }
        } catch (_: Exception) {
            // 실패 시 아래 더미 데이터를 반환하도록 fallthrough
        }

        // 파일이 없거나 파싱 실패 시 기존 더미 생성
        val dummy = createDummyEntries()
        // 반환만 하고 실제 entries 및 파일 저장은 init에서 처리합니다
        return dummy
    }

    private fun saveToStorage() {
        try {
            val arr = JSONArray()
            for (e in entries) {
                val obj = JSONObject()
                obj.put("id", e.id)
                obj.put("dateLabel", e.dateLabel)
                obj.put("dayMemo", e.dayMemo)
                val photosArr = JSONArray()
                for (p in e.photos) {
                    val pObj = JSONObject()
                    pObj.put("imageUri", p.imageUri)
                    pObj.put("memo", p.memo)
                    photosArr.put(pObj)
                }
                obj.put("photos", photosArr)
                arr.put(obj)
            }
            storageFile.writeText(arr.toString())
        } catch (_: Exception) {
            // ignore write errors for now
        }
    }

    // 기존 더미 데이터 생성 로직(초기 로드에서 사용)
    private fun createDummyEntries(): List<DayEntry> {
        val pkg = context.packageName
        fun drawableUri(name: String): String = "android.resource://$pkg/drawable/$name"

        val list = mutableListOf<DayEntry>()
        var idCounterLocal = 1L

        list.add(
            DayEntry(
                id = idCounterLocal++,
                dateLabel = "2024년 3월 20일",
                dayMemo = "해변에서 본 아름다운 석양.",
                photos = listOf(
                    PhotoItem(drawableUri("photo1"), "오늘 석양이 정말 멋졌어요."),
                    PhotoItem(drawableUri("photo2"), "구름이 인상적이었다."),
                    PhotoItem(drawableUri("photo3"), "혼자 바라본 풍경."),
                    PhotoItem(drawableUri("photo5"), "산책 중에 찍은 사진.")
                )
            )
        )

        list.add(
            DayEntry(
                id = idCounterLocal++,
                dateLabel = "2024년 3월 19일",
                dayMemo = "사라와 함께한 커피 시간.",
                photos = listOf(
                    PhotoItem(drawableUri("photo4"), "카페 분위기 좋았다."),
                    PhotoItem(drawableUri("photo5"), "산책 중에 찍은 사진.")
                )
            )
        )

        return list
    }

}
