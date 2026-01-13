package com.example.myapplication.feature.highlight

import androidx.lifecycle.ViewModel
import com.example.myapplication.feature.present.DailyRecord
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
        _uiState.value = buildUiState(records)
    }

    private fun buildUiState(records: List<DailyRecord>): HighlightUiState {
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

    private fun buildSection(
        metric: HighlightMetric,
        records: List<DailyRecord>,
        selector: (DailyRecord) -> Int
    ): HighlightRankSection {

        // 1. 점수와 날짜순으로 정렬
        val sorted = records.sortedWith(
            compareByDescending<DailyRecord> { selector(it) }
                .thenByDescending { parseDate(it.date) }
        )

        val topRecords = sorted.take(MAX_RANK_COUNT)

        // 2. items를 먼저 생성해야 avg 계산 시 사용할 수 있습니다.
        val items = topRecords.mapIndexed { index, record ->
            HighlightRankItem(
                recordId = record.id,
                rank = index + 1,
                photoUri = record.photoUri,
                memo = record.memo,
                score = selector(record)
            )
        }

        // 3. 이제 items.map { it.score }.average()가 가능합니다.
        val avg = if (items.isEmpty()) 0.0 else items.map { it.score }.average()

        val graphPoints = topRecords.mapIndexed { index, record ->
            HighlightGraphPoint(
                label = "${index + 1}",
                value = selector(record)
            )
        }

        val canShowGraph = graphPoints.size >= 3

        // 4. 생성자에 인자 이름(named arguments)을 명시해서 실수 방지
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
