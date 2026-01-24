package com.example.myapplication.feature.present

data class CesInput(
    val identity: Int = 3,
    val connectivity: Int = 3,
    val perspective: Int = 3
)

data class CesMetrics(
    val identity: Int,
    val connectivity: Int,
    val perspective: Int,
    val weightedScore: Float
)

interface CesOutlierAnalyzer {
    fun analyze(records: List<DailyRecord>): CesOutlierResult
}

data class CesOutlierResult(
    val identityOutliers: List<DailyRecord> = emptyList(),
    val connectivityOutliers: List<DailyRecord> = emptyList(),
    val perspectiveOutliers: List<DailyRecord> = emptyList()
)
