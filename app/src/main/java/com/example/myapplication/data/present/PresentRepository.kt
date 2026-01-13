package com.example.myapplication.data.present

import android.util.Log
import com.example.myapplication.core.network.RetrofitClient.presentApi
import com.example.myapplication.feature.present.DailyRecord
import com.example.myapplication.feature.present.Practice
import com.example.myapplication.feature.present.PresentUiState
import com.example.myapplication.feature.present.UserProfile


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
                userProfile = UserProfile("오류", "연결 실패"),
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
}