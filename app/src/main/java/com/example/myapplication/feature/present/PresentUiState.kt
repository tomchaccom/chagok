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
    val photoUri: String, // 사진 URI 또는 파일 경로
    val memo: String = "", // 한 줄 메모 (빈 값 허용)
    val score: Int, // 1 ~ 10
    val meaning: Meaning = Meaning.REMEMBER, // 기억 or 잊기
    val date: String = "", // 날짜 (yyyy-MM-dd)
    val isFeatured: Boolean = false // 오늘의 대표 기억 여부
)