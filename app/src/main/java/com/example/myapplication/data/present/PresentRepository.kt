package com.example.myapplication.data.present

import android.content.Context
import android.util.Log
import com.example.myapplication.core.network.RetrofitClient.presentApi
// 🌟 Import Alias 적용
import com.example.myapplication.data.present.DailyRecord as DataRecord
import com.example.myapplication.feature.present.DailyRecord as FeatureRecord
import com.example.myapplication.feature.present.Practice
import com.example.myapplication.feature.present.PresentUiState
import com.example.myapplication.feature.present.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PresentRepository(
    private val presentApi: PresentApi,
    private val context: Context
) {
    private val storageFile = File(context.filesDir, "present_records.json")
    private val _savedRecords = mutableListOf<FeatureRecord>()

    init {
        loadRecordsFromFile()
    }

    suspend fun getPresentData(): PresentUiState {
        return try {
            val dto = presentApi.getPresentData()
            PresentUiState(
                userProfile = UserProfile(dto.userProfile.greeting, dto.userProfile.prompt),
                practices = dto.practices.map { Practice(it.id, it.title, it.subtitle, it.isAchieved) },
                practicesLeft = dto.practicesLeft,
                todayRecords = _savedRecords.toList()
            )
        } catch (e: Exception) {
            PresentUiState(
                userProfile = UserProfile("안녕하세요 사용자님!", "연결 실패"),
                todayRecords = _savedRecords.toList()
            )
        }
    }

    // 기록 추가 및 파일 백업
    fun addRecord(record: FeatureRecord) {
        _savedRecords.add(0, record)
        saveRecordsToFile()
    }

    // Worker를 위한 데이터 제공 함수
    fun getTodayRecordsForWorker(): List<FeatureRecord> {
        return _savedRecords.toList()
    }

    // 파일에 현재 리스트 저장
    private fun saveRecordsToFile() {
        try {
            val arr = JSONArray()
            _savedRecords.forEach { record ->
                val obj = JSONObject().apply {
                    put("id", record.id)
                    put("photoUri", record.photoUri)
                    put("memo", record.memo)
                    put("score", record.score)
                    put("date", record.date)
                    put("isFeatured", record.isFeatured)
                    put("meaning", record.meaning.name)

                    val ces = JSONObject().apply {
                        put("identity", record.cesMetrics.identity)
                        put("connectivity", record.cesMetrics.connectivity)
                        put("perspective", record.cesMetrics.perspective)
                        put("weightedScore", record.cesMetrics.weightedScore.toDouble())
                    }
                    put("cesMetrics", ces)
                }
                arr.put(obj)
            }
            storageFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e("PresentRepository", "파일 저장 실패: ${e.message}")
        }
    }

    // 파일에서 데이터 복구
    // PresentRepository.kt 내의 loadRecordsFromFile 함수 수정
    private fun loadRecordsFromFile() {
        if (!storageFile.exists()) return
        try {
            val json = storageFile.readText()
            if (json.isBlank()) return

            val arr = JSONArray(json)
            _savedRecords.clear()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val cesObj = obj.optJSONObject("cesMetrics") ?: JSONObject()

                // 날짜 형식 정규화 (2026.01.13 -> 2026-01-13)
                val rawDate = obj.optString("date", "")
                val normalizedDate = rawDate.replace(".", "-")

                val record = FeatureRecord(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    photoUri = obj.optString("photoUri", ""),
                    memo = obj.optString("memo", ""),
                    score = obj.optInt("score", 5),
                    cesMetrics = com.example.myapplication.feature.present.CesMetrics(
                        identity = cesObj.optInt("identity", 3),
                        connectivity = cesObj.optInt("connectivity", 3),
                        perspective = cesObj.optInt("perspective", 3),
                        weightedScore = cesObj.optDouble("weightedScore", 3.0).toFloat()
                    ),
                    meaning = com.example.myapplication.feature.present.Meaning.valueOf(obj.optString("meaning", "REMEMBER")),
                    date = normalizedDate, // 🌟 정규화된 날짜 적용
                    isFeatured = obj.optBoolean("isFeatured", false)
                )
                _savedRecords.add(record)
            }
            Log.d("PresentRepository", "성공적으로 ${_savedRecords.size}개의 기록을 복구했습니다.")
        } catch (e: Exception) {
            Log.e("PresentRepository", "복구 중 오류 발생: ${e.message}")
        }
    }

    // Worker 완료 후 호출
    fun clearAllRecords() {
        _savedRecords.clear()
        if (storageFile.exists()) storageFile.delete()
    }

    // ✅ 복구된 메소드: 실천 상태 업데이트
    suspend fun updatePracticeState(practiceId: String, isAchieved: Boolean) {
        try {
            presentApi.updateGoalState(practiceId, isAchieved)
        } catch (e: Exception) {
            Log.e("PresentRepository", "Error updating practice state: ${e.message}")
        }
    }

    // ✅ 복구된 메소드: 새로운 실천(목표) 추가
    suspend fun addPractice(text: String) {
        try {
            presentApi.addGoal(text)
        } catch (e: Exception) {
            Log.e("PresentRepository", "Error adding practice: ${e.message}")
        }
    }

    // ✅ 복구된 메소드: 목표를 기록으로 변환
    suspend fun convertGoalToRecord(goalTitle: String) {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val newRecord = FeatureRecord(
                id = UUID.randomUUID().toString(),
                photoUri = "",
                memo = "[미래 실천] $goalTitle",
                score = 5,
                cesMetrics = com.example.myapplication.feature.present.CesMetrics(3, 3, 3, 3f),
                meaning = com.example.myapplication.feature.present.Meaning.REMEMBER,
                date = today,
                isFeatured = false
            )
            addRecord(newRecord) // 메모리 추가 및 파일 저장 동시 실행
            Log.d("PresentRepository", "성공적으로 변환됨: ${newRecord.memo}")
        } catch (e: Exception) {
            Log.e("PresentRepository", "Error converting goal: ${e.message}")
        }
    }
}