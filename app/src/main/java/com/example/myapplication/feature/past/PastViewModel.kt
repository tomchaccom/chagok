package com.example.myapplication.feature.past

import androidx.lifecycle.*
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.data.past.PastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PastViewModel(
    private val repository: PastRepository
) : ViewModel() {

    // 데이터 로딩을 IO 스레드에서 수행하도록 변경 — 메인 스레드 블로킹 방지
    private val _days = MutableLiveData<List<DayEntry>>()
    val days: LiveData<List<DayEntry>> = _days

    init {
        refresh()
    }

    /**
     * Reload entries from repository on IO and publish to LiveData.
     * Call this after external changes to repository (e.g. import from Present).
     */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.loadPastEntries()
            _days.postValue(list)
        }
    }

    private val _selectedDay = MutableLiveData<DayEntry?>(null)
    val selectedDay: LiveData<DayEntry?> = _selectedDay

    /**
     * 선택된 사진 index
     * null → 선택 없음 → 일자 메모 표시
     */
    private val _selectedPhotoIndex = MutableLiveData<Int?>(null)
    val selectedPhotoIndex: LiveData<Int?> = _selectedPhotoIndex

    fun selectDay(day: DayEntry) {
        _selectedDay.value = day
        _selectedPhotoIndex.value = null
    }

    fun clearDay() {
        _selectedDay.value = null
        _selectedPhotoIndex.value = null
    }

    fun togglePhoto(index: Int) {
        _selectedPhotoIndex.value =
            if (_selectedPhotoIndex.value == index) null else index
    }
    // PastViewModel.kt
    // PastViewModel.kt
    fun loadDays() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. IO 스레드에서 데이터 로드
            val list = repository.loadPastEntries()

            // 2. 새로운 리스트 객체 생성 (메모리 주소 변경으로 변경 감지 보장)
            val newList = list.toList()

            // 3. 메인 스레드에 안전하게 전달
            withContext(Dispatchers.Main) {
                _days.value = newList
            }
        }
    }
}
