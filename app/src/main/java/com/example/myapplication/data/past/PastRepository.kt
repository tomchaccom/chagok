package com.example.myapplication.data.past

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

import com.example.myapplication.data.present.DailyRecord
import com.example.myapplication.data.present.CesMetrics
import com.example.myapplication.data.present.Meaning

class PastRepository(private val context: Context) {
    private val storageFile = File(context.filesDir, "past_entries.json")
    private var idCounter = 1L
    // 즉시 빈 리스트로 초기화하여 loadFromStorage()가 entries를 참조하지 않도록 함
    private val entries: MutableList<DayEntry> = mutableListOf()

    init {
        val loaded = loadFromStorage()
        if (loaded.isNotEmpty()) {
            entries.addAll(loaded)
        } else {
            // 🌟 파일이 아예 없을 때만 더미를 만듭니다.
            // 기존에 데이터가 있었는데 파싱 에러로 안 불러와진 경우 덮어쓰면 안 됩니다.
            if (!storageFile.exists()) {
                entries.addAll(createDummyEntries())
                saveToStorage()
            }
        }
        val maxId = entries.maxOfOrNull { it.id } ?: 0L
        idCounter = maxId + 1
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

    // Add a single DailyRecord: group into existing DayEntry by date or create a new DayEntry
    // PastRepository.kt 내의 함수들을 아래와 같이 교체/수정하세요.

    /**
     * [해결] 동일한 날짜 레이블을 가진 엔트리를 찾아 사진을 합치거나 새로 추가합니다.
     */
    fun addOrUpdateDayEntry(newEntry: DayEntry) {
        // 1. 동일한 날짜 레이블을 가진 기존 엔트리의 인덱스를 찾습니다.
        val existingIndex = entries.indexOfFirst { it.dateLabel == newEntry.dateLabel }

        if (existingIndex != -1) {
            // 2. 이미 해당 날짜가 있다면, 기존 사진 리스트에 새 사진들을 합칩니다.
            val existingEntry = entries[existingIndex]
            // 중복된 ID를 가진 사진은 제외하고 합칩니다.
            val mergedPhotos = (existingEntry.photos + newEntry.photos).distinctBy { it.id }
            entries[existingIndex] = existingEntry.copy(photos = mergedPhotos)
        } else {
            // 3. 해당 날짜가 없으면 리스트의 맨 앞에 새로 추가합니다.
            val newId = idCounter++
            entries.add(0, newEntry.copy(id = newId))
        }

        // 4. 변경된 전체 리스트를 저장합니다.
        saveToStorage()
    }

    /**
     * 단일 레코드를 추가할 때도 위의 로직을 타도록 변경하여 일관성을 유지합니다.
     */
    fun addDailyRecord(record: DailyRecord) {
        try {
            val recordDate = if (record.date.isNotBlank()) record.date else {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            }
            val dateLabel = formatDateLabel(recordDate)

            // 위에서 만든 통합 로직용 DayEntry 객체 생성
            val tempEntry = DayEntry(id = 0L, dateLabel = dateLabel, photos = listOf(record))
            addOrUpdateDayEntry(tempEntry)
        } catch (_: Exception) {
        }
    }

    private fun formatDateLabel(dateStr: String): String {
        return try {
            val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val d = inFmt.parse(dateStr)
            if (d == null) return dateStr
            val outFmt = java.text.SimpleDateFormat("yyyy년 M월 d일", java.util.Locale.getDefault())
            outFmt.format(d)
        } catch (_: Exception) {
            dateStr
        }
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

    // 내부: 파일에서 로드(없으면 빈 리스트 반환)
    private fun loadFromStorage(): List<DayEntry> {
        try {
            // If file doesn't exist, return empty so init() can create and persist dummy data
            if (!storageFile.exists()) {
                return emptyList()
            }

            val text = storageFile.readText()
            if (text.isNotBlank()) {
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
                        // Support both legacy PhotoItem {imageUri,memo} and full DailyRecord
                        val photoUri = p.optString("photoUri", p.optString("imageUri", ""))
                        val memo = p.optString("memo", p.optString("memo", ""))
                        val pid = p.optString("id", java.util.UUID.randomUUID().toString())
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
                            CesMetrics(1,1,1,3f)
                        }
                        val meaningStr = p.optString("meaning", "REMEMBER")
                        val meaning = try { Meaning.valueOf(meaningStr) } catch (_: Exception) { Meaning.REMEMBER }
                        val date = p.optString("date", dateLabel)
                        val isFeatured = p.optBoolean("isFeatured", false)
                        photos.add(DailyRecord(pid, photoUri, memo, score, ces, meaning, date, isFeatured))
                    }
                    // create DayEntry without dayMemo
                    list.add(DayEntry(id = id, dateLabel = dateLabel, photos = photos))
                }
                if (list.isNotEmpty()) return list
            }
        } catch (_: Exception) {
            return emptyList()
        }

