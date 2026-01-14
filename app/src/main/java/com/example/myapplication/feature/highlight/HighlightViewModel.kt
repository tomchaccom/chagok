package com.example.myapplication.feature.highlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // 🌟 추가: 코루틴 사용을 위해 필수
import com.example.myapplication.data.Ai.RetrofitClient
import com.example.myapplication.data.present.DailyRecord as DataRecord
import com.google.gson.Gson // 🌟 추가: JSON 변환을 위해 필수
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch // 🌟 추가: viewModelScope.launch를 위해 필수
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class HighlightViewModel(
    private val repository: RecordRepository = FakeRecordRepository()
) : ViewModel() {

    // 1. AI 상태 관리
    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState

    // 2. UI 상태 관리
    private val _uiState = MutableStateFlow(HighlightUiState())
    val uiState: StateFlow<HighlightUiState> = _uiState.asStateFlow()

    private var lastSignature: List<String> = emptyList()

    /**
     * AI 분석 요청 함수
     * @param items 분석할 하이라이트 아이템 리스트
     */
    fun fetchAiAnalysis(items: List<HighlightRankItem>) {
        if (items.isEmpty()) {
            _aiState.value = AiUiState.Error("분석할 기록이 없어요.")
            return
        }

        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            try {
                // 1. 데이터를 JSON 문자열로 변환 (Gemini 프롬프트용)
                val jsonPrompt = Gson().toJson(items)

                // 2. Retrofit API 호출
                // baseUrl: http://10.0.2.2:8080/chat (에뮬레이터 로컬 서버)
                val response = RetrofitClient.instance.getAnalysis(jsonPrompt)

                _aiState.value = AiUiState.Success(response)
            } catch (e: Exception) {
                _aiState.value = AiUiState.Error(e.message ?: "서버와 연결할 수 없습니다.")
            }
        }
    }

    fun refreshIfNeeded() {
        val records = repository.getTodayRecords()
        val signature = records.map { record ->
            listOf(
                record.id,
                record.photoUri,
                record.memo,
                record.cesMetrics.identity,
                record.cesMetrics.connectivity,
                record.cesMetrics.perspective,
                record.date
            ).joinToString("|")
        }
        if (signature == lastSignature) return

        lastSignature = signature
        _uiState.value = buildUiState(records)
    }

    private fun buildUiState(records: List<DataRecord>): HighlightUiState {
        if (records.size < MIN_RECORDS_FOR_ANALYSIS) {
            return HighlightUiState(sections = emptyList(), showEmptyState = true)
        }

        val sections = listOf(
            buildSection(HighlightMetric.IDENTITY, records) { it.cesMetrics.identity },
            buildSection(HighlightMetric.CONNECTIVITY, records) { it.cesMetrics.connectivity },
            buildSection(HighlightMetric.PERSPECTIVE, records) { it.cesMetrics.perspective }
        )
        return HighlightUiState(
            sections = sections,
            showEmptyState = sections.all { it.items.isEmpty() }
        )
    }

    private fun buildSection(
        metric: HighlightMetric,
        records: List<DataRecord>,
        selector: (DataRecord) -> Int
    ): HighlightRankSection {
        val sorted = records.sortedWith(
            compareByDescending<DataRecord> { selector(it) }
                .thenByDescending { parseDate(it.date) }
        )

        val topRecords = sorted.take(MAX_RANK_COUNT)
        val items = topRecords.mapIndexed { index, record ->
            HighlightRankItem(
                recordId = record.id,
                rank = index + 1,
                photoUri = record.photoUri,
                memo = record.memo,
                score = selector(record),
                date = record.date
            )
        }

        val avg = if (items.isEmpty()) 0.0 else items.map { it.score }.average()
        val graphPoints = topRecords.mapIndexed { index, record ->
            HighlightGraphPoint(label = "${index + 1}", value = selector(record))
        }

        return HighlightRankSection(
            metric = metric,
            items = items,
            graphPoints = graphPoints,
            canShowGraph = graphPoints.size >= MIN_RECORDS_FOR_ANALYSIS,
            averageScore = avg
        )
    }

    private fun parseDate(date: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.parse(date)?.time ?: 0L
        } catch (e: ParseException) { 0L }
    }

    companion object {
        private const val MAX_RANK_COUNT = 5
        private const val MIN_RECORDS_FOR_ANALYSIS = 3
    }
}

// 🌟 ViewModel 클래스 밖으로 빼는 것이 관리하기 편합니다.
sealed class AiUiState {
    object Idle : AiUiState()
    object Loading : AiUiState()
    data class Success(val message: String) : AiUiState()
    data class Error(val error: String) : AiUiState()
}