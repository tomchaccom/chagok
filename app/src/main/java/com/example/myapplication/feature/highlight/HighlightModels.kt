package com.example.myapplication.feature.highlight

import java.time.LocalDate

enum class HighlightMetric(val title: String) {
    IDENTITY("나다운 기억 TOP 5"),
    CONNECTIVITY("무의식의 나 TOP 5"),
    PERSPECTIVE("가장 큰 영향을 준 기억 TOP 5")
}

data class HighlightRankItem(
    val recordId: String,
    val rank: Int,
    val photoUri: String,
    val memo: String,
    val score: Int,
    val date : String
)

data class HighlightRankSection(
    val metric: HighlightMetric,
    val items: List<HighlightRankItem>,
    val averageScore: Double,

    // 🔽 추가
    val graphPoints: List<HighlightGraphPoint> = emptyList(),
    val canShowGraph: Boolean = false
)


data class HighlightUiState(
    val sections: List<HighlightRankSection> = emptyList(),
    val showEmptyState: Boolean = true
)

data class HighlightGraphPoint(
    val label: String, // 날짜 또는 "1위", "2위"
    val value: Int     // CES 점수
)



