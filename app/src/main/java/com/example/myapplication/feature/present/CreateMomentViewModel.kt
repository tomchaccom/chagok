package com.example.myapplication.feature.present

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.util.TimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID
import com.example.myapplication.data.present.DailyRecord
// CreateMomentViewModel.kt 상단
import com.example.myapplication.data.present.DailyRecord as DataRecord
import com.example.myapplication.data.present.CesMetrics as DataCes
import com.example.myapplication.data.present.Meaning as DataMeaning
// 만약 feature.present 에도 동일한 이름이 있다면 아래처럼 구별됩니다.
import com.example.myapplication.feature.present.DailyRecord as FeatureRecord


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
    private var editingRecordId: String? = null
    private var editingRecordDate: String? = null
    val meaning: DataMeaning = DataMeaning.REMEMBER

    // 저장된 기록을 임시로 메모리에 보관 (싱글톤으로 변경 가능)
    companion object {
        private val savedRecords = mutableListOf<DailyRecord>()
        private const val DEFAULT_SCORE = 5
        private const val FILE_NAME = "present_records.json"
        private var appContext: Context? = null

        fun initialize(context: Context) {
            appContext = context.applicationContext
            loadFromStorage()
        }

        fun getSavedRecords(): List<DailyRecord> = savedRecords.toList()

        fun addRecord(record: DailyRecord) {
            savedRecords.add(record)
            persistToStorage()
        }

        fun clearRecords() {
            savedRecords.clear()
            persistToStorage()
        }

        fun updateRecordInMemory(updated: DailyRecord) {
            val index = savedRecords.indexOfFirst { it.id == updated.id }
            if (index == -1) savedRecords.add(updated) else savedRecords[index] = updated
            persistToStorage()
        }

        fun removeRecord(id: String) {
            val idx = savedRecords.indexOfFirst { it.id == id }
            if (idx >= 0) {
                savedRecords.removeAt(idx)
                persistToStorage()
            }
        }
        // 이 함수가 companion object 안에 있는지 반드시 확인하세요!
        fun performDailyCleanup() {
            // 1. 최신 데이터 로드
            loadFromStorage()

            // 2. 기록이 완료된(isAchieved == true) 데이터만 필터링
            // DataRecord가 Data 패키지의 모델인지 확인 (Alias 사용)
            val filteredList = savedRecords.filter { it.isAchieved }

            // 3. 리스트 갱신 및 파일 저장
            savedRecords.clear()
            savedRecords.addAll(filteredList)
            persistToStorage()

            Log.d("Cleanup", "밤 11:59 미실천 데이터 정리 완료")
        }

        private fun persistToStorage() {
            val ctx = appContext ?: return
            try {
                val file = File(ctx.filesDir, FILE_NAME)
                val arr = JSONArray()
                for (r in savedRecords) {
                    val obj = JSONObject()
                    obj.put("id", r.id)
                    obj.put("photoUri", r.photoUri)
                    obj.put("memo", r.memo)
                    obj.put("score", r.score)
                    obj.put("date", r.date)
                    obj.put("isFeatured", r.isFeatured)
                    // cesMetrics
                    val ces = JSONObject()
                    ces.put("identity", r.cesMetrics.identity)
                    ces.put("connectivity", r.cesMetrics.connectivity)
                    ces.put("perspective", r.cesMetrics.perspective)
                    ces.put("weightedScore", r.cesMetrics.weightedScore)
                    obj.put("cesMetrics", ces)

                    arr.put(obj)
                }
                file.writeText(arr.toString())
            } catch (e: Exception) {
                Log.e("CreateMomentVM", "persist error: ${e.message}", e)
            }
        }

        private fun loadFromStorage() {
            val ctx = appContext ?: return
            try {
                val file = File(ctx.filesDir, FILE_NAME)
                if (!file.exists()) return
                val content = file.readText()
                val arr = JSONArray(content)
                savedRecords.clear()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)

                    // 1. DataCes (별칭) 객체 안전하게 추출 및 생성
                    val cesObj = obj.optJSONObject("cesMetrics")
                    val ces = if (cesObj != null) {
                        DataCes(
                            identity = cesObj.optInt("identity", 3),
                            connectivity = cesObj.optInt("connectivity", 3),
                            perspective = cesObj.optInt("perspective", 3),
                            weightedScore = cesObj.optDouble("weightedScore", 3.0).toFloat()
                        )
                    } else {
                        DataCes(3, 3, 3, 3.0f)
                    }

                    // 2. DataMeaning (별칭) Enum 변환
                    val meaningStr = obj.optString("meaning", "REMEMBER")
                    val meaning = try {
                        DataMeaning.valueOf(meaningStr)
                    } catch (e: IllegalArgumentException) {
                        DataMeaning.REMEMBER
                    }

                    // 3. DataRecord (별칭) 객체 생성
                    val r = DataRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        photoUri = obj.optString("photoUri", ""),
                        memo = obj.optString("memo", ""),
                        score = obj.optInt("score", DEFAULT_SCORE),
                        cesMetrics = ces,
                        meaning = meaning,
                        date = obj.optString("date", ""),
                        isFeatured = obj.optBoolean("isFeatured", false),
                        isAchieved = obj.optBoolean("isAchieved", true)
                    )
                    savedRecords.add(r)
                }
            } catch (e: Exception) {
                Log.e("CreateMomentVM", "load error: ${e.message}")
            }
        }
    }

    fun startEdit(recordId: String) {
        // 1. DataRecord(별칭) 타입의 레코드를 찾습니다.
        val record: DataRecord = savedRecords.find { it.id == recordId } ?: run {
            _uiState.update { it.copy(errorMessage = "수정할 기록을 찾을 수 없습니다") }
            return
        }

        // 2. 오늘 기록인지 확인 (수정 가능 여부)
        if (!isRecordEditable(record)) {
            _uiState.update { it.copy(errorMessage = "오늘 기록만 수정할 수 있습니다") }
            return
        }

        editingRecordId = recordId
        editingRecordDate = record.date

        // 3. 기록된 CES 지표를 CesInput(입력용 객체)으로 변환
        val cesInput = CesInput(
            identity = record.cesMetrics.identity,
            connectivity = record.cesMetrics.connectivity,
            perspective = record.cesMetrics.perspective
        )

        val weightedScore = record.cesMetrics.weightedScore

        // 4. UI 상태 업데이트
        _uiState.update {
            it.copy(
                selectedPhotoUri = record.photoUri,
                memo = record.memo,
                cesInput = cesInput,
                cesWeightedScore = weightedScore,
                cesDescription = describeCesScore(weightedScore),
                meaning = meaning, // DataMeaning(별칭) 타입
                isFeatured = record.isFeatured,
                editMode = true,
                timeState = TimeState.PRESENT,
                savedSuccessfully = false,
                errorMessage = null,
                showFeaturedConflictDialog = false,
                allowFeaturedReplacement = false
            )
        }

    }
    // ViewModel 내부 함수
    fun setMeaning(meaning: DataMeaning) { // <--- 인자 타입을 DataMeaning으로
        _uiState.update { it.copy(meaning = meaning) }
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

                val hasFeaturedConflict = currentState.isFeatured &&
                    savedRecords.any { it.isFeatured && it.id != editingRecordId }
                if (hasFeaturedConflict) {
                    if (!currentState.allowFeaturedReplacement) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "이미 대표 기억이 설정되어 있습니다"
                            )
                        }
                        return@launch
                    }

                    clearFeaturedRecords(editingRecordId)
                }


                // 현재 날짜 포맷팅
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())

                val recordId = editingRecordId ?: UUID.randomUUID().toString()
                val recordDate = editingRecordDate ?: today
                val newRecord = DailyRecord(
                    id = recordId,
                    photoUri = currentState.selectedPhotoUri ?: "",
                    memo = currentState.memo,
                    score = savedRecords.find { it.id == recordId }?.score ?: DEFAULT_SCORE,
                    cesMetrics = buildCesMetrics(currentState.cesInput),
                    meaning = currentState.meaning,
                    date = recordDate,
                    isFeatured = currentState.isFeatured
                )

                if (editingRecordId != null) {
                    updateRecord(newRecord)
                } else {
                    addRecord(newRecord)
                }

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
        val currentEditId = editingRecordId
        val hasOtherFeatured = savedRecords.any { it.isFeatured && it.id != currentEditId }
        if (isFeatured && hasOtherFeatured) {
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
        clearFeaturedRecords(null)
    }

    private fun clearFeaturedRecords(excludeId: String?) {
        for (index in savedRecords.indices) {
            val record = savedRecords[index]
            if (record.isFeatured && record.id != excludeId) {
                savedRecords[index] = record.copy(isFeatured = false)
            }
        }
    }

    private fun updateRecord(updated: DailyRecord) {
        val index = savedRecords.indexOfFirst { it.id == updated.id }
        if (index == -1) {
            savedRecords.add(updated)
        } else {
            savedRecords[index] = updated
        }
    }

    private fun isRecordEditable(record: DailyRecord): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return record.date == today
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

    // CreateMomentViewModel.kt 내부
    private fun buildCesMetrics(input: CesInput): DataCes { // 반환 타입을 별칭으로 변경
        return DataCes(
            identity = input.identity,
            connectivity = input.connectivity,
            perspective = input.perspective,
            weightedScore = calculateCesScore(input)
        )
    }

    private fun isPresentState(): Boolean = _uiState.value.timeState == TimeState.PRESENT

    // 밤 11:59에 실행될 핵심 정리 로직
    fun performDailyCleanup() {
        loadFromStorage()
        // 기록이 완료된(isAchieved == true) 데이터만 남기고 나머지는 삭제
        val filteredList = savedRecords.filter { it.isAchieved }
        savedRecords.clear()
        savedRecords.addAll(filteredList)
        persistToStorage()
    }

    private fun persistToStorage() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, FILE_NAME)
            val arr = JSONArray()
            for (r in savedRecords) {
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("photoUri", r.photoUri)
                    put("memo", r.memo)
                    put("score", r.score)
                    put("date", r.date)
                    put("isFeatured", r.isFeatured)
                    put("isAchieved", r.isAchieved) // 필드 추가
                    put("cesMetrics", JSONObject().apply {
                        put("identity", r.cesMetrics.identity)
                        put("connectivity", r.cesMetrics.connectivity)
                        put("perspective", r.cesMetrics.perspective)
                        put("weightedScore", r.cesMetrics.weightedScore)
                    })
                }
                arr.put(obj)
            }
            file.writeText(arr.toString())
        } catch (e: Exception) { Log.e("VM", "Save Error", e) }
    }
}


