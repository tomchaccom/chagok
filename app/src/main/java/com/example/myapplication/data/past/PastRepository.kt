package com.example.myapplication.data.past

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.UUID

import com.example.myapplication.data.present.DailyRecord
import com.example.myapplication.data.present.CesMetrics
import com.example.myapplication.data.present.Meaning

class PastRepository(private val context: Context) {
    private val storageFile = File(context.filesDir, "past_entries.json")
    private var idCounter = 1L
    private val entries: MutableList<DayEntry> = mutableListOf()

    init {
        val loaded = loadFromStorage()
        if (loaded.isNotEmpty()) {
            entries.addAll(loaded)
        } else {
            // 파일이 아예 없을 때만 더미를 생성
            if (!storageFile.exists()) {
                // entries.addAll(createDummyEntries())
                saveToStorage()
            }
        }
        // ID 카운터를 현재 최대 ID + 1로 설정하여 중복 방지
        val maxId = entries.maxOfOrNull { it.id } ?: 0L
        idCounter = maxId + 1
    }

    /**
     * 로컬 리스트 반환
     */
    fun loadPastEntries(): List<DayEntry> {
        return entries.toList()
    }

    /**
     * 동일 날짜가 있으면 합치고, 없으면 새로 추가하는 통합 로직
     */
    fun addOrUpdateDayEntry(newEntry: DayEntry) {
        val existingIndex = entries.indexOfFirst { it.dateLabel == newEntry.dateLabel }

        if (existingIndex != -1) {
            val existingEntry = entries[existingIndex]
            // 사진 ID 중복 제거하며 합치기
            val mergedPhotos = (existingEntry.photos + newEntry.photos).distinctBy { it.id }
            entries[existingIndex] = existingEntry.copy(photos = mergedPhotos)
        } else {
            val newId = idCounter++
            entries.add(0, newEntry.copy(id = newId))
        }
        saveToStorage()
    }

    /**
     * 단일 레코드를 적절한 날짜 그룹에 추가
     */
    fun addDailyRecord(record: DailyRecord) {
        try {
            // 날짜가 비어있으면 오늘 날짜 사용
            val recordDate = if (record.date.isNotBlank()) record.date else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
            val dateLabel = formatDateLabel(recordDate)

            val tempEntry = DayEntry(id = 0L, dateLabel = dateLabel, photos = listOf(record))
            addOrUpdateDayEntry(tempEntry)
        } catch (e: Exception) {
            Log.e("PastRepository", "Error adding record: ${e.message}")
        }
    }

    private fun formatDateLabel(dateStr: String): String {
        return try {
            // 입력 형식이 마침표이든 하이픈이든 모두 처리
            val normalizedDate = dateStr.replace(".", "-")
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = inFmt.parse(normalizedDate) ?: return dateStr
            val outFmt = SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault())
            outFmt.format(d)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun updateDayEntry(updated: DayEntry): Boolean {
        val idx = entries.indexOfFirst { it.id == updated.id }
        return if (idx >= 0) {
            entries[idx] = updated
            saveToStorage()
            true
        } else false
    }

    fun deleteDayEntry(id: Long): Boolean {
        val removed = entries.removeIf { it.id == id }
        if (removed) saveToStorage()
        return removed
    }

    /**
     * 파일에서 데이터 로드 (JSON 키값 호환성 수정)
     */
    private fun loadFromStorage(): List<DayEntry> {
        try {
            if (!storageFile.exists()) return emptyList()

            val text = storageFile.readText()
            if (text.isBlank()) return emptyList()

            val arr = JSONArray(text)
            val list = mutableListOf<DayEntry>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optLong("id", 0L)
                val dateLabel = obj.optString("dateLabel", "")
                val photosJson = obj.optJSONArray("photos") ?: JSONArray()
                val photos = mutableListOf<DailyRecord>()

                for (j in 0 until photosJson.length()) {
                    val p = photosJson.getJSONObject(j)

                    // 🌟 해결: photoUri와 imageUri 모두 대응하도록 수정
                    val photoUri = p.optString("photoUri")
                        .ifBlank { p.optString("imageUri", "") }

                    val memo = p.optString("memo", "")
                    val pid = p.optString("id", UUID.randomUUID().toString())
                    val score = p.optInt("score", 5)

                    val cesObj = p.optJSONObject("cesMetrics")
                    val ces = if (cesObj != null) {
                        CesMetrics(
                            cesObj.optInt("identity", 1),
                            cesObj.optInt("connectivity", 1),
                            cesObj.optInt("perspective", 1),
                            cesObj.optDouble("weightedScore", 3.0).toFloat()
                        )
                    } else {
                        CesMetrics(1, 1, 1, 3f)
                    }

                    val meaningStr = p.optString("meaning", "REMEMBER")
                    val meaning = try { Meaning.valueOf(meaningStr) } catch (_: Exception) { Meaning.REMEMBER }
                    val date = p.optString("date", dateLabel)
                    val isFeatured = p.optBoolean("isFeatured", false)

                    photos.add(DailyRecord(pid, photoUri, memo, score, ces, meaning, date, isFeatured))
                }
                list.add(DayEntry(id = id, dateLabel = dateLabel, photos = photos))
            }
            return list
        } catch (e: Exception) {
            Log.e("PastRepository", "Load error: ${e.message}")
            return emptyList()
        }
    }

