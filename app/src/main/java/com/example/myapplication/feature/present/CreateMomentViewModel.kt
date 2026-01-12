package com.example.myapplication.feature.present

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.util.TimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
     * - 메모, 기억/잊기 상태 관리
 * - 저장 로직 실행
 *
 * 주의: 현재는 in-memory 저장만 지원합니다 (Room DB는 추후 추가)
 */
class CreateMomentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMomentUiState())
    val uiState: StateFlow<CreateMomentUiState> = _uiState.asStateFlow()

    // 저장된 기록을 임시로 메모리에 보관 (싱글톤으로 변경 가능)
    companion object {
        private val savedRecords = mutableListOf<DailyRecord>()

        fun getSavedRecords(): List<DailyRecord> = savedRecords.toList()
        fun addRecord(record: DailyRecord) {
            savedRecords.add(record)
        }
        fun clearRecords() {
            savedRecords.clear()
        }
    }

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
    fun setTimeState(state: TimeState) {
        _uiState.update { it.copy(timeState = state) }
    }

    fun setCesIdentity(value: Int) {
        if (!isPresentState()) {
            _uiState.update { it.copy(errorMessage = "과거 기록은 수정할 수 없습니다") }
            return
        }
        updateCesInput { it.copy(identity = value.coerceIn(1, 5)) }
    }

    fun setCesConnectivity(value: Int) {
        if (!isPresentState()) {
            _uiState.update { it.copy(errorMessage = "과거 기록은 수정할 수 없습니다") }
            return
        }
        updateCesInput { it.copy(connectivity = value.coerceIn(1, 5)) }
    }

    fun setCesPerspective(value: Int) {
        if (!isPresentState()) {
            _uiState.update { it.copy(errorMessage = "과거 기록은 수정할 수 없습니다") }
            return
        }
        updateCesInput { it.copy(perspective = value.coerceIn(1, 5)) }
    }

    /**
     * 기억/잊기 선택
     */
    fun setMeaning(meaning: Meaning) {
        _uiState.update { it.copy(meaning = meaning) }
    }

    /**
     * 오늘의 대표 기억 체크 토글
     */
    fun toggleFeatured() {
        _uiState.update { it.copy(isFeatured = !it.isFeatured) }
    }

    /**
     * 순간 저장 로직
     *
     * 요구사항:
     * 1. 사진은 필수 (selectedPhotoUri가 null이면 실패)
     * 2. 메모는 선택사항 (빈 값 허용)
     * 3. 기억/잊기 선택은 필수
     *
     * 저장 완료 시 savedSuccessfully = true로 설정
     * (호출자는 이 값을 감지하여 PresentFragment로 복귀)
     */
    fun saveMoment() {
        val currentState = _uiState.value

        if (!isPresentState()) {
            _uiState.update { it.copy(errorMessage = "과거 기록은 수정할 수 없습니다") }
            return
        }

        // 유효성 검사
        if (currentState.selectedPhotoUri.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "사진을 선택해주세요") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                if (currentState.isFeatured && savedRecords.any { it.isFeatured }) {
                    if (!currentState.allowFeaturedReplacement) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "이미 대표 기억이 설정되어 있습니다"
                            )
                        }
                        return@launch
                    }

                    clearFeaturedRecords()
                }


                // 현재 날짜 포맷팅
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())

                // DailyRecord 생성
                val newRecord = DailyRecord(
                    id = UUID.randomUUID().toString(),
                    photoUri = currentState.selectedPhotoUri ?: "",
                    memo = currentState.memo,
                    score = DEFAULT_SCORE,
                    cesMetrics = buildCesMetrics(currentState.cesInput),
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

    fun setFeatured(isFeatured: Boolean) {
        if (isFeatured && savedRecords.any { it.isFeatured }) {
            _uiState.update {
                it.copy(
                    isFeatured = false,
                    showFeaturedConflictDialog = true,
                    allowFeaturedReplacement = false
                )
            }
            return
        }

        _uiState.update { it.copy(
            isFeatured = isFeatured,
            allowFeaturedReplacement = if (isFeatured) it.allowFeaturedReplacement else false
        ) }
    }
    fun confirmFeaturedReplacement(shouldReplace: Boolean) {
        _uiState.update {
            it.copy(
                isFeatured = shouldReplace,
                showFeaturedConflictDialog = false,
                allowFeaturedReplacement = shouldReplace
            )
        }
    }

    fun consumeFeaturedConflictDialog() {
        _uiState.update { it.copy(showFeaturedConflictDialog = false) }
    }

    private fun clearFeaturedRecords() {
        for (index in savedRecords.indices) {
            val record = savedRecords[index]
            if (record.isFeatured) {
                savedRecords[index] = record.copy(isFeatured = false)
            }
        }
    }

    private fun updateCesInput(transform: (CesInput) -> CesInput) {
        _uiState.update { state ->
            val updatedInput = transform(state.cesInput)
            val weightedScore = calculateCesScore(updatedInput)
            val description = describeCesScore(weightedScore)
            state.copy(
                cesInput = updatedInput,
                cesWeightedScore = weightedScore,
                cesDescription = description
            )
        }
    }

    private fun calculateCesScore(input: CesInput): Float {
        val weightedScore = (0.5f * input.identity) +
            (0.2f * input.connectivity) +
            (0.3f * input.perspective)
        return (weightedScore * 10f).toInt() / 10f
    }

    private fun describeCesScore(score: Float): String {
        return when {
            score <= 2.0f -> "낮음"
            score <= 3.5f -> "보통"
            else -> "높음"
        }
    }

    private fun buildCesMetrics(input: CesInput): CesMetrics {
        return CesMetrics(
            identity = input.identity,
            connectivity = input.connectivity,
            perspective = input.perspective,
            weightedScore = calculateCesScore(input)
        )
    }

    private fun isPresentState(): Boolean = _uiState.value.timeState == TimeState.PRESENT

    companion object {
        private const val DEFAULT_SCORE = 5
    }

}
