package com.example.myapplication.feature.highlight

import androidx.lifecycle.ViewModel
// 1. 패키지 충돌 해결을 위한 Alias 설정
import com.example.myapplication.data.present.DailyRecord as DataRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class HighlightViewModel(
    private val repository: RecordRepository = FakeRecordRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HighlightUiState())
    val uiState: StateFlow<HighlightUiState> = _uiState.asStateFlow()

    private var lastSignature: List<String> = emptyList()

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
        if (signature == lastSignature) {
            return
        }
        lastSignature = signature

        // 38번 줄: 이제 records와 buildUiState의 인자 타입이 DataRecord로 일치합니다.
        _uiState.value = buildUiState(records)
    }

    // 2. 인자 타입을 DataRecord로 변경
    private fun buildUiState(records: List<DataRecord>): HighlightUiState {
        if (records.size < MIN_RECORDS_FOR_ANALYSIS) {
            return HighlightUiState(
                sections = emptyList(),
                showEmptyState = true
            )
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

    // 3. 인자 타입을 DataRecord로 변경
    private fun buildSection(
        metric: HighlightMetric,
        records: List<DataRecord>,
        selector: (DataRecord) -> Int
    ): HighlightRankSection {

        // 1. 점수와 날짜순으로 정렬 (DataRecord 타입 기반)
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
                score = selector(record)
            )
        }

        val avg = if (items.isEmpty()) 0.0 else items.map { it.score }.average()

        val graphPoints = topRecords.mapIndexed { index, record ->
            HighlightGraphPoint(
                label = "${index + 1}",
                value = selector(record)
            )
        }

        val canShowGraph = graphPoints.size >= 3

        return HighlightRankSection(
            metric = metric,
            items = items,
            graphPoints = graphPoints,
            canShowGraph = canShowGraph,
            averageScore = avg
        )
    }

    private fun parseDate(date: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.parse(date)?.time ?: 0L
        } catch (e: ParseException) {
            0L
        }
    }

    companion object {
        private const val MAX_RANK_COUNT = 5
        private const val MIN_RECORDS_FOR_ANALYSIS = 3
    }
}