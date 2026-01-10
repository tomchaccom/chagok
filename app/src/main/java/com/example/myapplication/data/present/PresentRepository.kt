package com.example.myapplication.data.present

import android.util.Log
import com.example.myapplication.feature.present.Practice
import com.example.myapplication.feature.present.PresentUiState
import com.example.myapplication.feature.present.UserProfile

class PresentRepository(private val presentApi: PresentApi) {

    suspend fun getPresentData(): PresentUiState {
        return try {
            val dto = presentApi.getPresentData()
            PresentUiState(
                userProfile = UserProfile(dto.userProfile.greeting, dto.userProfile.prompt),
                practices = dto.practices.map { Practice(it.id, it.title, it.subtitle, it.isAchieved) },
                practicesLeft = dto.practicesLeft,
                dailyRecords = emptyList() // Placeholder
            )
        } catch (e: Exception) {
            Log.e("PresentRepository", "Error fetching present data: ${e.message}", e)
            // Return default/empty state on error
            PresentUiState(
                userProfile = UserProfile("오류 발생", "데이터를 불러올 수 없습니다"),
                practices = emptyList(),
                practicesLeft = 0,
                dailyRecords = emptyList()
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