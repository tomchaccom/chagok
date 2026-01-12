package com.example.myapplication.feature.past

import androidx.lifecycle.*
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.data.past.PastRepository

class PastViewModel(
    repository: PastRepository
) : ViewModel() {

    val days: LiveData<List<DayEntry>> =
        MutableLiveData(repository.loadPastEntries())

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
