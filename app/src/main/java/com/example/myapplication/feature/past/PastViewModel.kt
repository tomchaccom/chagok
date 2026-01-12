package com.example.myapplication.feature.past

import androidx.lifecycle.*
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.data.past.PastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PastViewModel(
    private val repository: PastRepository
) : ViewModel() {

    // 데이터 로딩을 IO 스레드에서 수행하도록 변경 — 메인 스레드 블로킹 방지
    private val _days = MutableLiveData<List<DayEntry>>()
    val days: LiveData<List<DayEntry>> = _days

    init {
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
}
