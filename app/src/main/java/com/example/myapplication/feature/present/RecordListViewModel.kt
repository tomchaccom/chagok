package com.example.myapplication.feature.present

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class RecordListViewModel : ViewModel() {

    val records: StateFlow<List<DailyRecord>> = CreateMomentViewModel.getSavedRecordsFlow()

    sealed interface UiEvent {
        data object ShowMainImageReplaceDialog : UiEvent
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var pendingMainRecordId: String? = null

    fun onMainImageToggleIntent(recordId: String, shouldSelect: Boolean) {
        val record = records.value.firstOrNull { it.id == recordId } ?: return
        if (!shouldSelect) {
            CreateMomentViewModel.updateRecord(recordId) { it.copy(isFeatured = false) }
            return
        }

        val existingMain = records.value.firstOrNull {
            it.date == record.date && it.isFeatured && it.id != recordId
        }
        if (existingMain != null) {
            // 이전에는 체크박스가 즉시 토글되며 상태가 바뀌어 UI 이벤트가 재바인딩 없이 누락됐습니다.
            // 이제는 '선택 의도'만 기록하고 이벤트로 다이얼로그 표시를 요청합니다.
            pendingMainRecordId = recordId
            _events.tryEmit(UiEvent.ShowMainImageReplaceDialog)
            return
        }

        CreateMomentViewModel.updateRecord(recordId) { it.copy(isFeatured = true) }
    }

    fun confirmReplaceMainImage() {
        val recordId = pendingMainRecordId ?: return
        val record = records.value.firstOrNull { it.id == recordId } ?: return
        CreateMomentViewModel.clearFeaturedForDate(record.date)
        CreateMomentViewModel.updateRecord(recordId) { it.copy(isFeatured = true) }
        pendingMainRecordId = null
    }

    fun cancelReplaceMainImage() {
        pendingMainRecordId = null
    }
}
