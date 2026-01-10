package com.example.myapplication.feature.present

data class PresentUiState(
    val userProfile: UserProfile = UserProfile(),
    val practices: List<Practice> = emptyList(),
    val practicesLeft: Int = 0,
    val dailyRecords: List<DailyRecord> = emptyList()
)

data class UserProfile(
    val greeting: String = "안녕하세요, 사용자님",
    val prompt: String = "오늘을 기록할 준비가 되셨나요?"
)

data class Practice(
    val id: String,
    val title: String,
    val subtitle: String = "오늘",
    val isAchieved: Boolean? = null // null: untouched, true: achieved, false: unachieved
)

data class DailyRecord(
    val id: String,
    val photoUrl: String, // Or use Uri
    val memo: String,
    val score: Int
)