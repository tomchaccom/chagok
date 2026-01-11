package com.example.myapplication.feature.present

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * CreateMomentViewModel
 *
 * 순간 기록 화면(CreateMomentFragment)의 상태와 로직을 관리합니다.
 * - 사진 선택/촬영 결과 처리
 * - 메모, 점수, 기억/잊기 상태 관리
 * - 저장 로직 실행
 *
 * 주의: 현재는 in-memory 저장만 지원합니다 (Room DB는 추후 추가)
 */
class CreateMomentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMomentUiState())
    val uiState: StateFlow<CreateMomentUiState> = _uiState.asStateFlow()

    // 저장된 기록을 임시로 메모리에 보관 (싱글톤으로 변경 가능)
    companion object {
        private val savedRecordsFlow = MutableStateFlow<List<DailyRecord>>(emptyList())

        fun getSavedRecords(): List<DailyRecord> = savedRecordsFlow.value
        fun getSavedRecordsFlow(): StateFlow<List<DailyRecord>> = savedRecordsFlow.asStateFlow()
        fun addRecord(record: DailyRecord) {
            savedRecordsFlow.update { it + record }
        }
        fun clearRecords() {
            savedRecordsFlow.value = emptyList()
        }
    }

    sealed interface UiEvent {
        data object ShowFeaturedReplaceDialog : UiEvent
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var pendingFeaturedSelection = false

    /**
     * 사진 URI 설정 (갤러리 또는 카메라에서 선택한 이미지)
     */
    fun setSelectedPhoto(photoUri: String) {
        _uiState.update { it.copy(selectedPhotoUri = photoUri) }
    }

    /**
     * 메모 입력값 업데이트
     */
    fun setMemo(memo: String) {
        _uiState.update { it.copy(memo = memo) }
    }

    /**
     * 점수 슬라이더 값 업데이트 (1~10)
     */
    fun setScore(score: Int) {
        val validScore = score.coerceIn(1, 10)
        _uiState.update { it.copy(score = validScore) }
    }

    /**
     * 기억/잊기 선택
     */
    fun setMeaning(meaning: Meaning) {
        _uiState.update { it.copy(meaning = meaning) }
    }

    /**
     * 대표 기억 체크 변경 처리
     *
     * - 같은 날짜에 이미 대표 기억이 있는 경우, 사용자 확인을 요청합니다.
     * - 확인 후에만 기존 대표 기억을 해제하고 새로운 선택을 반영합니다.
     */
    fun onFeaturedSelectionChanged(isChecked: Boolean) {
        if (!isChecked) {
            pendingFeaturedSelection = false
            _uiState.update { it.copy(isFeatured = false) }
            return
        }

        val today = currentDateString()
        val hasFeatured = hasFeaturedRecordForDate(today)
        if (hasFeatured) {
            // 기존 대표 기억이 있으면 체크 상태는 유지하지 않고 다이얼로그만 요청
            pendingFeaturedSelection = true
            _uiState.update { it.copy(isFeatured = false) }
            _events.tryEmit(UiEvent.ShowFeaturedReplaceDialog)
        } else {
            _uiState.update { it.copy(isFeatured = true) }
        }
    }

    fun confirmReplaceFeatured() {
        val today = currentDateString()
        clearFeaturedForDate(today)
        if (pendingFeaturedSelection) {
            _uiState.update { it.copy(isFeatured = true) }
        }
        pendingFeaturedSelection = false
    }

    fun cancelReplaceFeatured() {
        pendingFeaturedSelection = false
        _uiState.update { it.copy(isFeatured = false) }
    }

    /**
     * 순간 저장 로직
     *
     * 요구사항:
     * 1. 사진은 필수 (selectedPhotoUri가 null이면 실패)
     * 2. 메모는 선택사항 (빈 값 허용)
     * 3. 점수는 1~10
     * 4. 기억/잊기 선택은 필수
     *
     * 저장 완료 시 savedSuccessfully = true로 설정
     * (호출자는 이 값을 감지하여 PresentFragment로 복귀)
     */
    fun saveMoment() {
        val currentState = _uiState.value

        // 유효성 검사
        if (currentState.selectedPhotoUri.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "사진을 선택해주세요") }
            return
        }

        if (currentState.score < 1 || currentState.score > 10) {
            _uiState.update { it.copy(errorMessage = "점수는 1~10 사이여야 합니다") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                // 현재 날짜 포맷팅
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())

                if (currentState.isFeatured) {
                    // 저장 시에도 대표 기억은 하루에 하나만 유지되도록 보정
                    clearFeaturedForDate(today)
                }

                // DailyRecord 생성
                val newRecord = DailyRecord(
                    id = UUID.randomUUID().toString(),
                    photoUri = currentState.selectedPhotoUri ?: "",
                    memo = currentState.memo,
                    score = currentState.score,
                    meaning = currentState.meaning,
                    date = today,
                    isFeatured = currentState.isFeatured
                )

                // 메모리에 저장
                addRecord(newRecord)

                Log.d("CreateMomentViewModel", "Moment saved: ${newRecord.id}")

                // 저장 완료 상태 업데이트
                _uiState.update { it.copy(isLoading = false, savedSuccessfully = true) }

            } catch (e: Exception) {
                Log.e("CreateMomentViewModel", "Error saving moment: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "저장 중 오류가 발생했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 오류 메시지 초기화
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 저장 완료 상태 초기화 (화면 전환 후 호출)
     */
    fun resetSavedState() {
        _uiState.update { it.copy(savedSuccessfully = false) }
    }

    private fun hasFeaturedRecordForDate(date: String): Boolean {
        return savedRecordsFlow.value.any { it.date == date && it.isFeatured }
    }

    private fun clearFeaturedForDate(date: String) {
        savedRecordsFlow.update { records ->
            records.map { record ->
                if (record.date == date && record.isFeatured) {
                    record.copy(isFeatured = false)
                } else {
                    record
                }
            }
        }
    }

    private fun currentDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

}
