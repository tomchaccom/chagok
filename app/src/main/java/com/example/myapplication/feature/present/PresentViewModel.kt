package com.example.myapplication.feature.present

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.present.PresentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PresentViewModel(private val repository: PresentRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PresentUiState())
    val uiState: StateFlow<PresentUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPresentData()
    }

    fun loadPresentData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                val presentData = repository.getPresentData()
                _uiState.value = presentData
            } catch (e: Exception) {
                Log.e("PresentViewModel", "Error loading data: ${e.message}", e)
                _errorMessage.value = "데이터를 불러올 수 없습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPracticeStateChanged(practiceId: String, isAchieved: Boolean) {
        viewModelScope.launch {
            try {
                repository.updatePracticeState(practiceId, isAchieved)
                _uiState.update {
                    val updatedPractices = it.practices.map {
                        if (it.id == practiceId) it.copy(isAchieved = isAchieved) else it
                    }
                    it.copy(practices = updatedPractices)
                }

            } catch (e: Exception) {
                Log.e("PresentViewModel", "Error updating practice: ${e.message}", e)
                _errorMessage.value = "실천 상태 업데이트에 실패했습니다"
            }
        }
    }

    fun onAddPractice(text: String) {
        viewModelScope.launch {
            try {
                repository.addPractice(text)
                // loadPresentData() // Refresh list
            } catch (e: Exception) {
                Log.e("PresentViewModel", "Error adding practice: ${e.message}", e)
                _errorMessage.value = "실천 추가에 실패했습니다"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // PresentViewModel.kt
    fun saveNewRecord(photoUri: String, memo: String, score: Int) {
        val newRecord = DailyRecord(
            id = System.currentTimeMillis().toString(), // 임시 ID
            photoUri = photoUri,
            memo = memo,
            score = score,
            cesMetrics = CesMetrics(3, 3, 3, 3.0f), // 임시 값
            date = "2026.01.13",
            meaning = Meaning.REMEMBER,
            isFeatured = false
        )
        repository.addRecord(newRecord)
        loadPresentData() // 저장 후 즉시 리스트 새로고침
    }
}