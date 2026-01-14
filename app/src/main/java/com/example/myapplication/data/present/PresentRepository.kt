package com.example.myapplication.data.present

import android.util.Log
import com.example.myapplication.core.network.RetrofitClient.presentApi
import com.example.myapplication.feature.present.DailyRecord
import com.example.myapplication.feature.present.Practice
import com.example.myapplication.feature.present.PresentUiState
import com.example.myapplication.feature.present.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


// PresentRepository.kt
class PresentRepository(private val presentApi: PresentApi) {

    // 메모리에 저장할 리스트 (임시 DB 역할)
    private val _savedRecords = mutableListOf<DailyRecord>()

    suspend fun getPresentData(): PresentUiState {
        return try {
            val dto = presentApi.getPresentData()
            PresentUiState(
                userProfile = UserProfile(dto.userProfile.greeting, dto.userProfile.prompt),
                practices = dto.practices.map { Practice(it.id, it.title, it.subtitle, it.isAchieved) },
                practicesLeft = dto.practicesLeft,
                todayRecords = _savedRecords.toList() // ✅ 가짜 데이터 대신 실제 저장된 리스트 반환
            )
        } catch (e: Exception) {
            PresentUiState(
                userProfile = UserProfile("안녕하세요 사용자님!", "연결 실패"),
                todayRecords = _savedRecords.toList() // ✅ 에러 시에도 저장된 데이터는 보여줌
            )
        }
    }

    // 새로운 기록을 리스트에 추가하는 함수
    fun addRecord(record: DailyRecord) {
        _savedRecords.add(0, record) // 최신순으로 맨 앞에 추가
    }


    suspend fun updatePracticeState(practiceId: String, isAchieved: Boolean) {
        try {
            presentApi.updateGoalState(practiceId, isAchieved)
        } catch (e: Exception) {
            Log.e("PresentRepository", "Error updating practice state: ${e.message}", e)
        }
    }

    suspend fun addPractice(text: String) {
        try {
            presentApi.addGoal(text)
        } catch (e: Exception) {
            Log.e("PresentRepository", "Error adding practice: ${e.message}", e)
        }
    }

    // PresentRepository.kt에 추가
    suspend fun convertGoalToRecord(goalTitle: String) {
        try {
            // 1. 오늘 날짜 및 기본 CES 지수 설정
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // 2. 새로운 DailyRecord 객체 생성
            val newRecord = DailyRecord(
                id = UUID.randomUUID().toString(),
                photoUri = "",
                memo = "[미래 실천] $goalTitle",
                score = 5,
                // 🌟 CesMetrics 필드 추가 (기본값 3, 3, 3, 3f 설정)
                cesMetrics = CesMetrics(
                    identity = 3,
                    connectivity = 3,
                    perspective = 3,
                    weightedScore = 3f
                ),
                // 🌟 Meaning 필드 추가 (기본값 REMEMBER)
                meaning = Meaning.REMEMBER,
                date = today,
                // 🌟 isFeatured 필드 추가
                isFeatured = false
            )

            // 3. 실제 DB나 API에 저장하는 로직 호출
            // presentApi.saveRecord(newRecord)

            Log.d("PresentRepository", "성공적으로 변환됨: ${newRecord.memo}")

        } catch (e: Exception) {
            Log.e("PresentRepository", "Error converting goal to record: ${e.message}")
        }
    }
}