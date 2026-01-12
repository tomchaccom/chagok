package com.example.myapplication.feature.highlight

enum class HighlightMetric(
    val letter: String,
    val title: String,
    val subtitle: String
) {
    IDENTITY(
        letter = "I",
        title = "나다운 기억 TOP 5",
        subtitle = "나를 정의하는 기억의 깊이"
    ),
    CONNECTIVITY(
        letter = "C",
        title = "무의식의 나 TOP 5",
        subtitle = "연결되는 생각의 밀도"
    ),
    PERSPECTIVE(
        letter = "P",
        title = "가장 큰 영향을 준 기억 TOP 5",
        subtitle = "관점의 변화를 만든 순간"
    )
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
