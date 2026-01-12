package com.example.myapplication.feature.highlight

import androidx.lifecycle.ViewModel
import com.example.myapplication.feature.present.DailyRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class HighlightViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HighlightUiState())
    val uiState: StateFlow<HighlightUiState> = _uiState.asStateFlow()

    private var lastRecordSignature: List<String> = emptyList()

    fun refreshIfNeeded(records: List<DailyRecord>) {
        val signature = records.map { record ->
            listOf(
                record.id,
                record.photoUri,
                record.memo,
                record.cesMetrics.identity,
                record.cesMetrics.connectivity,
                record.cesMetrics.perspective,
                record.date,
                record.isFeatured
            ).joinToString("|")
        }
        if (signature == lastRecordSignature) {
            return
        }
        lastRecordSignature = signature
        _uiState.value = HighlightUiState(
            sections = buildSections(records)
        )
    }

    private fun buildSections(records: List<DailyRecord>): List<HighlightSection> {
        val masterpiece = buildSection(
            type = HighlightType.MASTERPIECE,
            title = "나다운 기억",
            description = "I·C·P가 모두 높은 기억이에요.",
            candidates = records.filter { isMasterpiece(it) }
        )
        val hiddenDriver = buildSection(
            type = HighlightType.HIDDEN_DRIVER,
            title = "무의식의 나",
            description = "I가 낮고 P가 높은 기억이에요.",
            candidates = records.filter { isHiddenDriver(it) }
        )
        val emotionalAnchor = buildSection(
            type = HighlightType.EMOTIONAL_ANCHOR,
            title = "가장 큰 영향",
            description = "C가 다른 지표보다 크게 높은 기억이에요.",
            candidates = records.filter { isEmotionalAnchor(it) }
        )

        return listOf(masterpiece, hiddenDriver, emotionalAnchor)
            .filter { it.primary != null }
    }

    private fun buildSection(
        type: HighlightType,
        title: String,
        description: String,
        candidates: List<DailyRecord>
    ): HighlightSection {
        if (candidates.isEmpty()) {
            return HighlightSection(type, title, description, primary = null)
        }

        val scoredCandidates = candidates.map { record ->
            record to totalScore(record)
        }
        val maxScore = scoredCandidates.maxOf { it.second }
        val topCandidates = scoredCandidates.filter { it.second == maxScore }.map { it.first }
        val primaryRecord = topCandidates.maxByOrNull { record -> parseDate(record.date) } ?: topCandidates.first()
        val secondary = scoredCandidates
            .map { it.first }
            .filter { it.id != primaryRecord.id }
            .sortedWith(
                compareByDescending<DailyRecord> { totalScore(it) }
                    .thenByDescending { parseDate(it.date) }
            )
            .take(3)
            .map { toHighlightItem(type, title, description, it) }

        return HighlightSection(
            type = type,
            title = title,
            description = description,
            primary = toHighlightItem(type, title, description, primaryRecord),
            secondary = secondary
        )
    }

    private fun toHighlightItem(
        type: HighlightType,
        title: String,
        description: String,
        record: DailyRecord
    ): HighlightItem {
        return HighlightItem(
            type = type,
            title = title,
            description = description,
            photoUri = record.photoUri,
            memo = record.memo,
            identityScore = record.cesMetrics.identity,
            connectivityScore = record.cesMetrics.connectivity,
            perspectiveScore = record.cesMetrics.perspective,
            recordId = record.id
        )
    }

    private fun isMasterpiece(record: DailyRecord): Boolean {
        return record.cesMetrics.identity >= HIGH_THRESHOLD &&
            record.cesMetrics.connectivity >= HIGH_THRESHOLD &&
            record.cesMetrics.perspective >= HIGH_THRESHOLD
    }

    private fun isHiddenDriver(record: DailyRecord): Boolean {
        return record.cesMetrics.identity <= LOW_THRESHOLD &&
            record.cesMetrics.perspective >= HIGH_THRESHOLD
    }

    private fun isEmotionalAnchor(record: DailyRecord): Boolean {
        val maxOther = maxOf(record.cesMetrics.identity, record.cesMetrics.perspective)
        return record.cesMetrics.connectivity >= HIGH_THRESHOLD &&
            record.cesMetrics.connectivity - maxOther >= ANCHOR_DELTA
    }

    private fun totalScore(record: DailyRecord): Int {
        return record.cesMetrics.identity + record.cesMetrics.connectivity + record.cesMetrics.perspective
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
        private const val HIGH_THRESHOLD = 4
        private const val LOW_THRESHOLD = 2
        private const val ANCHOR_DELTA = 2
    }
}
