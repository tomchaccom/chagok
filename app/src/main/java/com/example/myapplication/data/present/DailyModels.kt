package com.example.myapplication.data.present

// 파일명: DailyModels.kt

/**
 * 하루의 기록을 담는 데이터 모델
 */
data class DailyRecord(
    val id: String,
    val photoUri: String,
    val memo: String,
    val score: Int,
    val cesMetrics: CesMetrics,
    val meaning: Meaning,
    val date: String,
    val isFeatured: Boolean
)


/**
 * CES 지수 (정체성, 연결성, 관점 + 가중치 점수)
 */
data class CesMetrics(
    val identity: Int,
    val connectivity: Int,
    val perspective: Int,
    val weightedScore: Float
)

/**
 * 의미 부여 (기억하기 / 잊기)
 */
enum class Meaning {
    REMEMBER, FORGET
}

/**
 * 입력용 임시 데이터 (ViewModel용)
 */
data class CesInput(
    val identity: Int = 1,
    val connectivity: Int = 1,
    val perspective: Int = 1
)