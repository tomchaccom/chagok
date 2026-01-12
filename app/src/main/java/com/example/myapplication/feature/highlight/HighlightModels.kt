package com.example.myapplication.feature.highlight

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
    val score: Int
)

data class HighlightRankSection(
    val metric: HighlightMetric,
    val items: List<HighlightRankItem>
)

data class HighlightUiState(
    val sections: List<HighlightRankSection> = emptyList(),
    val showEmptyState: Boolean = true
)
