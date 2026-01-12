package com.example.myapplication.feature.highlight

enum class HighlightType {
    MASTERPIECE,
    HIDDEN_DRIVER,
    EMOTIONAL_ANCHOR
}

data class HighlightItem(
    val type: HighlightType,
    val title: String,
    val description: String,
    val photoUri: String,
    val memo: String,
    val identityScore: Int,
    val connectivityScore: Int,
    val perspectiveScore: Int,
    val recordId: String
)

data class HighlightSection(
    val type: HighlightType,
    val title: String,
    val description: String,
    val primary: HighlightItem?,
    val secondary: List<HighlightItem> = emptyList()
)

data class HighlightUiState(
    val sections: List<HighlightSection> = emptyList()
)
