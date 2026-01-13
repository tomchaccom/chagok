package com.example.myapplication.data.present

import android.util.Log
import com.example.myapplication.feature.present.DailyRecord
import com.example.myapplication.feature.present.Practice
import com.example.myapplication.feature.present.PresentUiState
import com.example.myapplication.feature.present.UserProfile
import com.example.myapplication.feature.present.CesMetrics // CesMetrics도 feature 패키지 것을 쓰도록 확인
import com.example.myapplication.feature.present.Meaning

class PresentRepository(private val presentApi: PresentApi) {

    suspend fun getPresentData(): PresentUiState {
        // 1. 가짜 데이터를 try-catch 밖이나 최상단에서 먼저 정의합니다.
        val mockRecords = listOf(
            DailyRecord(
                id = "test_1",
                photoUri = "",
                memo = "드디어 첫 기록이 성공했어요! 📸",
                score = 8,
                cesMetrics = CesMetrics(identity = 4, connectivity = 3, perspective = 1, weightedScore = 3.1f),
                date = "2026.01.13",
                meaning = Meaning.REMEMBER,
                isFeatured = false
            )
        )

        return try {
            val dto = presentApi.getPresentData() // 여기서 에러가 나면 바로 catch로 갑니다.

            PresentUiState(
                userProfile = UserProfile(dto.userProfile.greeting, dto.userProfile.prompt),
                practices = dto.practices.map { Practice(it.id, it.title, it.subtitle, it.isAchieved) },
                practicesLeft = dto.practicesLeft,
                todayRecords = mockRecords // 서버 성공 시 데이터 전달
            )
        } catch (e: Exception) {
            Log.e("PresentRepository", "API Error: ${e.message}")
            // 2. API가 실패해도 mockRecords는 보여주도록 수정합니다.
            PresentUiState(
                userProfile = UserProfile("오류 발생", "서버 연결 안 됨"),
                practices = emptyList(),
                practicesLeft = 0,
                todayRecords = mockRecords // 서버가 죽어도 테스트 데이터는 나오게 함!
            )
        }
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