    /**
     * 데이터 안전하게 저장
     */
    fun saveToStorage(): Boolean {
        try {
            val arr = JSONArray()
            for (e in entries) {
                val obj = JSONObject()
                obj.put("id", e.id)
                obj.put("dateLabel", e.dateLabel)
                val photosArr = JSONArray()
                for (p in e.photos) {
                    val pObj = JSONObject()
                    pObj.put("id", p.id)
                    pObj.put("photoUri", p.photoUri)
                    pObj.put("memo", p.memo)
                    pObj.put("score", p.score)

                    val cesObj = JSONObject()
                    cesObj.put("identity", p.cesMetrics.identity)
                    cesObj.put("connectivity", p.cesMetrics.connectivity)
                    cesObj.put("perspective", p.cesMetrics.perspective)
                    cesObj.put("weightedScore", p.cesMetrics.weightedScore.toDouble())

                    pObj.put("cesMetrics", cesObj)
                    pObj.put("meaning", p.meaning.name)
                    pObj.put("date", p.date)
                    pObj.put("isFeatured", p.isFeatured)
                    photosArr.put(pObj)
                }
                obj.put("photos", photosArr)
                arr.put(obj)
            }

            val json = arr.toString()

            // 파일 쓰기 로직 (안정적인 rename 방식 권장)
            val tmpFile = File(context.filesDir, "${storageFile.name}.tmp")
            tmpFile.writeText(json)
            val success = tmpFile.renameTo(storageFile)

            if (!success) {
                storageFile.writeText(json) // rename 실패 시 직접 쓰기
            }

            Log.d("PastRepository", "Saved ${entries.size} entries to storage")
            return true
        } catch (e: Exception) {
            Log.e("PastRepository", "Save error: ${e.message}")
            return false
        }
    }

    private fun createDummyEntries(): List<DayEntry> {
        val pkg = context.packageName
        fun drawableUri(name: String): String = "android.resource://$pkg/drawable/$name"

        return listOf(
            DayEntry(
                id = 1L,
                dateLabel = "2024년 3월 20일",
                photos = listOf(
                    DailyRecord(UUID.randomUUID().toString(), drawableUri("photo1"), "오늘 석양이 정말 멋졌어요.", 5, CesMetrics(1,1,1,3f), Meaning.REMEMBER, "2024-03-20", false),
                    DailyRecord(UUID.randomUUID().toString(), drawableUri("photo2"), "구름이 인상적이었다.", 5, CesMetrics(1,1,1,3f), Meaning.REMEMBER, "2024-03-20", false)
                )
            ),
            DayEntry(
                id = 2L,
                dateLabel = "2024년 3월 19일",
                photos = listOf(
                    DailyRecord(UUID.randomUUID().toString(), drawableUri("photo4"), "카페 분위기 좋았다.", 5, CesMetrics(1,1,1,3f), Meaning.REMEMBER, "2024-03-19", false)
                )
            )
        )
    }
    /**
     * Worker나 외부 클래스에서 호출할 수 있도록 명시적으로 저장을 실행하는 메서드입니다.
     * 기존 ensurePersisted() 이름을 그대로 유지하여 오류를 해결합니다.
     */
    fun ensurePersisted(): Boolean {
        return saveToStorage()
    }
}