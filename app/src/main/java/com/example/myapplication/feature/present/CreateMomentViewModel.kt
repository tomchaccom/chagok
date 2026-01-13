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

// 🌟 패키지 충돌 방지를 위한 Alias(별칭) 설정
import com.example.myapplication.data.present.DailyRecord as DataRecord
import com.example.myapplication.data.present.CesMetrics as DataCes
import com.example.myapplication.data.present.Meaning as DataMeaning

/**
 * CreateMomentViewModel
 * 순간 기록 화면의 상태와 로직을 관리하며, 데이터 영속성(파일 저장)을 담당합니다.
 */
class CreateMomentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMomentUiState())
    val uiState: StateFlow<CreateMomentUiState> = _uiState.asStateFlow()

    private var editingRecordId: String? = null
    private var editingRecordDate: String? = null

    // 저장된 기록 관리 (정적 영역)
    companion object {
        private val savedRecords = mutableListOf<DataRecord>()
        private const val DEFAULT_SCORE = 5
        private const val FILE_NAME = "present_records.json"
        private var appContext: Context? = null

        /**
         * 앱 시작 시 Context 주입 및 초기 로딩
         */
        fun initialize(context: Context) {
            appContext = context.applicationContext
            loadFromStorage()
        }

        /**
         * [Public] 정해진 시간(밤 11:59)에 Worker가 호출할 핵심 정리 로직
         */
        fun performDailyCleanup() {
            loadFromStorage()
            // 실천 완료(isAchieved == true)된 데이터만 유지하고 나머지는 자동 삭제
            val filteredList = savedRecords.filter { it.isAchieved }
            savedRecords.clear()
            savedRecords.addAll(filteredList)
            persistToStorage()
            Log.d("Cleanup", "밤 11:59 미실천 데이터 정리 완료")
        }

        /**
         * [Public] 현재 메모리 데이터를 JSON 파일로 저장합니다.
         */
        fun persistToStorage() {
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
                        put("isAchieved", r.isAchieved)
                        put("meaning", r.meaning.name)
                        put("cesMetrics", JSONObject().apply {
                            put("identity", r.cesMetrics.identity)
                            put("connectivity", r.cesMetrics.connectivity)
                            put("perspective", r.cesMetrics.perspective)
                            put("weightedScore", r.cesMetrics.weightedScore.toDouble())
                        })
                    }
                    arr.put(obj)
                }
                file.writeText(arr.toString())
            } catch (e: Exception) {
                Log.e("CreateMomentVM", "persist error: ${e.message}", e)
            }
        }

        /**
         * [Public] 외부에서 저장된 레코드 리스트를 읽을 때 사용
         */
        fun getSavedRecords(): List<DataRecord> = savedRecords.toList()

        fun addRecord(record: DataRecord) {
            savedRecords.add(record)
            persistToStorage()
        }

        fun clearRecords() {
            savedRecords.clear()
            persistToStorage()
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

                    val meaningStr = obj.optString("meaning", "REMEMBER")
                    val meaning = try { DataMeaning.valueOf(meaningStr) } catch (e: Exception) { DataMeaning.REMEMBER }

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

    // --- ViewModel 인스턴스 로직 ---

    fun startEdit(recordId: String) {
        val record = savedRecords.find { it.id == recordId } ?: run {
            _uiState.update { it.copy(errorMessage = "기록을 찾을 수 없습니다") }
            return
        }

        if (!isRecordEditable(record)) {
            _uiState.update { it.copy(errorMessage = "오늘 기록만 수정할 수 있습니다") }
            return
        }

        editingRecordId = recordId
        editingRecordDate = record.date

        _uiState.update {
            it.copy(
                selectedPhotoUri = record.photoUri,
                memo = record.memo,
                cesInput = CesInput(
                    identity = record.cesMetrics.identity,
                    connectivity = record.cesMetrics.connectivity,
                    perspective = record.cesMetrics.perspective
                ),
                cesWeightedScore = record.cesMetrics.weightedScore,
                cesDescription = describeCesScore(record.cesMetrics.weightedScore),
                meaning = record.meaning,
                isFeatured = record.isFeatured,
                editMode = true,
                timeState = TimeState.PRESENT,
                savedSuccessfully = false
            )
        }
    }

    fun setSelectedPhoto(uri: String) = _uiState.update { it.copy(selectedPhotoUri = uri) }
    fun setMemo(memo: String) = _uiState.update { it.copy(memo = memo) }
    fun setMeaning(meaning: DataMeaning) = _uiState.update { it.copy(meaning = meaning) }
    fun toggleFeatured() = _uiState.update { it.copy(isFeatured = !it.isFeatured) }

    fun setCesIdentity(v: Int) = updateCesInput { it.copy(identity = v) }
    fun setCesConnectivity(v: Int) = updateCesInput { it.copy(connectivity = v) }
    fun setCesPerspective(v: Int) = updateCesInput { it.copy(perspective = v) }

    private fun updateCesInput(transform: (CesInput) -> CesInput) {
        _uiState.update { state ->
            val updatedInput = transform(state.cesInput)
            val score = calculateCesScore(updatedInput)
            state.copy(
                cesInput = updatedInput,
                cesWeightedScore = score,
                cesDescription = describeCesScore(score)
            )
        }
    }

    fun saveMoment() {
        val currentState = _uiState.value
        if (currentState.selectedPhotoUri.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "사진을 선택해주세요") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val record = DataRecord(
                    id = editingRecordId ?: UUID.randomUUID().toString(),
                    photoUri = currentState.selectedPhotoUri!!,
                    memo = currentState.memo,
                    score = DEFAULT_SCORE,
                    cesMetrics = DataCes(
                        currentState.cesInput.identity,
                        currentState.cesInput.connectivity,
                        currentState.cesInput.perspective,
                        currentState.cesWeightedScore
                    ),
                    meaning = currentState.meaning,
                    date = editingRecordDate ?: today,
                    isFeatured = currentState.isFeatured,
                    isAchieved = true // 저장 시 실천 완료로 간주
                )

                if (editingRecordId != null) updateRecordInMemory(record) else addRecord(record)

                _uiState.update { it.copy(isLoading = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "저장 실패: ${e.message}") }
            }
        }
    }

    private fun updateRecordInMemory(updated: DataRecord) {
        val index = savedRecords.indexOfFirst { it.id == updated.id }
        if (index >= 0) savedRecords[index] = updated else savedRecords.add(updated)
        persistToStorage()
    }

    private fun calculateCesScore(input: CesInput): Float {
        val score = (0.5f * input.identity) + (0.2f * input.connectivity) + (0.3f * input.perspective)
        return (score * 10f).toInt() / 10f
    }

    private fun describeCesScore(score: Float): String = when {
        score <= 2.0f -> "낮음"
        score <= 3.5f -> "보통"
        else -> "높음"
    }

    private fun isRecordEditable(record: DataRecord): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return record.date == today
    }

    fun resetSavedState() = _uiState.update { it.copy(savedSuccessfully = false) }
    fun clearErrorMessage() = _uiState.update { it.copy(errorMessage = null) }
    private fun isPresentState(): Boolean = _uiState.value.timeState == TimeState.PRESENT

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
    }
}