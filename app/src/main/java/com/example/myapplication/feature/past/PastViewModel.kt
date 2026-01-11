package com.example.myapplication.feature.past

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// --- ViewModel ---
class PastViewModel : ViewModel() {
    private val _days = MutableLiveData<List<DayEntry>>(generateSample())
    val days: LiveData<List<DayEntry>> = _days

    // 현재 상세로 보고 있는 Day (null이면 리스트 모드)
    private val _selectedDay = MutableLiveData<DayEntry?>(null)
    val selectedDay: LiveData<DayEntry?> = _selectedDay

    // 현재 선택된 사진 id (null이면 선택 없음 -> 일자 메모 보여줘야 함)
    private val _selectedPhotoId = MutableLiveData<Long?>(null)
    val selectedPhotoId: LiveData<Long?> = _selectedPhotoId

    fun selectDay(day: DayEntry) {
        _selectedDay.value = day
        _selectedPhotoId.value = null
    }

    fun clearSelectedDay() {
        _selectedDay.value = null
        _selectedPhotoId.value = null
    }

    fun togglePhotoSelection(photoId: Long) {
        val cur = _selectedPhotoId.value
        _selectedPhotoId.value = if (cur == photoId) null else photoId
    }

    // 샘플 데이터 (앱에선 DB나 Repository에서 가져오세요)
    companion object {
        private fun generateSample(): List<DayEntry> {
            return listOf(
                DayEntry(
                    dateLabel = "2024년 3월 20일",
                    dayMemo = "해변에서 본 아름다운 석양.",
                    photos = listOf(
                        PhotoItem(1, "석양1", "오늘 석양이 정말 멋졌어요."),
                        PhotoItem(2, "풍경2", "구름이 예쁘게 깔림."),
                        PhotoItem(3, "사람3", "이 사람과 같이 있었음.")
                    )
                ),
                DayEntry(
                    dateLabel = "2024년 3월 19일",
                    dayMemo = "사라와 함께한 커피 시간.",
                    photos = listOf(
                        PhotoItem(4, "카페1", "카페 분위기 좋았다."),
                        PhotoItem(5, "거리2", "산책을 했다.")
                    )
                ),
                DayEntry(
                    dateLabel = "2024년 3월 18일",
                    dayMemo = "프로젝트 완료!",
                    photos = listOf(
                        PhotoItem(6, "완료컷1", "끝냈다."),
                    )
                )
            )
        }
    }
}