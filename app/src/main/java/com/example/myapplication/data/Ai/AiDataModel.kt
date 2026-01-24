package com.example.myapplication.data.Ai

class AiDataModel {
}

// AiRequest.kt
data class AiAnalysisRequest(
    val prompt: String // JSON.stringify된 전체 기록 데이터
)

// AiResponse.kt (서버의 반환 형식에 맞춰 수정하세요)
data class AiAnalysisResponse(
    val content: String // AI가 분석한 결과 텍스트
)