        // 파일이 없거나 파싱 실패 시 빈 리스트 반환 — init()에서 더미를 채워 저장합니다
        return emptyList()
    }

    // Ensure persistence
    fun ensurePersisted(): Boolean {
        return saveToStorage()
    }

    private fun saveToStorage(): Boolean {
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
                    cesObj.put("weightedScore", p.cesMetrics.weightedScore)
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

            // 1) Try openFileOutput + fsync
            try {
                context.openFileOutput(storageFile.name, Context.MODE_PRIVATE).use { fos ->
                    fos.write(json.toByteArray())
                    try {
                        fos.fd.sync()
                    } catch (_: Throwable) {
                        // ignore sync failures
                    }
                    fos.flush()
                }
                val written = File(context.filesDir, storageFile.name)
                if (written.exists() && written.length() > 0) {
                    return true
                }
            } catch (_: Exception) {
            }

            // 2) Try write to temp file then rename
            try {
                val tmp = File(context.filesDir, storageFile.name + ".tmp")
                tmp.outputStream().use { os ->
                    os.write(json.toByteArray())
                    try { os.fd.sync() } catch (_: Throwable) {}
                    os.flush()
                }
                val renamed = tmp.renameTo(storageFile)
                if (renamed) {
                    val written = File(context.filesDir, storageFile.name)
                    if (written.exists() && written.length() > 0) {
                        return true
                    }
                } else {
                    try {
                        tmp.inputStream().use { it.copyTo(storageFile.outputStream()) }
                        if (storageFile.exists() && storageFile.length() > 0) {
                            tmp.delete()
                            return true
                        }
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }

            // 3) Fallback: direct File.writeText
            try {
                storageFile.writeText(json)
                return true
            } catch (_: Exception) {
            }

            return false
        } catch (_: Exception) {
            return false
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
                photos = listOf(
                    DailyRecord(java.util.UUID.randomUUID().toString(), drawableUri("photo1"), "오늘 석양이 정말 멋졌어요.", 5, CesMetrics(1,1,1,3f), Meaning.REMEMBER, "2024-03-20", false),
                    DailyRecord(java.util.UUID.randomUUID().toString(), drawableUri("photo2"), "구름이 인상적었다.", 5, CesMetrics(1,1,1,3f), Meaning.REMEMBER, "2024-03-20", false),
                    DailyRecord(java.util.UUID.randomUUID().toString(), drawableUri("photo3"), "혼자 바라본 풍경.", 5, CesMetrics(1,1,1,3f), Meaning.REMEMBER, "2024-03-20", true),
                    DailyRecord(java.util.UUID.randomUUID().toString(), drawableUri("photo5"), "산책 중에 찍은 사진.", 5, CesMetrics(1,1,1,3f), Meaning.REMEMBER, "2024-03-20", false)
                )
            )
        )

        list.add(
            DayEntry(
                id = idCounterLocal++,
                dateLabel = "2024년 3월 19일",
                photos = listOf(
                    DailyRecord(java.util.UUID.randomUUID().toString(), drawableUri("photo4"), "카페 분위기 좋았다.", 5, CesMetrics(1,1,1,3f), Meaning.REMEMBER, "2024-03-19", false),
                    DailyRecord(java.util.UUID.randomUUID().toString(), drawableUri("photo5"), "산책 중에 찍은 사진.", 5, CesMetrics(1,1,1,3f), Meaning.REMEMBER, "2024-03-19", true)
                )
            )
        )

        return list
    }

}
